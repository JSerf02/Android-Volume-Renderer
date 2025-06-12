package com.example.volumerenderer

import java.io.BufferedReader
import java.io.InputStreamReader

/*
 * A container for the oriented volume data.
 * Data is always scalar (we are not handling multiple values per voxel), and the type of the scalar
 * is always Float
*/
class Volume(
    applicationContext: android.content.Context,
    /* Note: I couldn't get NRRD parsing working, so I made my own file format for NRRDs called JNRRD for Jacob NRRD */
    filePath: String // The path of the JNRRD file containing the volume data
) {
    val size : IntArray // The number of samples collected along each axis
    val iToW : Matrix4x4 // A matrix that maps from the volume's image-space to world-space
    val data : FloatArray3 // The scalar data per voxel

    init {
        /* Read the volume file */
        try {
            // Extract all lines from the file
            val stream = applicationContext.assets.open(filePath)
            val reader = BufferedReader(InputStreamReader(stream))
            reader.useLines { lines ->
                val iterator = lines.iterator()

                // The first line of the file must contain the sizes
                val sizesStr = iterator.next()
                if(sizesStr.substring(0, 5) != "sizes") {
                    throw IllegalArgumentException("Line from volume file not formatted correctly: $sizesStr")
                }
                size = sizesStr
                    .substring(6, sizesStr.length)
                    .split("\\s+".toRegex())
                    .map { it.toInt() }
                    .toIntArray()

                // The second line of the file must contain the iToW matrix
                val iToWStr = iterator.next()
                if(iToWStr.substring(0, 4) != "itow") {
                    throw IllegalArgumentException("Line from volume file not formatted correctly: $iToWStr")
                }
                iToW = Matrix4x4(
                    iToWStr.substring(5, iToWStr.length)
                        .split("\\s+".toRegex())
                        .map { it.toFloat() }
                        .toFloatArray()
                )

                // The remaining lines contain the colors
                data = FloatArray3.zeros(size)
                var idx = 0
                iterator.forEach { line ->
                    // Get the x, y, and z indices into `data` for the current line
                    val idxX = idx % size[2]
                    val idxYZ = idx / size[2]
                    val idxY = idxYZ % size[1]
                    val idxZ = idxYZ / size[1]
                    idx++

                    // Split on one+ whitespace characters
                    val floats = line.split("\\s+".toRegex())
                        .map { it.toFloat() }

                    // Make sure the line has the expected format
                    if(floats.size != 1) {
                        throw IllegalArgumentException("Line from volume file not formatted correctly: $floats")
                    }

                    // Save the line data
                    data[idxZ, idxY, idxX] = floats[0]
                }
            }
        }
        catch(e: Exception) {
            println("Error reading volume file: ${e.message}")
            throw ExceptionInInitializerError()
        }
    }

    // A matrix that maps from world-space to the volume's image-space
    val wToI : Matrix4x4 = Matrix4x4.zeros().inverse(iToW)

    // A matrix that maps a gradient from the volume's image-space to world-space
    // Equal to M^(-T) where M is the upper 3x3 matrix of iToW
    val gradItoW : Matrix3x3 = Matrix3x3.zeros()
        .upper3x3(iToW)
        .inverse()
        .transpose()
}