package com.example.volumerenderer

import kotlin.math.sqrt

@JvmInline // Value class ensures this has no performance overhead compared to regular FloatArrays
value class Vector2(private val nums: FloatArray) {
    /* Make sure the backing array has the correct size */
    init {
        assert(nums.size >= 2) { "Invalid size for Vector2" }
    }

    /* Allocate memory for a FloatArray for this object */
    constructor(x1: Float, x2: Float) : this(floatArrayOf(x1, x2))

    /* Type Cast */
    fun toFloatArray() : FloatArray = nums

    /* Allow using vec[n] syntax for modifying vectors */
    operator fun get(index: Int) : Float = nums[index]
    operator fun set(index: Int, num: Float) {
        nums[index] = num
    }

    /* Modify the array */
    fun set(x1: Float = 0f, x2: Float = 0f) : Vector2 {
        nums[0] = x1
        nums[1] = x2
        return this
    }
    fun setAll(num: Float) : Vector2 = set(num, num)
    fun zero() : Vector2 = setAll(0f) // Sets everything to 0
    fun copy(vec: Vector2) : Vector2 = set(vec[0], vec[1])

    /* Arithmetic */
    fun add(fst: Vector2, snd: Vector2 = this) : Vector2 = set(fst[0] + snd[0], fst[1] + snd[1])
    fun subtract(fst: Vector2, snd: Vector2) : Vector2 = set(fst[0] - snd[0], fst[1] - snd[1])
    fun subtract(vec: Vector2) = subtract(this, vec)
    fun mul(scalar: Float, vec: Vector2 = this) : Vector2 = set(scalar * vec[0], scalar * vec[1])
    fun mul(fst: Vector2, snd: Vector2 = this) : Vector2 = set(fst[0] * snd[0], fst[1] * snd[1])
    fun div(scalar: Float, vec: Vector2 = this) : Vector2 = mul(1 / scalar, vec)

    /* Linear Algebra Operations */
    fun norm() : Float = sqrt(nums[0] * nums[0] + nums[1] * nums[1])
    fun normalize(vec: Vector2 = this) : Vector2 = this.div(vec.norm(), vec)

    /* Static functions */
    companion object {
        fun zeros() : Vector2 {
            return Vector2(floatArrayOf(0f, 0f))
        }
    }
}

fun det2(x1: Float, x2: Float, x3: Float, x4: Float) : Float = x1 * x4 - x2 * x3

