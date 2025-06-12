package com.example.volumerenderer

import kotlin.math.round

// Inverse lerp
fun unlerp(x1: Float, x: Float, x2: Float) = (x - x1) / (x2 - x1)

// 3 argument lerp
fun lerp(a: Float, b: Float, t: Float) = (1 - t) * a + t * b

// 5 argument lerp
fun lerp(y1: Float, y2: Float, x1: Float, x: Float, x2: Float) = lerp(y1, y2, unlerp(x1, x, x2))

// Quantize, based on mprQuantize
fun quantize(min: Float, value: Float, max: Float, num: Int) : Int {
    val sampleLen = (max - min) / num
    val x = (value - min - sampleLen / 2)
    val x2 = (max - min) - sampleLen
    return round(lerp(0f, num - 1f, 0f, x, x2)).toInt().coerceIn(0, num-1)
}

// Convert an HSV color to RGB format
// Stores the result in `hsv`
fun hsvToRgb(hsv: Vector3) : Vector3 {
    if(hsv[1] == 0f) {
        return hsv.setAll(hsv[2])
    }
    if(hsv[0] == 1f) {
        hsv[0] = 0f
    }
    hsv[0] *= 6f
    val sextant = hsv[0] as Int
    val frac = hsv[0] - sextant
    val vsf = hsv[1] * hsv[2] * frac
    val min = hsv[2] * (1 - hsv[1])
    val mid1 = min + vsf
    val mid2 = hsv[2] - vsf
    return when(sextant) {
        0 -> hsv.set(hsv[2], mid1, min)
        1 -> hsv.set(mid2, hsv[2], min)
        2 -> hsv.set(min, hsv[2], mid1)
        3 -> hsv.set(min, mid2, hsv[2])
        4 -> hsv.set(mid1, min, hsv[2])
        else -> hsv.set(hsv[2], min, mid2)
    }
}

// Convert a color to RGB if it is not already RGB
fun ensureRgb(color: Vector3, isHsv: Boolean) = if(isHsv) hsvToRgb(color) else color