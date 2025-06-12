package com.example.volumerenderer

import android.graphics.Bitmap
import androidx.core.graphics.createBitmap

/* Manages the output image from volume rendering */
class Image(
    // The dimensions of `data`
    val size: IntArray,
    // Whether the 4th coordinate of data is transparency (true) or alpha (false)
    private val usingTransparency: Boolean
) {
    // A 4x`size[0]`x`size[1]` array of RGBA or RGBT colors
    val data: FloatArray3 = FloatArray3.zeros(intArrayOf(size[0], size[1], 4))

    // An array of pixel colors in ARGB format, the ideal format for Android Bitmaps
    // This array will be written to and copied to a bitmap whenever `SetBitmap()` is called
    private val pixels = IntArray(size[0] * size[1])

    // A bitmap image that will be rendered to
    private val bitmapConfig = Bitmap.Config.ARGB_8888 // The format of data for the bitmap
    private val bitmap : Bitmap = createBitmap(size[0], size[1], bitmapConfig)

    /* Saves the resulting image colors into a bitmap image, which can then be rendered using Jetpack Compose */
    fun updateBitmap() : Bitmap {
        pixels.fill(0)
        data.forEachMultiIndexed { indices, value ->
            // Flip the 4th coordinate if it stores transparency instead of alpha
            val curValue = (if (usingTransparency && indices[2] == 3) 1 - value else value).coerceIn(0f, 1f)

            if(curValue > 1) {
                println("(${indices[0]}, ${indices[1]}, ${indices[2]}) = $curValue")
            }

            // Figure out which byte block in ARGB format the current index corresponds to
            // Our current format is ABGR (this is what a hex value of our RGBA data looks like)
            val shiftIdx = if (indices[2] == 2) 0 else if (indices[2] == 0) 2 else indices[2]

            // Save the color to the bitmap using bitwise manipulation
            pixels[indices[1] + size[0] * indices[0]] = pixels[indices[1] + size[0] * indices[0]] or
                    ((curValue * 255).toInt() shl (8 * (shiftIdx % 4)))
        }
        bitmap.setPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return bitmap
    }
}