package tachiyomi.domain.chapter.model

/**
 * Display mode for chapters in the manga screen.
 * Similar to LibraryDisplayMode but for chapters.
 */
sealed interface ChapterDisplayMode {
    /**
     * Display chapters in a list format (default, existing behavior).
     */
    data object List : ChapterDisplayMode

    /**
     * Display chapters in a compact grid format with cover and chapter info.
     */
    data object CompactGrid : ChapterDisplayMode

    /**
     * Display chapters in a comfortable grid format with more spacing.
     */
    data object ComfortableGrid : ChapterDisplayMode

    object Serializer {
        fun deserialize(serialized: String): ChapterDisplayMode {
            return ChapterDisplayMode.deserialize(serialized)
        }

        fun serialize(value: ChapterDisplayMode): String {
            return value.serialize()
        }
    }

    companion object {
        val values by lazy { setOf(List, CompactGrid, ComfortableGrid) }
        val default = List

        fun deserialize(serialized: String): ChapterDisplayMode {
            return when (serialized) {
                "COMPACT_GRID" -> CompactGrid
                "COMFORTABLE_GRID" -> ComfortableGrid
                "LIST" -> List
                else -> default
            }
        }
    }

    fun serialize(): String {
        return when (this) {
            CompactGrid -> "COMPACT_GRID"
            ComfortableGrid -> "COMFORTABLE_GRID"
            List -> "LIST"
        }
    }
}
