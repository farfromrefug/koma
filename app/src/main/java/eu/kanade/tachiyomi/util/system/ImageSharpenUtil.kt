package eu.kanade.tachiyomi.util.system

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicConvolve3x3

/**
 * Utility class for image sharpening using RenderScript.
 * This is used for Android API < 31 where RenderEffect is not available.
 * 
 * Uses ScriptIntrinsicConvolve3x3 with an unsharp mask kernel for efficient,
 * memory-optimized image sharpening.
 */
@Suppress("DEPRECATION")
object ImageSharpenUtil {
    
    /**
     * Applies a sharpening effect to the bitmap using RenderScript.
     * Only used on API < 31 where RenderEffect is not available.
     * 
     * @param context Android context for RenderScript initialization
     * @param inputBitmap The source bitmap to sharpen (will not be modified)
     * @param scale Sharpening intensity (0.0 to 2.0)
     * @return A new sharpened bitmap, or the original if sharpening fails or is not needed
     */
    fun sharpenBitmap(context: Context, inputBitmap: Bitmap, scale: Float): Bitmap {
        // Only use RenderScript sharpening on API < 31
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S || scale <= 0f) {
            return inputBitmap
        }
        
        var renderScript: RenderScript? = null
        var inputAllocation: Allocation? = null
        var outputAllocation: Allocation? = null
        
        try {
            // Create mutable output bitmap with same config
            val config = inputBitmap.config ?: Bitmap.Config.ARGB_8888
            val outputBitmap = inputBitmap.copy(config, true)
            
            // Initialize RenderScript
            renderScript = RenderScript.create(context)
            
            // Create allocations - these are memory-efficient wrappers around the bitmaps
            inputAllocation = Allocation.createFromBitmap(
                renderScript, 
                inputBitmap,
                Allocation.MipmapControl.MIPMAP_NONE,
                Allocation.USAGE_SCRIPT,
            )
            outputAllocation = Allocation.createFromBitmap(
                renderScript, 
                outputBitmap,
                Allocation.MipmapControl.MIPMAP_NONE,
                Allocation.USAGE_SCRIPT,
            )
            
            // Create convolution script
            val script = ScriptIntrinsicConvolve3x3.create(renderScript, Element.U8_4(renderScript))
            
            // Create sharpening kernel using unsharp mask technique
            val kernel = createSharpenKernel(scale)
            script.setCoefficients(kernel)
            
            // Apply convolution
            script.setInput(inputAllocation)
            script.forEach(outputAllocation)
            
            // Copy result to output bitmap
            outputAllocation.copyTo(outputBitmap)
            
            return outputBitmap
        } catch (e: Exception) {
            // If sharpening fails, return original bitmap
            return inputBitmap
        } finally {
            // Clean up RenderScript resources to free memory
            inputAllocation?.destroy()
            outputAllocation?.destroy()
            renderScript?.destroy()
        }
    }
    
    /**
     * Creates a 3x3 sharpening kernel based on the scale.
     * 
     * The kernel uses an unsharp mask technique:
     * - Center pixel is enhanced
     * - Neighboring pixels (cross pattern) are subtracted
     * 
     * Kernel format (3x3):
     * [  0,  -s,   0 ]
     * [ -s, 1+4s, -s ]
     * [  0,  -s,   0 ]
     * 
     * Where s = scale * 0.5 to keep the effect subtle and controllable
     */
    private fun createSharpenKernel(scale: Float): FloatArray {
        // Scale the effect to be controllable (0.0-2.0 maps to 0.0-1.0 kernel strength)
        val s = scale * 0.5f
        
        return floatArrayOf(
            0f,    -s,        0f,
            -s,    1f + 4*s,  -s,
            0f,    -s,        0f,
        )
    }
}