@JvmInline
value class Matrix3x3(private val nums : FloatArray) {
    /* Make sure the backing array has the correct size */
    init {
        assert(nums.size >= 9) { "Invalid size for Matrix3x3" }
    }

    /* Allocate memory for a FloatArray for this object */
    constructor(x1: Float, x2: Float, x3: Float, x4: Float, x5: Float, x6: Float, x7: Float, x8: Float, x9: Float) :
        this(floatArrayOf(x1, x2, x3, x4, x5, x6, x7, x8, x9))

    /* Type cast */
    fun toFloatArray() : FloatArray = nums

    /* Allow using vec[n] or vec[i, j] syntax for modifying matrices */
    operator fun get(index: Int) : Float = nums[index]
    operator fun get(row: Int, col: Int) : Float = nums[row * 4 + col]
    operator fun set(index: Int, num: Float) {
        nums[index] = num
    }
    operator fun set(row: Int, col: Int, num: Float) {
        nums[row * 4 + col] = num
    }

    /* Modify the array */
    fun set(x1: Float, x2: Float, x3: Float, x4: Float, x5: Float, x6: Float, x7: Float, x8: Float, x9: Float) : Matrix3x3 {
        nums[0] = x1
        nums[1] = x2
        nums[2] = x3
        nums[3] = x4
        nums[4] = x5
        nums[5] = x6
        nums[6] = x7
        nums[7] = x8
        nums[8] = x9
        return this
    }
    fun setCol(col: Int, x1: Float, x2: Float, x3: Float) : Matrix3x3 {
        this[0, col] = x1
        this[1, col] = x2
        this[2, col] = x3
        return this
    }
    fun setRow(row: Int, x1: Float, x2: Float, x3: Float) : Matrix3x3 {
        this[row, 0] = x1
        this[row, 1] = x2
        this[row, 2] = x3
        return this
    }

    /* Matrix Transpose */
    fun transpose(source: Matrix3x3 = this) : Matrix3x3 = set(
        source[0], source[3], source[6],
        source[1], source[4], source[7],
        source[2], source[5], source[8]
    )

    /* Matrix Multiplication */
    fun mul(mat1: Matrix3x3, mat2: Matrix3x3) : Matrix3x3 = set(
        mat1[0]*mat2[0] + mat1[1]*mat2[3] + mat1[2]*mat2[6],
        mat1[0]*mat2[1] + mat1[1]*mat2[4] + mat1[2]*mat2[7],
        mat1[0]*mat2[2] + mat1[1]*mat2[5] + mat1[2]*mat2[8],

        mat1[3]*mat2[0] + mat1[4]*mat2[3] + mat1[5]*mat2[6],
        mat1[3]*mat2[1] + mat1[4]*mat2[4] + mat1[5]*mat2[7],
        mat1[3]*mat2[2] + mat1[4]*mat2[5] + mat1[5]*mat2[8],

        mat1[6]*mat2[0] + mat1[7]*mat2[3] + mat1[8]*mat2[6],
        mat1[6]*mat2[1] + mat1[7]*mat2[4] + mat1[8]*mat2[7],
        mat1[6]*mat2[2] + mat1[7]*mat2[5] + mat1[8]*mat2[8]
    )

    /* Matrix Determinant */
    private fun determinant(x0: Float, x1: Float, x2: Float, x3: Float, x4: Float, x5: Float, x6: Float, x7: Float, x8: Float) : Float =
        x0 * x4 * x8 +
        x3 * x7 * x2 +
        x6 * x1 * x5 -
        x6 * x4 * x2 -
        x3 * x1 * x8 -
        x0 * x7 * x5
    fun determinant() : Float =
        determinant(this[0], this[1], this[2], this[3], this[4], this[5], this[6], this[7], this[8])

    /* Matrix Inverse */
    fun inverse(source: Matrix3x3 = this) : Matrix3x3 {
        val det = this.determinant()
        return set(
             det2(source[4],source[5],source[7],source[8])/det,
            -det2(source[1],source[2],source[7],source[8])/det,
             det2(source[1],source[2],source[4],source[5])/det,
            -det2(source[3],source[5],source[6],source[8])/det,
             det2(source[0],source[2],source[6],source[8])/det,
            -det2(source[0],source[2],source[3],source[5])/det,
             det2(source[3],source[4],source[6],source[7])/det,
            -det2(source[0],source[1],source[6],source[7])/det,
             det2(source[0],source[1],source[3],source[4])/det
        )
    }

    /* Get the upper 3x3 matrix of a 4x4 matrix*/
    fun upper3x3(source: Matrix4x4) : Matrix3x3 =
        set(source[0], source[1], source[2], source[4], source[5], source[6], source[8], source[9], source[10])

    companion object {
        fun zeros() : Matrix3x3 {
            return Matrix3x3(FloatArray(9) { 0f })
        }
    }
}

