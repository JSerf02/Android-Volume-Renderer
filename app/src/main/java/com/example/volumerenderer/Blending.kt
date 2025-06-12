package com.example.volumerenderer

import kotlin.math.max

enum class BlendMode {
    MAX,
    SUM,
    MEAN,
    OVER
}

interface Blender {
    fun blend(accum: Vector4, cur: Vector4) : Vector4
}

class BlendMax: Blender {
    override fun blend(accum: Vector4, cur: Vector4) : Vector4 = accum.set(
        max(accum[0], cur[0]), max(accum[1], cur[1]), max(accum[2], cur[2]), max(accum[3], cur[3])
    )
}

class BlendSum: Blender {
    override fun blend(accum: Vector4, cur: Vector4) : Vector4 = accum.add(cur)
}

class BlendOver: Blender {
    override fun blend(accum: Vector4, cur: Vector4) : Vector4 {
        cur.mul(accum[3])
        accum[3] = 0f
        return accum.add(cur)
    }
}

fun getBlender(blendMode: BlendMode) : Blender= when(blendMode) {
    BlendMode.MAX -> BlendMax()
    BlendMode.OVER -> BlendOver()
    else -> BlendSum()
}