package com.example.volumerenderer

import android.graphics.Bitmap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

enum class KernelType {
    BOX,
    TENT,
    BSPLINE2,
    BSPLINE3
}

data class RendererUIState(
    val kernel: KernelType = KernelType.BSPLINE3,
    val planeSeparation: Float = 0.01f,
    val renderType: RenderType = RenderType.RGBT,
    val blendMode: BlendMode = BlendMode.OVER,
    val numThreads: Int = 0,
    val useThreads: Boolean = false
)

data class CameraUIState(
    val eye: Vector3 = Vector3(3.8f, 6.9f, 4.1f),
    val lookAt: Vector3 = Vector3(0.0f, -0.067f, -0.11f),
    val up: Vector3 = Vector3(0.25f, 0.40f, 0.88f),
    val nearClip: Float = -1.86f,
    val farClip: Float = 1.805f,
    val fov: Float = 26f,
    val width: Int = 185,
    val height: Int = 190,
    val isOrthographic: Boolean = false
)

enum class VolumeFile {
    CUBE_RL
}

enum class LutFile {
    CUBE_LUT1,
}

enum class LevoyFile {
    CUBE_LEVOY3
}

data class TransferFunctionUIState(
    val unitStep: Float = 0.03f,
    val alphaNearOne: Float = 1f,
    val lutFile: LutFile = LutFile.CUBE_LUT1,
    val levoyFile: LevoyFile = LevoyFile.CUBE_LEVOY3
)

enum class LightingFile {
    LIGHTING1
}

data class LightingUIState(
    val lightingFile: LightingFile = LightingFile.LIGHTING1,
    val kAmbient: Float = 0.2f,
    val kDiffuse: Float = 0.8f,
    val kSpecular: Float = 0.1f,
    val shininess: Float = 150f,
    val depthCueNear: Vector3 = Vector3(1.1f, 1.1f, 1.1f),
    val depthCueFar: Vector3 = Vector3(0.4f, 0.4f, 0.4f)
)

class RendererViewModel : ViewModel() {
    /* Create StateFlows for reading UI state from the application View */
    // StateFlows allow managing data for UI in a way that Jetpack Compose's rendering graph system
    // understands well, which allows Jetpack Compose to optimize rendering around your data
    private val _rendererUIState = MutableStateFlow(RendererUIState())
    val rendererUIState : StateFlow<RendererUIState> = _rendererUIState.asStateFlow()

    private val _cameraUIState = MutableStateFlow(CameraUIState())
    val cameraUIState : StateFlow<CameraUIState> = _cameraUIState.asStateFlow()

    private val _volumeUIState = MutableStateFlow(VolumeFile.CUBE_RL)
    val volumeUIState : StateFlow<VolumeFile> = _volumeUIState.asStateFlow()

    private val _transFunUIState = MutableStateFlow(TransferFunctionUIState())
    val transFunUIState : StateFlow<TransferFunctionUIState> = _transFunUIState.asStateFlow()

    private val _lightingUIState = MutableStateFlow(LightingUIState())
    val lightingUIState : StateFlow<LightingUIState> = _lightingUIState.asStateFlow()

    private val _bitmapUIState = MutableStateFlow<Bitmap?>(null)
    val bitmapUIState : StateFlow<Bitmap?> = _bitmapUIState.asStateFlow()

