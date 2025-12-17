package eu.kanade.tachiyomi.data.coil

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.os.Build
import coil3.ImageLoader
import coil3.asImage
import coil3.decode.DecodeResult
import coil3.decode.DecodeUtils
import coil3.decode.Decoder
import coil3.decode.ImageSource
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import coil3.request.bitmapConfig
import okio.BufferedSource
import tachiyomi.core.common.util.system.ImageUtil

/**
 * A [Decoder] that uses Android's ImageDecoder (Skia-backed) on API 28+,
 * and falls back to BitmapFactory on older platforms.
 */
class TachiyomiImageDecoder(private val resources: ImageSource, private val options: Options) : Decoder {

    override suspend fun decode(): DecodeResult {
        // Prepare source bytes
        val bytes = resources.sourceOrNull()?.use { src ->
            src.readByteArray()
        } ?: throw IllegalStateException("Failed to read image source")

        // Get source dimensions by decoding bounds first (BitmapFactory) to compute sampling.
        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, boundsOptions)
        val srcWidth = boundsOptions.outWidth
        val srcHeight = boundsOptions.outHeight
        check(srcWidth > 0 && srcHeight > 0) { "Failed to get image dimensions" }

        val dstWidth = options.size.widthPx(options.scale) { srcWidth }
        val dstHeight = options.size.heightPx(options.scale) { srcHeight }

        val sampleSize = DecodeUtils.calculateInSampleSize(
            srcWidth = srcWidth,
            srcHeight = srcHeight,
            dstWidth = dstWidth,
            dstHeight = dstHeight,
            scale = options.scale,
        )

        var bitmap: Bitmap? = null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(java.nio.ByteBuffer.wrap(bytes))
            bitmap = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                // Set target size so ImageDecoder can sample efficiently
                decoder.setTargetSize(dstWidth, dstHeight)
                // Choose allocator based on requested config
                if (options.bitmapConfig == Bitmap.Config.HARDWARE) {
                    decoder.setAllocator(ImageDecoder.ALLOCATOR_HARDWARE)
                } else {
                    decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE)
                }
                // Preserve color space / postprocessing if needed (displayProfile handling removed)
            }
        } else {
            val bmOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = options.bitmapConfig
            }
            bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bmOptions)
        }

        check(bitmap != null) { "Failed to decode image" }

        // Convert to hardware bitmap if requested and supported
        if (options.bitmapConfig == Bitmap.Config.HARDWARE && ImageUtil.canUseHardwareBitmap(bitmap)) {
            val hwBitmap = bitmap.copy(Bitmap.Config.HARDWARE, false)
            if (hwBitmap != null) {
                bitmap.recycle()
                bitmap = hwBitmap
            }
        }

        return DecodeResult(
            image = bitmap.asImage(),
            isSampled = sampleSize > 1,
        )
    }

    class Factory : Decoder.Factory {

        override fun create(result: SourceFetchResult, options: Options, imageLoader: ImageLoader): Decoder? {
            return if (options.customDecoder || isApplicable(result.source.source())) {
                TachiyomiImageDecoder(result.source, options)
            } else {
                null
            }
        }

        private fun isApplicable(source: BufferedSource): Boolean {
            val type = source.peek().inputStream().use {
                ImageUtil.findImageType(it)
            }
            return when (type) {
                ImageUtil.ImageType.AVIF, ImageUtil.ImageType.JXL, ImageUtil.ImageType.HEIF -> true
                else -> false
            }
        }

        override fun equals(other: Any?) = other is Factory

        override fun hashCode() = javaClass.hashCode()
    }

    companion object {
        var displayProfile: ByteArray? = null
    }
}
