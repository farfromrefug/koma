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
import java.util.concurrent.atomic.AtomicInteger

/**
 * Tests for concurrent processing patterns used in local source chapter processing.
 * These tests verify that bounded concurrency works correctly and prevents
 * memory exhaustion from unbounded parallelism.
 */
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
        val currentConcurrent = AtomicInteger(0)
        val maxObservedConcurrent = AtomicInteger(0)

        coroutineScope {
            (1..totalTasks).map { taskId ->
                async {
                    semaphore.withPermit {
                        val current = currentConcurrent.incrementAndGet()
                        // Track max concurrent
                        maxObservedConcurrent.updateAndGet { maxOf(it, current) }
                        
                        // Simulate some work
                        delay(10)
                        
                        currentConcurrent.decrementAndGet()
                        taskId
                    }
                }
            }.awaitAll()
        }

        // Verify that concurrent execution was bounded
        maxObservedConcurrent.get() shouldBe maxConcurrency
        currentConcurrent.get() shouldBe 0
    }

    /**
     * Test that all tasks complete even with bounded concurrency.
     */
    @Test
    fun `All tasks complete with bounded concurrency`() = runBlocking {
        val maxConcurrency = 2
        val totalTasks = 20
        val semaphore = Semaphore(maxConcurrency)
        val completedTasks = AtomicInteger(0)

        coroutineScope {
            (1..totalTasks).map { taskId ->
                async {
                    semaphore.withPermit {
                        delay(5) // Simulate work
                        completedTasks.incrementAndGet()
                        taskId
                    }
                }
            }.awaitAll()
        }

        completedTasks.get() shouldBe totalTasks
    }

    /**
     * Test that errors in one task don't prevent other tasks from completing.
     */
    @Test
    fun `Errors in individual tasks are isolated`() = runBlocking {
        val maxConcurrency = 3
        val totalTasks = 10
        val semaphore = Semaphore(maxConcurrency)
        val successfulTasks = AtomicInteger(0)

        val results = coroutineScope {
            (1..totalTasks).map { taskId ->
                async {
                    semaphore.withPermit {
                        try {
                            if (taskId % 3 == 0) {
                                throw RuntimeException("Simulated failure for task $taskId")
                            }
                            delay(5)
                            successfulTasks.incrementAndGet()
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
        successfulTasks.get() shouldBe successes
    }
}
