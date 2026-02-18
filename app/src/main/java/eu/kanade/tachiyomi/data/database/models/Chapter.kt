@file:Suppress("PropertyName")

package eu.kanade.tachiyomi.data.database.models

import eu.kanade.tachiyomi.source.model.SChapter
import java.io.Serializable
import kotlinx.serialization.Serializable as KSerializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import tachiyomi.domain.chapter.model.ChapterTag
import tachiyomi.domain.chapter.model.Chapter as DomainChapter

interface Chapter : SChapter, Serializable {

    var id: Long?

    var manga_id: Long?

    var read: Boolean

    var bookmark: Boolean

    var last_page_read: Int

    var date_fetch: Long

    var source_order: Int

    var last_modified: Long

    var version: Long

}

val Chapter.isRecognizedNumber: Boolean
    get() = chapter_number >= 0f

@KSerializable
private data class SerializedChapterTag(
    val text: String,
    val color: Long,
)

fun parseBannersFromJson(bannersJson: String?): List<ChapterTag>? {
    if (bannersJson.isNullOrBlank()) return null
    return try {
        Json.decodeFromString<List<SerializedChapterTag>>(bannersJson).map {
            ChapterTag(text = it.text, color = it.color)
        }
    } catch (e: Exception) {
        null
    }
}

fun Chapter.getBannersFromJson(): List<ChapterTag>? {
    return parseBannersFromJson(banners)
}

fun List<ChapterTag>?.toJsonString(): String? {
    if (this == null || this.isEmpty()) return null
    return try {
        Json.encodeToString(
            kotlinx.serialization.builtins.ListSerializer(
                kotlinx.serialization.serializer<SerializedChapterTag>()
            ),
            this.map { SerializedChapterTag(text = it.text, color = it.color) }
        )
    } catch (e: Exception) {
        null
    }
}

fun Chapter.toDomainChapter(): DomainChapter? {
    if (id == null || manga_id == null) return null
    return DomainChapter(
        id = id!!,
        mangaId = manga_id!!,
        read = read,
        bookmark = bookmark,
        lastPageRead = last_page_read.toLong(),
        dateFetch = date_fetch,
        sourceOrder = source_order.toLong(),
        url = url,
        name = name,
        dateUpload = date_upload,
        chapterNumber = chapter_number.toDouble(),
        scanlator = scanlator,
        lastModifiedAt = last_modified,
        version = version,
        coverUrl = thumbnail_url,
        totalPages = 0,
        description = description,
        language = language,
        genre = getGenres(),
        tags = getTags(),
        moods = getMoods(),
        banners = parseBannersFromJson(banners),
    )
}
