package eu.kanade.domain.chapter.model

import eu.kanade.tachiyomi.data.database.models.ChapterImpl
import eu.kanade.tachiyomi.data.database.models.toJsonString
import eu.kanade.tachiyomi.data.database.models.getBannersFromJson
import eu.kanade.tachiyomi.source.model.SChapter
import tachiyomi.domain.chapter.model.Chapter
import eu.kanade.tachiyomi.data.database.models.Chapter as DbChapter

// TODO: Remove when all deps are migrated
fun Chapter.toSChapter(): SChapter {
    return SChapter.create().also {
        it.url = url
        it.name = name
        it.date_upload = dateUpload
        it.chapter_number = chapterNumber.toFloat()
        it.scanlator = scanlator
        it.thumbnail_url = coverUrl
        it.description = description
        it.genre = genre.orEmpty().joinToString()
        it.tags = tags.orEmpty().joinToString()
        it.moods = moods.orEmpty().joinToString()
        it.language = language
        it.total_pages = totalPages
        it.banners = banners.toJsonString()
    }
}

fun Chapter.copyFromSChapter(sChapter: SChapter): Chapter {
    val description = sChapter.description ?: description
    val genres = if (sChapter.genre != null) {
        sChapter.getGenres()
    } else {
        genre
    }
    val tags = if (sChapter.tags != null) {
        sChapter.getTags()
    } else {
        tags
    }
    val moods = if (sChapter.moods != null) {
        sChapter.getMoods()
    } else {
        moods
    }
    val finalTotalPages = sChapter.total_pages ?: totalPages
    
    // Create a temporary DbChapter to parse banners from JSON
    val bannersFromSChapter = if (sChapter.banners != null) {
        object : DbChapter {
            override var banners: String? = sChapter.banners
            override var id: Long? = null
            override var manga_id: Long? = null
            override var read: Boolean = false
            override var bookmark: Boolean = false
            override var last_page_read: Int = 0
            override var date_fetch: Long = 0
            override var source_order: Int = 0
            override var last_modified: Long = 0
            override var version: Long = 0
            override var url: String = ""
            override var name: String = ""
            override var date_upload: Long = 0
            override var chapter_number: Float = 0f
            override var scanlator: String? = null
            override var description: String? = null
            override var genre: String? = null
            override var tags: String? = null
            override var moods: String? = null
            override var language: String? = null
            override var thumbnail_url: String? = null
            override var total_pages: Long? = null
        }.getBannersFromJson()
    } else {
        banners
    }
    
    return this.copy(
        name = sChapter.name,
        url = sChapter.url,
        dateUpload = sChapter.date_upload,
        chapterNumber = sChapter.chapter_number.toDouble(),
        scanlator = sChapter.scanlator?.ifBlank { null }?.trim(),
        coverUrl = sChapter.thumbnail_url,
        genre = genres,
        tags = tags,
        moods = moods,
        language = language,
        description = description,
        totalPages = finalTotalPages,
        banners = bannersFromSChapter,
    )
}

fun Chapter.toDbChapter(): DbChapter = ChapterImpl().also {
    it.id = id
    it.manga_id = mangaId
    it.url = url
    it.name = name
    it.description = description
    it.genre = genre.orEmpty().joinToString()
    it.tags = tags.orEmpty().joinToString()
    it.moods = moods.orEmpty().joinToString()
    it.scanlator = scanlator
    it.read = read
    it.bookmark = bookmark
    it.last_page_read = lastPageRead.toInt()
    it.date_fetch = dateFetch
    it.date_upload = dateUpload
    it.chapter_number = chapterNumber.toFloat()
    it.source_order = sourceOrder.toInt()
    it.banners = banners.toJsonString()
}
