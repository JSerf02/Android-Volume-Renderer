package com.example.volumerenderer

import java.lang.Float.max
import kotlin.math.pow

class Ray(
    private val outputImage: Image,
    private val ctx: Context,
    private val convo: Convo = Convo(ctx)
) {
    // Use object pooling to prevent reallocations
    private val floatVecs : Array<Vector4> = Array(10) {
        Vector4.zeros()
    }

    private val floatVec3s : Array<Vector3> = Array(10) {
        Vector3.zeros()
    }

    // The resulting color of the ray, stored as 4 floats in [0, 1] representing either RGBA or RGBT
    val result: Vector4 = Vector4.zeros()

    // The number of valid results seen while traveling along the ray
    var numValidResults: Int = 0

    // Parameters used while computing rays
    private var horizIdx: Int = 0 // The horizontal index of the pixel we're rendering
    private var vertIdx: Int = 0 // The vertical index of the pixel we're rendering
    private val startPos: Vector3 = Vector3.zeros() // The starting position of the ray, in view space
    private val viewPos: Vector3 = Vector3.zeros() // The current position of the ray, in view space
    private val stepVec: Vector3 = Vector3.zeros() // A (view-space) vector that steps along the ray from one plane to the next
    private val viewerDir: Vector3 = Vector3.zeros() // The normalized direction vector pointing towards the viewer
    private var deltaOverU: Float = 1f // The exponent to use for opacity correction (see 02/13 slide 35)

    // Parameters for storing intermediary results
    private val curResult: Vector4 = Vector4.zeros() // The result of the current probe before blending
    private var numSteps: Int = 0 // The number of steps taken so far

    /* Cast a ray through the volume and blend all color results */
    fun go(horizIdx: Int, vertIdx: Int) {
        // Cast the ray
        start(horizIdx, vertIdx)
        while(step()) {
            continue
        }
        finish()

        // Save the output
        for(i in 0..3) {
            outputImage.data[vertIdx, horizIdx, i] = result[i]
        }
    }

    /* Start casting a ray at the indicated positions */
    private fun start(horizIdx: Int, vertIdx: Int) {
        // Store the pixel that the ray is currently rendering
        this.horizIdx = horizIdx
        this.vertIdx = vertIdx

        // Get the view-space vector from the center of the image plane to the pixel's position
        val camera = ctx.camera
        val planeVec = floatVecs[0].set(
            camera.width / 2 * lerp(-1f, 1f, -0.5f, horizIdx.toFloat(), camera.size[0].toFloat() - 0.5f),
            camera.height / 2 * lerp(-1f, 1f, -0.5f, vertIdx.toFloat(), camera.size[1].toFloat() - 0.5f),
        )

        // Set the starting position and step vector for the ray
        if(camera.isOrthographic || planeVec.isZero()) {
            /* The second condition above handles the case where the ray travels perfectly
             * straight with parallel projection and therefore moves parallel just like
             * with orthographic projection */
            // Rays move parallel so u and v remain the same
            startPos.set(planeVec[0], planeVec[1], camera.nearClipDistance)
            stepVec.set(0f, 0f, ctx.planeSeparation)
        }
        else {
            // Get the distance and direction the pixel is away from the origin in
            // the image plane
            val imgPlaneLen = planeVec.norm()
            val pixelDir = floatVecs[1].div(imgPlaneLen, planeVec)

            // Use similar triangles to figure out the distance away from the origin
            // in the near clip-plane that the ray should be
            val nearClipPlaneLen = imgPlaneLen * camera.nearClipDistance / camera.lookAtDistance
            startPos.set(nearClipPlaneLen * pixelDir[0], nearClipPlaneLen * pixelDir[1], camera.nearClipDistance)

            // Use similar triangles to figure out the change in the distance away from
            // the origin in plane
            val deltaLen = imgPlaneLen * ctx.planeSeparation / camera.lookAtDistance
            stepVec.set(deltaLen * pixelDir[0], deltaLen * pixelDir[1], ctx.planeSeparation)
        }

        // View position starts at starting position
        viewPos.copy(startPos)

        // Compute vector along ray towards viewer and exponent for opacity correction
        val worldStep = floatVecs[2].copy(stepVec).mul(camera.vToW)
        deltaOverU = worldStep.norm()
        viewerDir.copy(worldStep.div(-deltaOverU))
        deltaOverU /= ctx.transFun.unitStep

        // Reset step counts
        numValidResults = 0
        numSteps = 0
    }

    /* Calculate lighting at the current position using Blinn-Phong */
    private fun blinnPhong(rgb: Vector3) : Vector3 {
        // Give useful info easier names
        val lighting = ctx.lighting
        val gradient = convo.gradient

        // Add the ambient light
        val result = floatVec3s[0].mul(lighting.kAmbient, rgb)

        // Stop if not including other light sources
        if((lighting.kSpecular == 0f && lighting.kDiffuse == 0f) || gradient.isZero()) {
            return result
        }

        // Compute the normal
        val normal = floatVec3s[1].normalize(gradient).mul(-1f)

        for(lightIdx in 0..<lighting.numLights) {
            // Diffuse lighting
            val color = floatVec3s[2].set(lighting.colors[lightIdx, 0], lighting.colors[lightIdx, 1], lighting.colors[lightIdx, 2])
            val direction = floatVec3s[3].set(lighting.directions[lightIdx, 0], lighting.directions[lightIdx, 1], lighting.directions[lightIdx, 2])
            result.add(
                floatVec3s[4].mul(color, rgb)
                    .mul(max(0f, normal.dot(direction)))
                    .mul(lighting.kDiffuse)
            )

            // Specular lighting
            result.add(
                floatVec3s[5].mul(
                    max(0f,
                        normal.dot(
                            direction.add(viewerDir).normalize()
                        )
                    ).pow(lighting.shininess),
                    color
                ).mul(lighting.kSpecular)
            )
        }
        return result
    }

    /* Calculate the value at the current point along the ray */
    private fun sample() : Boolean {
        // Use homogenous coordinates to compute the world-space position
        val worldPos = floatVecs[3]
            .copy(viewPos, 1f)
            .mul(ctx.camera.vToW)

        // Compute the convolution result at the current point
        convo.eval(worldPos.toVector3()) // Allocation will likely get optimized out by compiler because Vector3 is a value class
        if(!convo.inside) {
            return false
        }

        // Get the base color from the transfer function's LUT
        val lutIdx = quantize(ctx.transFun.minValue, convo.value, ctx.transFun.maxValue, ctx.transFun.length)
        val rgba = floatVecs[3].set(ctx.transFun.rgbaData[lutIdx, 0], ctx.transFun.rgbaData[lutIdx, 1], ctx.transFun.rgbaData[lutIdx, 2], ctx.transFun.rgbaData[lutIdx, 3])
        rgba[3] *= ctx.transFun.levoy(convo.value, convo.gradient.norm())

        // Correct opacity (see 02/13 slide 35)
        rgba[3] = rgba[3].coerceIn(0f, 1f)
        rgba[3] = 1 - (1f - rgba[3]).pow(deltaOverU)

        // Save the base color
        if(ctx.renderType == RenderType.RGBA_MAT) {
            curResult.copy(rgba)
            return true
        }

        // Apply lighting
        if(ctx.blendMode == BlendMode.OVER && rgba[3] == 0f) {
            curResult.zero()
        }
        else {
            // Apply Blinn_Phong lighting
            val rgb = blinnPhong(rgba.toVector3()) // Compiler should optimize out memory allocation because this is a value class

            // Multiply by depth cue
            rgba.copy(rgb.mul(
                floatVec3s[4].lerp(
                    ctx.lighting.depthCueNear, ctx.lighting.depthCueFar,
                    ctx.camera.nearClipDistance, viewPos[2], ctx.camera.farClipDistance
                )
            ), rgba[3])
        }

        // Save the lit result in RGBA format
        if(ctx.renderType == RenderType.RGBA_LIT) {
            curResult.copy(rgba)
            return true
        }

        // Save the lit result in (premultiplied) RGBT format
        val a = 1 - rgba[3]
        curResult.mul(rgba[3], rgba)[3] = a
        return true
    }

    /* Calculate the value at the current point on the ray, then move to the next point */
    private fun step() : Boolean {
        // Stop the ray if past the far-clip plane
        if(viewPos[2] > ctx.camera.farClipDistance) {
            return false
        }

        // Sample in the current plane
        if(sample()) {
            result.copy(
                if(numValidResults == 0) {
                    curResult
                } else {
                    ctx.blender.blend(result, curResult)
                }
            )
            numValidResults++
        }

        // Stop early if alpha is near 1 when using over blending
        if(ctx.blendMode == BlendMode.OVER && numValidResults > 0 && 1 - result[3] > ctx.transFun.alphaNear1) {
            result.div(1 - result[3])[3] = 0f
            return false
        }

        // Move to the next plane
        numSteps++
        viewPos.scaleAdd(1f, startPos, numSteps.toFloat(), stepVec)
        return true
    }

    /* Stop the ray and save the results in the correct format */
    private fun finish() {
        if(numValidResults == 0 && ctx.renderType == RenderType.RGBT) {
            // No results in rgbt mode => (0, 0, 0, 1) output
            result.set(0f, 0f, 0f, 1f)
        }
        else if(numValidResults == 0) {
            // No results in other modes => (0, 0, 0, 0) output
            // Note that this is different than the output for this case in the actual p3 (nan(0))
            result.zero()
        }
        else if(ctx.blendMode == BlendMode.MEAN) {
            result.div(numValidResults.toFloat())
        }
    }
}