package tachiyomi.domain.library.service

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.incrementAndFetch

/**
 * Tests for concurrent processing patterns used in local source chapter processing.
 * These tests verify that bounded concurrency works correctly and prevents
 * memory exhaustion from unbounded parallelism.
 */
@OptIn(ExperimentalAtomicApi::class)
@Execution(ExecutionMode.CONCURRENT)
class LocalSourceConcurrencyTest {

    /**
     * Test that semaphore-based bounded concurrency limits the number of
     * concurrent tasks.
     */
    @Test
    fun `Semaphore limits concurrent execution`() = runBlocking {
        val maxConcurrency = 3
        val totalTasks = 10
        val semaphore = Semaphore(maxConcurrency)
        val currentConcurrent = AtomicInt(0)
        val maxObservedConcurrent = AtomicInt(0)

        coroutineScope {
            (1..totalTasks).map { taskId ->
                async {
                    semaphore.withPermit {
                        val current = currentConcurrent.incrementAndFetch()
                        // Track max concurrent
                        synchronized(maxObservedConcurrent) {
                            if (current > maxObservedConcurrent.load()) {
                                maxObservedConcurrent.store(current)
                            }
                        }
                        
                        // Simulate some work
                        delay(10)
                        
                        currentConcurrent.decrementAndFetch()
                        taskId
                    }
                }
            }.awaitAll()
        }

        // Verify that concurrent execution was bounded
        maxObservedConcurrent.load() shouldBe maxConcurrency
        currentConcurrent.load() shouldBe 0
    }

    /**
     * Test that all tasks complete even with bounded concurrency.
     */
    @Test
    fun `All tasks complete with bounded concurrency`() = runBlocking {
        val maxConcurrency = 2
        val totalTasks = 20
        val semaphore = Semaphore(maxConcurrency)
        val completedTasks = AtomicInt(0)

        coroutineScope {
            (1..totalTasks).map { taskId ->
                async {
                    semaphore.withPermit {
                        delay(5) // Simulate work
                        completedTasks.incrementAndFetch()
                        taskId
                    }
                }
            }.awaitAll()
        }

        completedTasks.load() shouldBe totalTasks
    }

    /**
     * Test that errors in one task don't prevent other tasks from completing.
     */
    @Test
    fun `Errors in individual tasks are isolated`() = runBlocking {
        val maxConcurrency = 3
        val totalTasks = 10
        val semaphore = Semaphore(maxConcurrency)
        val successfulTasks = AtomicInt(0)

        val results = coroutineScope {
            (1..totalTasks).map { taskId ->
                async {
                    semaphore.withPermit {
                        try {
                            if (taskId % 3 == 0) {
                                throw RuntimeException("Simulated failure for task $taskId")
                            }
                            delay(5)
                            successfulTasks.incrementAndFetch()
                            Result.success(taskId)
                        } catch (e: Exception) {
                            Result.failure(e)
                        }
                    }
                }
            }.awaitAll()
        }

        // Should have some successes and some failures
        val successes = results.count { it.isSuccess }
        val failures = results.count { it.isFailure }
        
        successes shouldBe (totalTasks - totalTasks / 3) // Tasks not divisible by 3 succeed
        failures shouldBe (totalTasks / 3) // Tasks divisible by 3 fail
        successfulTasks.load() shouldBe successes
    }
    
    private fun AtomicInt.decrementAndFetch(): Int {
        return addAndFetch(-1)
    }
    
    private fun AtomicInt.addAndFetch(delta: Int): Int {
        var current: Int
        var new: Int
        do {
            current = load()
            new = current + delta
        } while (!compareAndSet(current, new))
        return new
    }
}
