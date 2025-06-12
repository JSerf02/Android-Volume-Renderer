package com.example.volumerenderer

/* All info necessary for volume rendering, not thread safe */
class Context(
    val volume: Volume, // The volume to render, represented as a scalar field
    val kernel: Kernel, // The kernel to use for convolution
    val planeSeparation: Float, // The distance between sampling planes in the view volume
    val renderType: RenderType, // The format of the output results of volume rendering
    val blendMode: BlendMode, // The type of blending to use
    val camera: Camera, // The parameters for the camera
    val transFun: TransferFunction, // A color map
    val lighting: Lighting, // The lighting information for the scene
    val numThreads: Int = 0, // The number of threads other than the main thread to use when rendering
) {
    val blender : Blender = getBlender(blendMode) // The object that blends colors from different planes
    val convoParams: ConvolutionParams // Helpful parameters for computing convolutions

    init {
        // Make sure blend mode is compatible with render mode
        assert(blendMode != BlendMode.OVER || renderType == RenderType.RGBT) {
            "Blend mode $blendMode is not compatible with render type $renderType"
        }
        // Make sure plane separation is reasonable
        assert(planeSeparation > 0) {
            "Plane separation must be positive, currently planeSeparation=$planeSeparation<=0"
        }
        // Set iteration bounds based on the kernel
        val nOffset: Float
        val minIter: Int
        val maxIter: Int
        if(kernel.support % 2 == 0) {
            nOffset = 0f
            maxIter = kernel.support / 2
            minIter = 1 - maxIter
        }
        else {
            nOffset = 0.5f
            maxIter = (kernel.support - 1) / 2
            minIter = -maxIter
        }
        val numIters: Int = maxIter - minIter

        // Save iteration bounds to `convoParams`
        convoParams = ConvolutionParams(nOffset, minIter, maxIter, numIters)
    }
}

/* Types of results to return through volume rendering */
enum class RenderType {
    RGBA_MAT,
    RGBA_LIT,
    RGBT
}

data class ConvolutionParams(
    val nOffset: Float,
    val minIter: Int,
    val maxIter: Int,
    val numIters: Int
)