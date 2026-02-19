package tachiyomi.domain.chapter.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString

@Serializable
private data class SerializedChapterTag(
    val text: String,
    val color: Long,
)

/**
 * Parses a JSON string into a list of ChapterTag objects.
 * Returns null if the input is null/blank or if parsing fails.
 */
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

/**
 * Serializes a list of ChapterTag objects into a JSON string.
 * Returns null if the list is null or empty, or if serialization fails.
 */
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
