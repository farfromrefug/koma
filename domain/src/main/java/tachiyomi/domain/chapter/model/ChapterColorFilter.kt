package tachiyomi.domain.chapter.model

/**
 * Data class representing color filter settings that can be saved per chapter.
 * All fields have defaults that match the global preferences defaults.
 * This makes it easy to extend with new filters in the future - just add new fields with defaults.
 */
data class ChapterColorFilter(
    val chapterId: Long,
    val customBrightness: Boolean = false,
    val customBrightnessValue: Int = 0,
    val colorFilter: Boolean = false,
    val colorFilterValue: Int = 0,
    val colorFilterMode: Int = 0,
    val grayscale: Boolean = false,
    val invertedColors: Boolean = false,
    val sharpenFilter: Boolean = false,
    val sharpenFilterScale: Float = 0.5f,
    val einkFilter: Boolean = false,
    val einkFilterBrightness: Float = 0.1f,
    val einkFilterContrast: Float = 0.2f,
    val einkFilterSaturation: Float = 0.0f,
) {
    companion object {
        fun create(chapterId: Long) = ChapterColorFilter(chapterId = chapterId)
    }
}
