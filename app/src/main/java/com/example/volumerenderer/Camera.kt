package com.example.volumerenderer

import kotlin.math.PI
import kotlin.math.tan

/* All the properties for defining a view frustum for a given set of camera parameters */
class Camera(
    val eye : Vector3, // World-space vector from origin to look-from (aka eye point aka "eye")
    val lookAt : Vector3, // World-space vector from origin to look-at point
    val up : Vector3, // World-space vector pointing "up" for viewer
    val nearClip : Float, // Near clip-plane distance from the eye
    val farClip : Float, // Far clip-plane distance from the eye
    val fov : Float, // Field-of-view, in degrees, of the image plane
    val size : IntArray, // The width and height of the image plane in index-space
    val isOrthographic : Boolean
) {
    val aspectRatio : Float // Image's aspect ratio, defined by width / height
    val lookAtDistance : Float // Distance between the eye and the look-at point
    val u : Vector3 // Rightward basis vector of view space
    val v : Vector3 // Downward basis vector of view space
    val n : Vector3 // Forwards basis vector of view space
    val vToW : Matrix4x4 // Matrix that transforms coordinates from view space to world space
    val nearClipDistance : Float // Distance from the eye to the near clip-plane
    val farClipDistance : Float // Distance from the eye to the far clip-plane
    val height : Float // The world-space height of the image
    val width : Float // The world-space width of the image

    init {
        /* Make sure the inputs have correct dimensions */
        assert(size.size == 2) { "The image must have a 2-dimensional size, current value: $size" }

        /* Make sure inputs define a valid view frustum */
        assert(nearClip < farClip) { "Near clip-plane must be closer to eye than far clip-plane, currently nearClip=$nearClip >= $farClip=farClip" }
        assert(0 < fov && fov < 160) { "Field of view is too wide, currently fov=$fov which is not in [0, 160]" }
        assert(size[0] > 0 && size[1] > 0) { "Image dimensions must be positive, current sizes: $size" }

        /* Define additional camera parameters */
        aspectRatio = size[0].toFloat() / size[1].toFloat()
        val nScaled = Vector3.zeros().subtract(lookAt, eye)
        lookAtDistance = nScaled.norm()
        n = nScaled.normalize() // Normalize in-place, now `n` and `nScaled` point to the same object
        u = Vector3.zeros().cross(n, up).normalize()
        v = Vector3.zeros().cross(n, u)
        vToW = Matrix4x4(
            u[0], v[0], n[0], eye[0],
            u[1], v[1], n[1], eye[1],
            u[2], v[2], n[2], eye[2]
        )
        nearClipDistance = nearClip + lookAtDistance
        farClipDistance = farClip + lookAtDistance
        height = 2 * lookAtDistance * tan(fov * PI / 360).toFloat()
        width = aspectRatio * height
    }
}