@JvmInline // Value class ensures this has no performance overhead compared to regular FloatArrays
value class Vector3(private val nums: FloatArray) {
    /* Make sure the backing array has the correct size */
    init {
        assert(nums.size >= 3) { "Invalid size for Vector3" }
    }

    /* Allocate memory for a FloatArray for this object */
    constructor(x1: Float, x2: Float, x3: Float) : this(floatArrayOf(x1, x2, x3))

    /* Cast to a Vector2 */
    fun toVector2() : Vector2 = Vector2(nums)
    fun toFloatArray() : FloatArray = nums

    /* Allow using vec[n] syntax for modifying vectors */
    operator fun get(index: Int) : Float = nums[index]
    operator fun set(index: Int, num: Float) {
        nums[index] = num
    }

    /* Modify the array */
    fun set(x1: Float = 0f, x2: Float = 0f, x3: Float = 0f) : Vector3 {
        nums[0] = x1
        nums[1] = x2
        nums[2] = x3
        return this
    }
    fun setAll(num: Float) : Vector3 = set(num, num, num)
    fun zero() : Vector3 = setAll(0f) // Sets everything to 0
    fun copy(vec: Vector3) : Vector3 = set(vec[0], vec[1], vec[2])
    fun copy(vec: Vector4) : Vector3 = set(vec[0], vec[1], vec[2])

    /* Check a condition on all elements of the vector */
    fun all(condition : (Float) -> Boolean) = nums.all(condition)
    fun isZero() : Boolean = (nums[0] == 0f && nums[1] == 0f && nums[2] == 0f)

    /* Arithmetic */
    fun add(fst: Vector3, snd: Vector3 = this) : Vector3 =
        set(fst[0] + snd[0], fst[1] + snd[1], fst[2] + snd[2])
    fun subtract(fst: Vector3, snd: Vector3) : Vector3 = 
        set(fst[0] - snd[0], fst[1] - snd[1], fst[2] - snd[2])
    fun subtract(vec: Vector3) = subtract(this, vec)
    fun mul(scalar: Float, vec: Vector3 = this) : Vector3 =
        set(scalar * vec[0], scalar * vec[1], scalar * vec[2])
    fun mul(fst: Vector3, snd: Vector3 = this) : Vector3 =
        set(fst[0] * snd[0], fst[1] * snd[1], fst[2] * snd[2])
    fun div(scalar: Float, vec: Vector3 = this) : Vector3 = mul(1 / scalar, vec)
    fun scaleAdd(scalar1: Float, vec1: Vector3, scalar2: Float = 1f, vec2: Vector3 = this) =
        set(
            scalar1 * vec1[0] + scalar2 * vec2[0],
            scalar1 * vec1[1] + scalar2 * vec2[1],
            scalar1 * vec1[2] + scalar2 * vec2[2]
        )

    /* Linear Interpolation */
    fun lerp(vec1: Vector3, vec2: Vector3, t: Float) = scaleAdd(1 - t, vec1, t, vec2)
    fun lerp(vec1: Vector3, vec2: Vector3, x1: Float, x: Float, x2: Float) = lerp(vec1, vec2, unlerp(x1, x, x2))

    /* Linear Algebra Operations */
    fun norm() : Float = sqrt(nums[0] * nums[0] + nums[1] * nums[1] + nums[2] * nums[2])
    fun normalize(vec: Vector3 = this) : Vector3 = div(vec.norm(), vec)
    infix fun dot(vec: Vector3) : Float = this[0] * vec[0] + this[1] * vec[1] + this[2] * vec[2]
    fun cross(vec1: Vector3, vec2: Vector3) : Vector3 = set(
        vec1[1] * vec2[2] - vec1[2] * vec2[1],
        vec1[2] * vec2[0] - vec1[0] * vec2[2],
        vec1[0] * vec2[1] - vec1[1] * vec2[0]
    )

    /* Matrix-Vector Multiplication */
    fun mul(mat: Matrix3x3, vec: Vector3 = this) = set(
        mat[0]*vec[0] + mat[1]*vec[1] + mat[2]*vec[2],
        mat[3]*vec[0] + mat[4]*vec[1] + mat[5]*vec[2],
        mat[6]*vec[0] + mat[7]*vec[1] + mat[8]*vec[2]
    )

    /* Static functions */
    companion object {
        fun zeros() : Vector3 {
            return Vector3(floatArrayOf(0f, 0f, 0f))
        }
    }
}

