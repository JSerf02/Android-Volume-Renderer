package com.example.volumerenderer

import kotlin.math.abs
import kotlin.math.max
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.ExceptionInInitializerError
import kotlin.io.println

/* A color map, but with a fancy name :) */
class TransferFunction(
    applicationContext: android.content.Context,
    val unitStep : Float, // The "unit" length to use when computing opacity correction as a function of inter-sample distance
    val alphaNear1: Float, // Rays terminate early when accumulated alpha surpasses this threshold
    /* Note: I couldn't get NRRD parsing working, so I made my own file format for NRRDs called JNRRD for Jacob NRRD */
    lutPath: String, // The file path to the .jnrrd file containing LUT data
    levoyPath: String? = null // The file path to the .txt file containing levoy data
) {
    val minValue: Float // The minimum value for the domain of the RGBA LUT
    val maxValue: Float // The maximum value for the domain of the RGBA LUT
    val length: Int // The number of entries in the RGBA LUT
    val rgbaData: FloatArray2 // 4x`length` array of RGBA values, represented as floats in [0, 1]
    /* Multiplies the opacity of the transfer function with Levoy's "Iso-value contour surface" opacity
     * function (from 1988 "Display of Surfaces from Volume Data" paper; the triangular shapes in
     * value/gradmag space) */
    val levoyVra: FloatArray2?

    init {
        /* Parse the levoy data if it exists */
        if(levoyPath != null) {
            /* Levoy file inputted, so data must exist */
            try {
                // Extract all lines from the file
                // This is very inefficient but it's only happening once so whatever
                val levoyStream = applicationContext.assets.open(levoyPath)
                val levoyReader = BufferedReader(InputStreamReader(levoyStream))
                val levoyList : MutableList<FloatArray> = mutableListOf()
                levoyReader.useLines { lines->
                    lines.forEach { line ->
                        // Split on one+ whitespace characters
                        val floats = line.split("\\s+".toRegex())
                            .map { it.toFloat() }

                        // Make sure the line has the expected format
                        if(floats.size != 3) {
                            throw IllegalArgumentException("Line from levoy file not formatted correctly: $floats")
                        }

                        // Save the line data
                        levoyList.add(floats.toFloatArray())
                    }
                }
                levoyVra = FloatArray2.zeros(intArrayOf(levoyList.size, 4))
                levoyList.forEachIndexed { i, floats ->
                    floats.forEachIndexed { j, float ->
                        levoyVra[i, j] = float
                    }
                }
            }
            catch(e: Exception) {
                println("Error reading levoy file: ${e.message}")
                throw ExceptionInInitializerError()
            }
        }
        else {
            /* No levoy file */
            levoyVra = null
        }

        /* Parse the LUT */
        try {
            // Extract all lines from the file
            val lutStream = applicationContext.assets.open(lutPath)
            val lutReader = BufferedReader(InputStreamReader(lutStream))
            lutReader.useLines { lines ->
                val iterator = lines.iterator()

                // The first line of the file must contain the sizes
                val sizesStr = iterator.next()
                if(sizesStr.substring(0, 5) != "sizes") {
                    throw IllegalArgumentException("Line from LUT file not formatted correctly: $sizesStr")
                }
                length = sizesStr.substring(6).split("\\s+".toRegex())
                    .map { it.toInt() }[0]

                // The second line of the file must contain the bounds
                val boundsStr = iterator.next()
                if(boundsStr.substring(0, 6) != "bounds") {
                    throw IllegalArgumentException("Line from LUT file not formatted correctly: $boundsStr")
                }
                val bounds = boundsStr.substring(7).split("\\s+".toRegex())
                    .map { it.toFloat() }
                minValue = bounds[0]
                maxValue = bounds[1]

                // The remaining lines contain the colors
                val rgbaList : MutableList<FloatArray> = mutableListOf()
                iterator.forEach { line ->
                    // Split on one+ whitespace characters
                    val floats = line.split("\\s+".toRegex())
                        .map { it.toFloat() }

                    // Make sure the line has the expected format
                    if(floats.size != 4) {
                        throw IllegalArgumentException("Line from LUT file not formatted correctly: $floats")
                    }

                    // Save the line data
                    rgbaList.add(floats.toFloatArray())
                }
                rgbaData = FloatArray2.zeros(intArrayOf(rgbaList.size, 4))
                rgbaList.forEachIndexed { i, floats ->
                    floats.forEachIndexed { j, float ->
                        rgbaData[i, j] = float
                    }
                }
            }
        }
        catch(e: Exception) {
            println("Error reading LUT file: ${e.message}")
            throw ExceptionInInitializerError()
        }

    }

    fun levoy(value: Float, gradientMag: Float): Float {
        if(levoyVra == null) {
            return 1f
        }
        // Idk what a levoy is so I'm not questioning anything here
        var transp = 1f
        for(i in 0..<levoyVra.size[0]) {
            val vv = levoyVra[i, 0]
            val rr = levoyVra[i, 1]
            val am = levoyVra[i, 2]
            var aa : Float
            if(gradientMag != 0f) {
                aa = 1 - (1 / rr) * abs((value - vv) / gradientMag)
                aa = max(0f, aa)
            }
            else {
                aa = if (value == vv) 1f else 0f
            }
            aa *= am
            transp *= (1 - aa)
        }
        return 1 - transp
    }
}