package com.example.volumerenderer

/* Do convolution with a kernel */
data class Convo (
    private val ctx: Context,
    val indexPos : Vector3 = Vector3.zeros(),
    var inside : Boolean = true,
    var value : Float = 0.0f,
    val gradient : Vector3 = Vector3.zeros()
) {
    // Use object pooling to prevent reallocations
    private val floatVecs : Array<Vector4> = Array(10) {
        Vector4.zeros()
    }
    private val intVec = IntArray(4)
    fun eval(worldPos: Vector3) {
        // Store iteration parameters for easier access
        val minIter = ctx.convoParams.minIter
        val maxIter = ctx.convoParams.maxIter

        // Get the index-position from the world-position
        indexPos.copy(
            floatVecs[0]
            .set(worldPos[0], worldPos[1], worldPos[2], 1f)
            .mul(ctx.volume.wToI)
        )

        // Find the starting index
        intVec[0] = 0
        intVec[1] = 0
        intVec[2] = 0
        intVec[3] = 0
        val n = intVec
        val alpha = floatVecs[1].zero()
        for(i in 0..2) {
            n[i] = (indexPos[i] + ctx.convoParams.nOffset).toInt()
            alpha[i] = indexPos[i] - n[i]

            // Make sure the index is within bounds
            if(n[i] + minIter < 0 || n[i] + maxIter >= ctx.volume.size[2 - i]) {
                inside = false
                return
            }
        }
        inside = true

        // Cache kernel and kernel' results to prevent recomputations
        val kernel = ctx.kernel
        val kernCacheX = kernel.apply(floatVecs[2], alpha[0])
        val kernCacheY = kernel.apply(floatVecs[3], alpha[1])
        val kernCacheZ = kernel.apply(floatVecs[4], alpha[2])
        val dKernel = ctx.kernel.derivative
        val dKernCacheX = dKernel.apply(floatVecs[5], alpha[0])
        val dKernCacheY = dKernel.apply(floatVecs[6], alpha[1])
        val dKernCacheZ = dKernel.apply(floatVecs[7], alpha[2])

        // Compute the convolution and index-space gradient
        value = 0f
        gradient.zero()
        for(iz in minIter..maxIter) {
            for(iy in minIter..maxIter) {
                for(ix in minIter..maxIter) {
                    // Get the current scalar from the image
                    val scalar = ctx.volume.data[n[2] + iz, n[1] + iy, n[0] + ix]

                    // Get the index into each of the kernel caches
                    val cacheIdxX = ix - minIter
                    val cacheIdxY = iy - minIter
                    val cacheIdxZ = iz - minIter

                    // Get the current values from the kernel cache
                    val kernX = kernCacheX[cacheIdxX]
                    val kernY = kernCacheY[cacheIdxY]
                    val kernZ = kernCacheZ[cacheIdxZ]
                    val dKernX = dKernCacheX[cacheIdxX]
                    val dKernY = dKernCacheY[cacheIdxY]
                    val dKernZ = dKernCacheZ[cacheIdxZ]

                    // Add current value to results following the convolution formula
                    value += scalar * kernX * kernY * kernZ
                    gradient[0] += scalar * dKernX * kernY * kernZ
                    gradient[1] += scalar * kernX * dKernY * kernZ
                    gradient[2] += scalar * kernX * kernY * dKernZ
                }
            }
        }

        // Convert the gradient to world-space
        gradient.mul(ctx.volume.gradItoW)
    }
}