    /* Render the volume data based on the inputted parameters */
    fun renderVolume(applicationContext: android.content.Context) {
        viewModelScope.launch(Dispatchers.Default) {
            _bitmapUIState.update {
                null
            }
            val rendererUI = _rendererUIState.value
            val cameraUI = _cameraUIState.value
            val volumeUI = _volumeUIState.value
            val transFunUI = _transFunUIState.value
            val lightingUI = _lightingUIState.value

            val camera = Camera(
                eye = cameraUI.eye,
                lookAt = cameraUI.lookAt,
                up = cameraUI.up,
                nearClip = cameraUI.nearClip,
                farClip = cameraUI.farClip,
                fov = cameraUI.fov,
                size = intArrayOf(cameraUI.width, cameraUI.height),
                isOrthographic = cameraUI.isOrthographic
            )
            val ctx = Context(
                volume = Volume(
                    applicationContext = applicationContext,
                    filePath = when(volumeUI) {
                        VolumeFile.CUBE_RL -> "cube-rl.jnrrd"
                    }
                ),
                kernel = when(rendererUI.kernel) {
                    KernelType.BOX -> BoxKernel()
                    KernelType.TENT -> TentKernel()
                    KernelType.BSPLINE2 -> BSpline2Kernel()
                    KernelType.BSPLINE3 -> BSpline3Kernel()
                },
                planeSeparation = rendererUI.planeSeparation,
                renderType = rendererUI.renderType,
                blendMode = rendererUI.blendMode,
                numThreads = rendererUI.numThreads,
                camera = camera,
                transFun = TransferFunction(
                    applicationContext = applicationContext,
                    unitStep = transFunUI.unitStep,
                    alphaNear1 = transFunUI.alphaNearOne,
                    lutPath = when(transFunUI.lutFile) {
                        LutFile.CUBE_LUT1 -> "cube-lut1.jnrrd"
                    },
                    levoyPath = when(transFunUI.levoyFile) {
                        LevoyFile.CUBE_LEVOY3 -> "cube-levoy3.txt"
                    }
                ),
                lighting = Lighting(
                    applicationContext = applicationContext,
                    camera = camera,
                    filePath = when(lightingUI.lightingFile) {
                        LightingFile.LIGHTING1 -> "1.txt"
                    },
                    kAmbient = lightingUI.kAmbient,
                    kDiffuse = lightingUI.kDiffuse,
                    kSpecular = lightingUI.kSpecular,
                    shininess = lightingUI.shininess,
                    depthCueNear = lightingUI.depthCueNear,
                    depthCueFar = lightingUI.depthCueFar
                )
            )
            val renderer = Renderer(ctx)

            // Render the volume in the correct mode
            val bitmap =
                if(ctx.numThreads > 0 && rendererUI.useThreads)
                    renderer.renderWithThreads()
                else if(ctx.numThreads > 0)
                    renderer.renderWithCoroutines()
                else
                    renderer.renderSynchronously()

            // Show the resulting image on screen
            _bitmapUIState.update { bitmap }
        }
    }

    /* Boilerplate code for updating UI state */
    // There are better ways of doing this but this works for now even though it's very ugly/verbose
    private fun updateRenderUIState(
        kernel: KernelType? = null,
        planeSeparation: Float? = null,
        renderType: RenderType? = null,
        blendMode: BlendMode? = null,
        numThreads: Int? = null,
        useThreads: Boolean? = null
    ) = _rendererUIState.update { currentState ->
        RendererUIState(
            kernel = kernel ?: currentState.kernel,
            planeSeparation =planeSeparation ?: currentState.planeSeparation,
            renderType = renderType ?: currentState.renderType,
            blendMode = blendMode ?: currentState.blendMode,
            numThreads = numThreads ?: currentState.numThreads,
            useThreads = useThreads ?: currentState.useThreads
        )
    }
    fun setKernelType(kernelStr: String) = updateRenderUIState(kernel = when(kernelStr) {
        "Box" -> KernelType.BOX
        "Tent" -> KernelType.TENT
        "BSpline2" -> KernelType.BSPLINE2
        "BSpline3" -> KernelType.BSPLINE3
        else -> KernelType.TENT
    })
    fun setPlaneSeparation(newPlaneSep: Float?) = updateRenderUIState(planeSeparation = newPlaneSep)
    fun setRenderType(renderTypeStr: String) = updateRenderUIState(renderType = when(renderTypeStr) {
        "RGBA Mat" -> RenderType.RGBA_MAT
        "RGBA Lit" -> RenderType.RGBA_LIT
        "RGBT" -> RenderType.RGBT
        else -> RenderType.RGBA_MAT
    })
    fun setBlendMode(blendModeStr: String) = updateRenderUIState(blendMode = when(blendModeStr) {
        "Max" -> BlendMode.MAX
        "Sum" -> BlendMode.SUM
        "Mean" -> BlendMode.MEAN
        "Over" -> BlendMode.OVER
        else -> BlendMode.MAX
    })
    fun setNumThreads(newNumThreads: Int?) = updateRenderUIState(numThreads = newNumThreads)
    fun setUseThreads(useThreads: Boolean?) = updateRenderUIState(useThreads = useThreads)

