package com.example.volumerenderer

/* Simulate a 2D array with a 1D array */
class FloatArray2(val size: IntArray, val data: FloatArray) {
    /* Make sure the array can be described using the requested dimensions */
    init {
        assert(size.size == 2) { "FloatArray2s must be 2-dimensional! Currently ${size.size}-dimensional." }
        assert(data.size == size[0] * size[1]) { "FloatArray2 data array has incorrect size! data.size=${data.size}, size=(${size[0]}, ${size[1]})"}
    }

    /* Allow use of arr[i, j] to retrieve elements */
    operator fun get(i: Int, j: Int) : Float = data[i * size[1] + j]
    operator fun set(i: Int, j: Int, value: Float) {
        data[i * size[1] + j] = value
    }

    /* Static functions */
    companion object {
        fun zeros(size: IntArray) : FloatArray2 =
            FloatArray2(size, FloatArray(size[0] * size[1]) { 0f })
    }
}

/* Simulate a 3D array with a 1D array */
class FloatArray3(val size: IntArray, val data: FloatArray) {
    init {
        assert(size.size == 3) { "FloatArray3s must be 3-dimensional! Currently ${size.size}-dimensional." }
        assert(data.size == size[0] * size[1] * size[2]) { "FloatArray3 data array has incorrect size! data.size=${data.size}, size=(${size[0]}, ${size[1]}, size[2])"}
    }

    val iterVec : IntArray = IntArray(3)

    /* Allow use of arr[i, j, k] to retrieve elements */
    operator fun get(i: Int, j: Int, k: Int) : Float = data[i * size[1] * size[2] + j * size[2] + k]
    operator fun set(i: Int, j: Int, k: Int, value: Float) {
        data[i * size[1] * size[2] + j * size[2] + k] = value
    }

    inline fun forEachMultiIndexed(action: (IntArray, Float) -> Unit) = data.forEachIndexed { idx, value ->
        iterVec[2] = idx % size[2]
        val idxYZ = idx / size[2]
        iterVec[1] = idxYZ % size[1]
        iterVec[0] = idxYZ / size[1]
        action(iterVec, value)
    }

    /* Static functions */
    companion object {
        fun zeros(size: IntArray) : FloatArray3 =
            FloatArray3(size, FloatArray(size[0] * size[1] * size[2]) { 0f })
    }
}