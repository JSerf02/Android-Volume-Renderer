package com.example.volumerenderer

import java.io.BufferedReader
import java.io.InputStreamReader

/* Parameters defining the (directional + ambient) lights in the scene */
class Lighting(
    applicationContext: android.content.Context,
    camera: Camera,
    filePath: String?,
    val kAmbient: Float = 0f, // The ambient lighting factor
    val kDiffuse: Float = 0f, // The diffuse lighting factor
    val kSpecular: Float = 0f, // The specular lighting factor
    val shininess: Float = 0f, // The specular lighting exponent
    val depthCueNear: Vector3 = Vector3(1f, 1f, 1f), // The near clip-plane depth cue
    val depthCueFar: Vector3 = Vector3(1f, 1f, 1f), // The far clip-plane depth cue
) {
    val numLights: Int // The number of directional lights
    val colors: FloatArray2 // The colors of each of the directional lights
    val directions: FloatArray2// The directions of each of the directional lights
    init {
        if(filePath.isNullOrEmpty()) {
            // No lighting file inputted, using no lights
            numLights = 0
            colors = FloatArray2.zeros(intArrayOf(1, 3))
            directions = FloatArray2.zeros(intArrayOf(1, 3))
        }
        else {
            // Parse the inputted lighting file
            try {
                // Extract all lines from the file
                val stream = applicationContext.assets.open(filePath)
                val reader = BufferedReader(InputStreamReader(stream))
                reader.useLines { lines ->
                    // The first line tells whether the color is in RGB or HSV space
                    val iterator = lines.iterator()
                    val isHsv =  when(val colorSpaceStr = iterator.next().substring(8)) {
                        "rgb" -> false
                        "hsv" -> true
                        else -> throw IllegalArgumentException("Color space is not recognized: $colorSpaceStr")
                    }

                    // Read the colors and directions from the file
                    val colorsList : MutableList<Vector3> = mutableListOf()
                    val directionsList : MutableList<Vector3> = mutableListOf()
                    iterator.forEach { line ->
                        // Skip the line if it contains metadata
                        if(line[0] == '#') {
                            return@forEach
                        }

                        // Split on one+ whitespace characters
                        val floats = line.split("\\s+".toRegex())
                            .map { it.toFloat() }

                        // Make sure the line has the expected format
                        if(floats.size != 7) {
                            throw IllegalArgumentException("Line from lighting file not formatted correctly: $floats")
                        }

                        // Save the line data
                        val viewSpace = (floats[6] == 1f)
                        // Because Vector3 is a value class, this shouldn't allocate memory
                        colorsList.add(ensureRgb(Vector3(floats.subList(0, 3).toFloatArray()), isHsv))
                        val direction = Vector4(floats[3], floats[4], floats[5], 0f)
                        directionsList.add(
                            (if(viewSpace) direction.mul(camera.vToW) else direction)
                            .normalize()
                            .toVector3()
                        )
                    }

                    // Save all the results in the respective D2Arrays
                    numLights = colorsList.size
                    colors = FloatArray2.zeros(intArrayOf(numLights, 3))
                    directions = FloatArray2.zeros(intArrayOf(numLights, 3))
                    for(i in 0..<numLights) {
                        for(j in 0..2) {
                            colors[i, j] = colorsList[i][j]
                            directions[i, j] = directionsList[i][j]
                        }
                    }
                }

            } catch (e: Exception) {
                println("Error reading file: ${e.message}")
                throw ExceptionInInitializerError()
            }
        }
    }
}