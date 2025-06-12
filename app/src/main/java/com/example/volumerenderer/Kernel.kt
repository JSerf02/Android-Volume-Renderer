package com.example.volumerenderer

import kotlin.math.abs

/* A reconstruction kernel for convolution.
 * This function is nonzero only within [-`support`/2, `support`/2] for integer `support` which
 * may be odd or even (but always positive).
 */
interface Kernel {
    val name: String // Short identifying string
    val support: Int // Number of samples needed for convolution
    val derivative: Kernel // The derivative of this kernel, will point back to itself when this is the zero kernel

    // Evaluates the kernel once
    fun eval(x: Float): Float

    // Evaluates the kernel `support` times around x in [0, 1) for even `support` and in [-0.5, 0.5) for odd support
    // Using Vector4 here as all kernels we support have support <= 4
    fun apply(output: Vector4, x: Float): Vector4
}

/* Definitions of various kernels */
/*------------------------------------------------------------------------------------------------*/

// Returns zero everywhere
class ZeroKernel(override val support: Int) : Kernel {
    override val name = "Zero"
    override val derivative = this
    override fun eval(x: Float) = 0f
    override fun apply(output: Vector4, x: Float) = output.zero()
}

// The Box kernel, for nearest-neighbor interpolation
class BoxKernel : Kernel {
    override val name = "Box"
    override val support = 1
    override val derivative = ZeroKernel(support)
    override fun eval(x: Float) = if (x < -0.5f) 0f else if (x < 0.5f) 1f else 0f
    override fun apply(output: Vector4, x: Float) = output.set(1f)
}

// The Tent kernel, for Linear interpolation
class TentKernel : Kernel {
    override val name = "Tent"
    override val support = 2
    override val derivative = object : Kernel {
        // The derivative of the Tent kernel
        override val name = "DTent"
        override val support = 2
        override val derivative = ZeroKernel(support)
        override fun eval(x: Float) = if (x < -1) 0f else if (x < 0) 1f else if (x < 1) -1f else 0f
        override fun apply(output: Vector4, x : Float) = output.set(-1f, 1f)
    }
    override fun eval(x: Float) = if (x < -1) 0f else if (x < 0) x + 1 else if (x < 1) 1 - x else 0f
    override fun apply(output: Vector4, x: Float) = output.set(1 - x, x)
}

// The B-Spline 2 Kernel, for creating smooth reconstructions but not interpolating
class BSpline2Kernel : Kernel {
    override val name = "BSpline2"
    override val support = 3
    override val derivative = object : Kernel {
        // First derivative of the BSpline2 Kernel
        override val name = "DBSpline2"
        override val support = 3
        override val derivative = object : Kernel {
            // Second derivative of the BSpline2 Kernel
            override val name = "DDBSpline2"
            override val support = 3
            override val derivative = ZeroKernel(support)
            override fun eval(x: Float) : Float {
                val absX = abs(x)
                return if (absX < 0.5)
                        -2f
                    else if (absX < 1.5)
                        1f
                    else 0f
            }
            override fun apply(output: Vector4, x: Float) = output.set(1f, -2f, 1f)
        }
        override fun eval(x: Float) : Float {
            val absX = abs(x)
            val res = if (absX < 0.5)
                    -2f * absX
                else if (absX < 1.5)
                    x - 1.5f else 0f
            return if(x < 0) -res else res
        }
        override fun apply(output: Vector4, x: Float) = output.set(x - 0.5f, -2f * x, x + 0.5f)
    }
    override fun eval(x: Float) : Float {
        val absX = abs(x)
        return if (absX < 0.5f)
                3f/4f * absX * absX
            else if (absX < 1.5f)
                1f/8f + (absX - 1) * (absX - 1) / 2f
            else 0f
    }
    override fun apply(output: Vector4, x: Float): Vector4 = output.set(
        1f/8f + x * (-0.5f + x / 2f),
        3f/4f - x * x,
        1f/8f + x * (0.5f + x / 2f)
    )
}

class BSpline3Kernel : Kernel {
    override val name = "BSpline3"
    override val support = 4
    override val derivative = object : Kernel {
        override val name = "dBSpline3"
        override val support = 4
        override val derivative = object : Kernel {
            override val name = "ddBSpline3"
            override val support = 4
            override val derivative = object : Kernel {
                override val name = "dddBSpline3"
                override val support = 4
                override val derivative = ZeroKernel(support)
                override fun eval(x: Float): Float {
                    val absX = abs(x)
                    val ret = if(absX < 0) 3f else if (absX < 2) -1f else 0f
                    return if (x < 0) -ret else ret
                }

                override fun apply(output: Vector4, x: Float): Vector4 =
                    output.set(-1f, 3f, -3f, 1f)
            }

            override fun eval(x: Float): Float {
                val absX = abs(x)
                return if(absX < 1) -2f + 3 * absX else if (absX < 2) 1 - (absX - 1) else 0f
            }

            override fun apply(output: Vector4, x: Float): Vector4 =
                output.set(1 - x, -2 + 3 * x, 1 - 3 * x, x)
        }

        override fun eval(x: Float): Float {
            val absX = abs(x)
            val ret = if(absX < 1)
                    absX * (-2f + absX * 1.5f)
                else if (absX < 2)
                    -0.5f + (absX - 1f) * (1 - (absX - 1f) / 2f)
                else
                    0f
            return if (x < 0) -ret else ret
        }

        override fun apply(output: Vector4, x: Float): Vector4 =
            output.set(
                -.5f + x * (1 - x / 2),
                x * (-2 + x * 3 / 2),
                0.5f + x * (1 - 3 * x / 2),
                x * x / 2
            )
    }

    override fun eval(x: Float): Float {
        val absX = abs(x)
        return if (absX < 1) 2f / 3f + absX * absX * (-1 + absX / 2) else if (absX < 2) 1f / 6f + (absX - 1) * (-0.5f + (absX - 1) * (0.5f - (absX - 1) / 6f)) else 0f
    }

    override fun apply(output: Vector4, x: Float): Vector4 =
        output.set(
            1f/6f + x * (-0.5f + x * (0.5f - x / 6)),
            2f/3f + x * x * (-1 + x / 2),
            1f/6f + x * (0.5f + x * (0.5f - x / 2)),
            x * x * x/6
        )
}