    private fun updateCameraUIState(
        eye: Vector3? = null,
        lookAt: Vector3? = null,
        up: Vector3? = null,
        nearClip: Float? = null,
        farClip: Float? = null,
        fov: Float? = null,
        width: Int? = null,
        height: Int? = null,
        isOrthographic: Boolean? = null
    ) = _cameraUIState.update { currentState ->
        CameraUIState(
            eye = eye ?: currentState.eye,
            lookAt = lookAt ?: currentState.lookAt,
            up = up ?: currentState.up,
            nearClip = nearClip ?: currentState.nearClip,
            farClip = farClip ?: currentState.farClip,
            fov = fov ?: currentState.fov,
            width = width ?: currentState.width,
            height = height ?: currentState.height,
            isOrthographic = isOrthographic ?: currentState.isOrthographic
        )
    }

    fun setEyeX(newX : Float?) = newX?.let { updateCameraUIState(eye = Vector3(newX, _cameraUIState.value.eye[1], _cameraUIState.value.eye[2])) }
    fun setEyeY(newY : Float?) = newY?.let { updateCameraUIState(eye = Vector3(_cameraUIState.value.eye[0], newY, _cameraUIState.value.eye[2])) }
    fun setEyeZ(newZ : Float?) = newZ?.let { updateCameraUIState(eye = Vector3(_cameraUIState.value.eye[0], _cameraUIState.value.eye[1], newZ)) }
    fun setLookAtX(newX : Float?) = newX?.let { updateCameraUIState(lookAt = Vector3(newX, _cameraUIState.value.lookAt[1], _cameraUIState.value.lookAt[2])) }
    fun setLookAtY(newY : Float?) = newY?.let { updateCameraUIState(lookAt = Vector3(_cameraUIState.value.lookAt[0], newY, _cameraUIState.value.lookAt[2])) }
    fun setLookAtZ(newZ : Float?) = newZ?.let { updateCameraUIState(lookAt = Vector3(_cameraUIState.value.lookAt[0], _cameraUIState.value.lookAt[1], newZ)) }
    fun setUpX(newX : Float?) = newX?.let { updateCameraUIState(up = Vector3(newX, _cameraUIState.value.up[1], _cameraUIState.value.up[2])) }
    fun setUpY(newY : Float?) = newY?.let { updateCameraUIState(up = Vector3(_cameraUIState.value.up[0], newY, _cameraUIState.value.up[2])) }
    fun setUpZ(newZ : Float?) = newZ?.let { updateCameraUIState(up = Vector3(_cameraUIState.value.up[0], _cameraUIState.value.up[1], newZ)) }
    fun setNearClip(newNearClip: Float?) = updateCameraUIState(nearClip = newNearClip)
    fun setFarClip(newFarClip: Float?) = updateCameraUIState(farClip = newFarClip)
    fun setFOV(newFOV: Float?) = updateCameraUIState(fov = newFOV)
    fun setWidth(newWidth: Int?) = updateCameraUIState(width = newWidth)
    fun setHeight(newHeight: Int?) = updateCameraUIState(height = newHeight)
    fun setIsOrthographic(newIsOrthographic: Boolean) = updateCameraUIState(isOrthographic = newIsOrthographic)

    fun updateVolume(volumeStr: String) = _volumeUIState.update {
        when(volumeStr) {
            "Cube RL" -> VolumeFile.CUBE_RL
            else -> VolumeFile.CUBE_RL
        }
    }