@JvmInline
value class Matrix4x4(private val nums : FloatArray) {
    /* Make sure the backing array has the correct size */
    init {
        assert(nums.size >= 16) { "Invalid size for Matrix4x4" }
    }

    /* Allocate memory for a FloatArray for this object */
    constructor(x1: Float, x2: Float, x3: Float, x4: Float, x5: Float, x6: Float, x7: Float, x8: Float,
                x9: Float, x10: Float, x11: Float, x12: Float, x13: Float = 0f, x14: Float = 0f, x15: Float = 0f, x16: Float = 1f) :
            this(floatArrayOf(x1, x2, x3, x4, x5, x6, x7, x8, x9, x10, x11, x12, x13, x14, x15, x16))

    /* Type Cast */
    fun toFloatArray() : FloatArray = nums

    /* Allow using vec[n] or vec[i, j] syntax for modifying matrices */
    operator fun get(index: Int) : Float = nums[index]
    operator fun get(row: Int, col: Int) : Float = nums[row * 4 + col]
    operator fun set(index: Int, num: Float) {
        nums[index] = num
    }
    operator fun set(row: Int, col: Int, num: Float) {
        nums[row * 4 + col] = num
    }

    /* Modify the array */
    fun set(x1: Float, x2: Float, x3: Float, x4: Float, x5: Float, x6: Float, x7: Float, x8: Float,
            x9: Float, x10: Float, x11: Float, x12: Float, x13: Float = 0f, x14: Float = 0f, x15: Float = 0f, x16: Float = 1f) : Matrix4x4 {
        nums[0] = x1
        nums[1] = x2
        nums[2] = x3
        nums[3] = x4
        nums[4] = x5
        nums[5] = x6
        nums[6] = x7
        nums[7] = x8
        nums[8] = x9
        nums[9] = x10
        nums[10] = x11
        nums[11] = x12
        nums[12] = x13
        nums[13] = x14
        nums[14] = x15
        nums[15] = x16
        return this
    }

    fun setCol(col: Int, x1: Float, x2: Float, x3: Float, x4: Float) : Matrix4x4 {
        this[0, col] = x1
        this[1, col] = x2
        this[2, col] = x3
        this[3, col] = x4
        return this
    }
    fun setRow(row: Int, x1: Float, x2: Float, x3: Float, x4: Float) : Matrix4x4 {
        this[row, 0] = x1
        this[row, 1] = x2
        this[row, 2] = x3
        this[row, 3] = x4
        return this
    }

    /* Matrix Transpose */
    fun transpose(source: Matrix4x4) : Matrix4x4 = set(
        nums[0], nums[4], nums[8], nums[12],
        nums[1], nums[5], nums[9], nums[13],
        nums[2], nums[6], nums[10], nums[14],
        nums[3], nums[7], nums[11], nums[15]
    )

    /* Matrix Inverse */
    // Note: Only works for *affine* transformations!
    fun inverse(source: Matrix4x4 = this) : Matrix4x4 {
        val inverse3 = Matrix3x3.zeros().upper3x3(source).inverse()
        val inversePos = Vector3(-source[3], -source[7], -source[11]).mul(inverse3)
        return set(
            inverse3[0], inverse3[1], inverse3[2], inversePos[0],
            inverse3[3], inverse3[4], inverse3[5], inversePos[1],
            inverse3[6], inverse3[7], inverse3[8], inversePos[2],
        )
    }

    /* Matrix Multiplication */
    fun mul(mat1: Matrix4x4, mat2: Matrix4x4) : Matrix4x4 = set(
        mat1[ 0]*mat2[ 0]+mat1[ 1]*mat2[ 4]+mat1[ 2]*mat2[ 8]+mat1[ 3]*mat2[12],
        mat1[ 0]*mat2[ 1]+mat1[ 1]*mat2[ 5]+mat1[ 2]*mat2[ 9]+mat1[ 3]*mat2[13],
        mat1[ 0]*mat2[ 2]+mat1[ 1]*mat2[ 6]+mat1[ 2]*mat2[10]+mat1[ 3]*mat2[14],
        mat1[ 0]*mat2[ 3]+mat1[ 1]*mat2[ 7]+mat1[ 2]*mat2[11]+mat1[ 3]*mat2[15],

        mat1[ 4]*mat2[ 0]+mat1[ 5]*mat2[ 4]+mat1[ 6]*mat2[ 8]+mat1[ 7]*mat2[12],
        mat1[ 4]*mat2[ 1]+mat1[ 5]*mat2[ 5]+mat1[ 6]*mat2[ 9]+mat1[ 7]*mat2[13],
        mat1[ 4]*mat2[ 2]+mat1[ 5]*mat2[ 6]+mat1[ 6]*mat2[10]+mat1[ 7]*mat2[14],
        mat1[ 4]*mat2[ 3]+mat1[ 5]*mat2[ 7]+mat1[ 6]*mat2[11]+mat1[ 7]*mat2[15],

        mat1[ 8]*mat2[ 0]+mat1[ 9]*mat2[ 4]+mat1[10]*mat2[ 8]+mat1[11]*mat2[12],
        mat1[ 8]*mat2[ 1]+mat1[ 9]*mat2[ 5]+mat1[10]*mat2[ 9]+mat1[11]*mat2[13],
        mat1[ 8]*mat2[ 2]+mat1[ 9]*mat2[ 6]+mat1[10]*mat2[10]+mat1[11]*mat2[14],
        mat1[ 8]*mat2[ 3]+mat1[ 9]*mat2[ 7]+mat1[10]*mat2[11]+mat1[11]*mat2[15],

        mat1[12]*mat2[ 0]+mat1[13]*mat2[ 4]+mat1[14]*mat2[ 8]+mat1[15]*mat2[12],
        mat1[12]*mat2[ 1]+mat1[13]*mat2[ 5]+mat1[14]*mat2[ 9]+mat1[15]*mat2[13],
        mat1[12]*mat2[ 2]+mat1[13]*mat2[ 6]+mat1[14]*mat2[10]+mat1[15]*mat2[14],
        mat1[12]*mat2[ 3]+mat1[13]*mat2[ 7]+mat1[14]*mat2[11]+mat1[15]*mat2[15]
    )

    companion object {
        fun zeros() : Matrix4x4 {
            return Matrix4x4(FloatArray(16) { 0f })
        }
    }
}

