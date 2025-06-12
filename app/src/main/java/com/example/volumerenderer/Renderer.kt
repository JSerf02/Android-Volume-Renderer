package com.example.volumerenderer

import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread

class Renderer(
    private val ctx: Context
) {
    private val outputImage = Image(ctx.camera.size, ctx.renderType == RenderType.RGBT)

    /* Render the volume in the current coroutine */
    fun renderSynchronously() : Bitmap {
        println("Rendering synchronously")
        val ray = Ray(outputImage, ctx)
        for(j in 0..<outputImage.size[0]) {
            for(i in 0..<outputImage.size[1]) {
                ray.go(i, j)

                // Print logs
                // This is the slowest mode, so it is okay if we make it even slower with printing
                if(i == outputImage.size[1] - 1) {
                    println("Finished scanline $j! Output color: ${ray.result}")
                }
            }
        }
        println("Done rendering!")
        return outputImage.updateBitmap()
    }

    /* Render the volume using coroutines to render multiple scanlines at once */
    suspend fun renderWithCoroutines() : Bitmap = coroutineScope {
        println("Rendering with coroutines!")

        // Use an AtomicInteger to efficiently manage scanlines without needing a mutex
        val slowIdx = AtomicInteger(0)

        // Create coroutines and have each one run scanlines
        val jobs = List(ctx.numThreads) {
            launch(Dispatchers.Default) {
                val ray = Ray(outputImage, ctx)
                while(true) {
                    val curSlowIdx = slowIdx.incrementAndGet()
                    if(curSlowIdx >= outputImage.size[0]) {
                        return@launch
                    }
                    for(fastIdx in 0..<outputImage.size[1]) {
                        ray.go(fastIdx, curSlowIdx)
                    }
                }
            }
        }
        jobs.joinAll()

        // Save and return the result
        // Because we are in coroutineScope, the last line is returned
        outputImage.updateBitmap()
    }

    fun renderWithThreads() : Bitmap {
        println("Rendering with threads!")

        // Use an AtomicInteger to efficiently manage scanlines without needing a mutex
        val slowIdx = AtomicInteger(0)

        // Create threads and have each one run scanlines
        val threads = List(ctx.numThreads) {
            thread {
                val ray = Ray(outputImage, ctx)
                while(true) {
                    val curSlowIdx = slowIdx.incrementAndGet()
                    if(curSlowIdx >= outputImage.size[0]) {
                        return@thread
                    }
                    for(fastIdx in 0..<outputImage.size[1]) {
                        ray.go(fastIdx, curSlowIdx)
                    }
                }
            }
        }

        // Wait for all threads to finish
        for (thread in threads) {
            try {
                thread.join()
            } catch (e: InterruptedException) {
                println("Thread interrupted: ${thread.name}")
                Thread.currentThread().interrupt() // Re-interrupt the current thread
            }
        }

        // Save and return the result
        return outputImage.updateBitmap()
    }
}