    private fun updateTransFunUIState(
        unitStep: Float? = null,
        alphaNearOne: Float? = null,
        lutFile: LutFile? = null,
        levoyFile: LevoyFile? = null
    ) = _transFunUIState.update { currentState ->
        TransferFunctionUIState(
            unitStep = unitStep ?: currentState.unitStep,
            alphaNearOne = alphaNearOne ?: currentState.alphaNearOne,
            lutFile = lutFile ?: currentState.lutFile,
            levoyFile = levoyFile ?: currentState.levoyFile
        )
    }
    fun setUnitStep(newUnitStep: Float?) = updateTransFunUIState(unitStep = newUnitStep)
    fun setAlphaNearOne(newAlphaNearOne: Float?) = updateTransFunUIState(alphaNearOne = newAlphaNearOne)
    fun setLutFile(lutFileStr: String) = updateTransFunUIState(lutFile = when(lutFileStr) {
        "Cube Lut 1" -> LutFile.CUBE_LUT1
        else -> LutFile.CUBE_LUT1
    })
    fun setLevoyFile(levoyFileStr: String) = updateTransFunUIState(levoyFile = when(levoyFileStr) {
        "Cube Levoy 3" -> LevoyFile.CUBE_LEVOY3
        else -> LevoyFile.CUBE_LEVOY3
    })

    private fun updateLightingUIState(
        lightingFile: LightingFile? = null,
        kAmbient: Float? = null,
        kDiffuse: Float? = null,
        kSpecular: Float? = null,
        shininess: Float? = null,
        depthCueNear: Vector3? = null,
        depthCueFar: Vector3? = null
    ) = _lightingUIState.update { currentState ->
        LightingUIState(
            lightingFile = lightingFile ?: currentState.lightingFile,
            kAmbient = kAmbient ?: currentState.kAmbient,
            kDiffuse = kDiffuse ?: currentState.kDiffuse,
            kSpecular = kSpecular ?: currentState.kSpecular,
            shininess = shininess ?: currentState.shininess,
            depthCueNear = depthCueNear ?: currentState.depthCueNear,
            depthCueFar = depthCueFar ?: currentState.depthCueFar
        )
    }
    fun setLightingFile(lightingFileStr: String) = updateLightingUIState(lightingFile = when(lightingFileStr) {
        "Lighting 1" -> LightingFile.LIGHTING1
        else -> LightingFile.LIGHTING1
    })
    fun setKAmbient(newKAmbient: Float?) = updateLightingUIState(kAmbient = newKAmbient)
    fun setKDiffuse(newKDiffuse: Float?) = updateLightingUIState(kDiffuse = newKDiffuse)
    fun setKSpecular(newKSpecular: Float?) = updateLightingUIState(kSpecular = newKSpecular)
    fun setShininess(newShininess: Float?) = updateLightingUIState(shininess = newShininess)
    fun setDepthCueNearX(newX : Float?) = newX?.let { updateLightingUIState(depthCueNear = Vector3(newX, _lightingUIState.value.depthCueNear[1], _lightingUIState.value.depthCueNear[2])) }
    fun setDepthCueNearY(newY : Float?) = newY?.let { updateLightingUIState(depthCueNear = Vector3(_lightingUIState.value.depthCueNear[0], newY, _lightingUIState.value.depthCueNear[2])) }
    fun setDepthCueNearZ(newZ : Float?) = newZ?.let { updateLightingUIState(depthCueNear = Vector3(_lightingUIState.value.depthCueNear[0], _lightingUIState.value.depthCueNear[1], newZ)) }
    fun setDepthCueFarX(newX : Float?) = newX?.let { updateLightingUIState(depthCueFar = Vector3(newX, _lightingUIState.value.depthCueFar[1], _lightingUIState.value.depthCueFar[2])) }
    fun setDepthCueFarY(newY : Float?) = newY?.let { updateLightingUIState(depthCueFar = Vector3(_lightingUIState.value.depthCueFar[0], newY, _lightingUIState.value.depthCueFar[2])) }
    fun setDepthCueFarZ(newZ : Float?) = newZ?.let { updateLightingUIState(depthCueFar = Vector3(_lightingUIState.value.depthCueFar[0], _lightingUIState.value.depthCueFar[1], newZ)) }



}