@JvmInline // Value class ensures this has no performance overhead compared to regular FloatArrays
value class Vector4(private val nums: FloatArray) {
    /* Make sure the backing array has the correct size */
    init {
        assert(nums.size >= 4) { "Invalid size for Vector4" }
    }

    /* Allocate memory for a FloatArray for this object */
    constructor(x1: Float, x2: Float, x3: Float, x4: Float) : this(floatArrayOf(x1, x2, x3, x4))

    /* Cast to a smaller vector types */
    fun toVector2() : Vector2 = Vector2(nums)
    fun toVector3() : Vector3 = Vector3(nums)
    fun toFloatArray() : FloatArray = nums

    /* Allow using vec[n] syntax for modifying vectors */
    operator fun get(index: Int) : Float = nums[index]
    operator fun set(index: Int, num: Float) {
        nums[index] = num
    }

    /* Modify the array */
    fun set(x1: Float = 0f, x2: Float = 0f, x3: Float = 0f, x4: Float = 0f) : Vector4 {
        nums[0] = x1
        nums[1] = x2
        nums[2] = x3
        nums[3] = x4
        return this
    }
    fun setAll(num: Float) : Vector4 = set(num, num, num, num)
    fun zero() : Vector4 = setAll(0f) // Sets everything to 0
    fun copy(vec: Vector4) : Vector4 = set(vec[0], vec[1], vec[2], vec[3])
    fun copy(vec: Vector3, lastNum: Float = 0f) : Vector4 = set(vec[0], vec[1], vec[2], lastNum)

    /* Check a condition on all elements of the vector */
    fun all(condition : (Float) -> Boolean) = nums.all(condition)
    fun isZero() : Boolean = (nums[0] == 0f && nums[1] == 0f && nums[2] == 0f)

    /* Arithmetic */
    fun add(fst: Vector4, snd: Vector4 = this) : Vector4 =
        set(fst[0] + snd[0], fst[1] + snd[1], fst[2] + snd[2], fst[3] + snd[3])
    fun subtract(fst: Vector4, snd: Vector4 = this) : Vector4 =
        set(fst[0] - snd[0], fst[1] - snd[1], fst[2] - snd[2], fst[3] - snd[3])
    fun subtract(vec: Vector4) = subtract(this, vec)
    fun mul(scalar: Float, vec: Vector4 = this) : Vector4 =
        set(scalar * vec[0], scalar * vec[1], scalar * vec[2], scalar * vec[3])
    fun mul(fst: Vector4, snd: Vector4 = this) : Vector4 =
        set(fst[0] * snd[0], fst[1] * snd[1], fst[2] * snd[2], fst[3] * snd[3])
    fun div(scalar: Float, vec: Vector4 = this) : Vector4 = mul(1 / scalar, vec)

    /* Linear Algebra Operations */
    fun norm() : Float = sqrt(nums[0] * nums[0] + nums[1] * nums[1] + nums[2] * nums[2] + nums[3] * nums[3])
    fun normalize(vec: Vector4 = this) : Vector4 = this.div(vec.norm(), vec)

    /* Matrix-Vector Multiplication */
    fun mul(mat: Matrix4x4, vec: Vector4 = this) : Vector4 = set(
        mat[ 0]*vec[0] + mat[ 1]*vec[1] + mat[ 2]*vec[2] + mat[ 3]*vec[3],
        mat[ 4]*vec[0] + mat[ 5]*vec[1] + mat[ 6]*vec[2] + mat[ 7]*vec[3],
        mat[ 8]*vec[0] + mat[ 9]*vec[1] + mat[10]*vec[2] + mat[11]*vec[3],
        mat[12]*vec[0] + mat[13]*vec[1] + mat[14]*vec[2] + mat[15]*vec[3]
    )

    /* Static functions */
    companion object {
        fun zeros() : Vector4 {
            return Vector4(floatArrayOf(0f, 0f, 0f, 0f))
        }
    }
}

