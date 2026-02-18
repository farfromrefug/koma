package eu.kanade.tachiyomi.ui.reader.loader

import android.app.Application
import android.net.Uri
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.data.database.models.toDomainChapter
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.download.DownloadProvider
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import mihon.core.archive.archiveReader
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.download.service.DownloadPreferences
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.storage.service.StorageManager
import tachiyomi.source.local.LocalSource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy

/**
 * Loader used to load a chapter from the downloaded chapters.
 */
internal class DownloadPageLoader(
    private val chapter: ReaderChapter,
    private val manga: Manga,
    private val source: Source,
    private val downloadManager: DownloadManager,
    private val downloadProvider: DownloadProvider,
) : PageLoader() {

    private val context: Application by injectLazy()
    private val storageManager: StorageManager by injectLazy()
    private val downloadPreferences: DownloadPreferences by injectLazy()

    private var archivePageLoader: ArchivePageLoader? = null

    override var isLocal: Boolean = true

    override suspend fun getPages(): List<ReaderPage> {
        val dbChapter = chapter.chapter
        var chapterPath = downloadProvider.findChapterDir(
            dbChapter.name,
            dbChapter.scanlator,
            dbChapter.url,
            manga.title,
            source,
        )
        
        // If not found and downloadToLocalSource is enabled, try finding in local source directory
        // using the chapter's metadata to construct template-based names
        if (chapterPath == null) {
            if (downloadPreferences.downloadToLocalSource().get() && source.id != LocalSource.ID) {
                val domainChapter = dbChapter.toDomainChapter()
                if (domainChapter != null) {
                    chapterPath = findChapterInLocalSource(domainChapter)
                }
            }
        }
        
        return if (chapterPath?.isFile == true) {
            getPagesFromArchive(chapterPath)
        } else {
            getPagesFromDirectory()
        }
    }
    
    /**
     * Attempts to find a chapter in the local source directory using chapter metadata.
     * This is needed when chapters are downloaded to local source with template-based names.
     */
    private fun findChapterInLocalSource(dbChapter: Chapter): UniFile? {
        val localSourceDir = storageManager.getLocalSourceDirectory() ?: return null
        
        // Get the local source manga directory
        val localMangaDirName = downloadProvider.getLocalSourceMangaDirName(manga.title)
        val localMangaDir = localSourceDir.findFile(localMangaDirName) ?: return null
        
        // Try to find chapter using local source template names if metadata is available
        // Chapter number >= 0 or dateUpload > 0 indicates valid metadata that can be used in templates
        if (dbChapter.chapterNumber >= 0 || dbChapter.dateUpload > 0) {
            val localSourceNames = downloadProvider.getValidLocalSourceChapterDirNames(
                dbChapter.name,
                dbChapter.chapterNumber,
                dbChapter.scanlator,
                manga.title,
                dbChapter.url,
                dbChapter.dateUpload,
            )
            
            // Try each potential name
            for (name in localSourceNames) {
                localMangaDir.findFile(name)?.let { return it }
                localMangaDir.findFile("$name.cbz")?.let { return it }
            }
        }
        
        // Fall back to URL hash matching when metadata is not available
        val chapterHash = downloadProvider.getChapterUrlHashSuffix(dbChapter.url)
        return localMangaDir.listFiles()?.firstOrNull { file ->
            val fileName = file.name ?: return@firstOrNull false
            fileName.endsWith(chapterHash) || fileName.endsWith("$chapterHash.cbz")
        }
    }

    override fun recycle() {
        super.recycle()
        archivePageLoader?.recycle()
    }

    private suspend fun getPagesFromArchive(file: UniFile): List<ReaderPage> {
        val loader = ArchivePageLoader(file.archiveReader(context)).also { archivePageLoader = it }
        return loader.getPages()
    }

    private fun getPagesFromDirectory(): List<ReaderPage> {
        val pages = downloadManager.buildPageList(source, manga, chapter.chapter.toDomainChapter()!!)
        return pages.map { page ->
            ReaderPage(page.index, page.url, page.imageUrl) {
                context.contentResolver.openInputStream(page.uri ?: Uri.EMPTY)!!
            }.apply {
                status = Page.State.Ready
            }
        }
    }

    override suspend fun loadPage(page: ReaderPage) {
        archivePageLoader?.loadPage(page)
    }
}
