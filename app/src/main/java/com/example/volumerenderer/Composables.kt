package com.example.volumerenderer

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.createBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.volumerenderer.ui.theme.VolumeRendererTheme

@Composable
fun ComposeRoot(viewModel: RendererViewModel, applicationContext: android.content.Context?) {
    VolumeRendererTheme {
        // Track the bitmap to update the image onscreen whenever it changes
        val bitmap by viewModel.bitmapUIState.collectAsStateWithLifecycle()
        DrawScreen(bitmap, viewModel, applicationContext)
    }
}

@Composable
fun OldComposeRoot(bitmap: Bitmap) {
    VolumeRendererTheme {
        DrawScreen(bitmap)
    }
}

@Composable
fun DrawScreen(bitmap: Bitmap? = null, viewModel: RendererViewModel? = null, applicationContext: android.content.Context? = null) {
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally){
            Text(
                "Volume Renderer",
                fontSize = 40.sp
            )
            Spacer(
                modifier = Modifier.height(5.dp)
            )
            Settings(viewModel)
            val loading = remember { mutableStateOf(false) }
            RenderButton(viewModel, applicationContext, loading)
            if(bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    modifier = Modifier.fillMaxSize(),
                    contentDescription = "Render output"
                )
            }
            else if (loading.value) {
                Text("Rendering volume...")
            }

        }
    }
}

@Composable
fun RendererSettings(viewModel: RendererViewModel? = null) {
    Column(
        modifier = Modifier
            .padding(16.dp)
    ) {
        Text(
            text= "Renderer Settings",
            fontSize = 20.sp,
        )
        Spacer(
            modifier = Modifier.height(5.dp)
        )
        Row {
            var planeSepText by remember { mutableStateOf("0.01") }
            var numThreadsText by remember { mutableStateOf("0") }
            TextField(
                value = planeSepText,
                onValueChange = {
                    planeSepText = it
                    viewModel?.setPlaneSeparation(planeSepText.toFloatOrNull())
                },
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .padding(end=5.dp),
                label = {
                    Text("Plane Separation")
                },
                maxLines = 1,
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number)
            )
            TextField(
                value = numThreadsText,
                onValueChange = {
                    numThreadsText = it
                    viewModel?.setNumThreads(numThreadsText.toIntOrNull())
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start=5.dp),
                label = {
                    Text("# Threads")
                },
                maxLines = 1,
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
            )
        }
        var kernelText by remember { mutableStateOf("Tent") }
        var kernelDropdownExpanded by remember { mutableStateOf(false) }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { kernelDropdownExpanded = !kernelDropdownExpanded },
        ) {

            OutlinedTextField(
                value = kernelText,
                onValueChange = { kernelText = it },
                enabled = false,
                label = {
                    Text("Convolution Kernel")
                },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    Icon(Icons.Default.MoreVert,"Select Convolution Kernel")
                },
                colors = OutlinedTextFieldDefaults.colors().copy(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledIndicatorColor = MaterialTheme.colorScheme.outline,
                    disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant)
            )
            DropdownMenu(
                expanded = kernelDropdownExpanded,
                onDismissRequest = { kernelDropdownExpanded = false }
            ) {
                val boxText = "Box"
                val tentText = "Tent"
                val bspline2Text = "BSpline2"
                val bspline3Text = "BSpline3"
                DropdownMenuItem(
                    text = { Text(boxText) },
                    onClick = {
                        kernelText = boxText
                        viewModel?.setKernelType(boxText)
                    }
                )
                DropdownMenuItem(
                    text = { Text(tentText) },
                    onClick = {
                        kernelText = tentText
                        viewModel?.setKernelType(tentText)
                    }
                )
                DropdownMenuItem(
                    text = { Text(bspline2Text) },
                    onClick = {
                        kernelText = bspline2Text
                        viewModel?.setKernelType(bspline2Text)
                    }
                )
                DropdownMenuItem(
                    text = { Text(bspline3Text) },
                    onClick = {
                        kernelText = bspline3Text
                        viewModel?.setKernelType(bspline3Text)
                    }
                )
            }
        }
        Row {
            var renderTypeText by remember { mutableStateOf("RGBT") }
            var renderTypeDropdownExpanded by remember { mutableStateOf(false) }
            Box(
                modifier = Modifier
                    .padding(end=5.dp)
                    .fillMaxWidth(0.5f)
                    .clickable { renderTypeDropdownExpanded = !renderTypeDropdownExpanded },
            ) {
                OutlinedTextField(
                    value = renderTypeText,
                    onValueChange = { renderTypeText = it },
                    enabled = false,
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text("Render Type")
                    },
                    trailingIcon = {
                        Icon(Icons.Default.MoreVert,"Select Render Type")
                    },
                    colors = OutlinedTextFieldDefaults.colors().copy(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledIndicatorColor = MaterialTheme.colorScheme.outline,
                        disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant)
                )
                DropdownMenu(
                    expanded = renderTypeDropdownExpanded,
                    onDismissRequest = { renderTypeDropdownExpanded = false }
                ) {
                    val rgbaMatText = "RGBA Mat"
                    val rgbaLitText = "RGBA Lit"
                    val rgbtText = "RGBT"
                    DropdownMenuItem(
                        text = { Text(rgbaMatText) },
                        onClick = {
                            renderTypeText = rgbaMatText
                            viewModel?.setRenderType(rgbaMatText)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(rgbaLitText) },
                        onClick = {
                            renderTypeText = rgbaLitText
                            viewModel?.setRenderType(rgbaLitText)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(rgbtText) },
                        onClick = {
                            renderTypeText = rgbtText
                            viewModel?.setRenderType(rgbtText)
                        }
                    )
                }
            }
            var blendModeText by remember { mutableStateOf("Over") }
            var blendModeDropdownExpanded by remember { mutableStateOf(false) }
            Box(modifier = Modifier
                .padding(start=5.dp)
                .fillMaxWidth()
                .clickable { blendModeDropdownExpanded = !blendModeDropdownExpanded }) {
                OutlinedTextField(
                    value = blendModeText,
                    onValueChange = { blendModeText = it },
                    enabled = false,
                    label = {
                        Text("Blend Mode")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        Icon(Icons.Default.MoreVert,"Select Blend Mode")
                    },
                    colors = OutlinedTextFieldDefaults.colors().copy(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledIndicatorColor = MaterialTheme.colorScheme.outline,
                        disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant)
                )
                DropdownMenu(
                    expanded = blendModeDropdownExpanded,
                    onDismissRequest = { blendModeDropdownExpanded = false }
                ) {
                    val maxText = "Max"
                    val sumText = "Sum"
                    val meanText = "Mean"
                    val overText = "Over"
                    DropdownMenuItem(
                        text = { Text(maxText) },
                        onClick = {
                            blendModeText = maxText
                            viewModel?.setBlendMode(maxText)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(sumText) },
                        onClick = {
                            blendModeText = sumText
                            viewModel?.setBlendMode(sumText)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(meanText) },
                        onClick = {
                            blendModeText = meanText
                            viewModel?.setBlendMode(meanText)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(overText) },
                        onClick = {
                            blendModeText = overText
                            viewModel?.setBlendMode(overText)
                        }
                    )
                }
            }
        }
        var useThreadsText by remember { mutableStateOf("Coroutines") }
        var useThreadsExpanded by remember { mutableStateOf(false) }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { useThreadsExpanded = !useThreadsExpanded },
        ) {

            OutlinedTextField(
                value = useThreadsText,
                onValueChange = { useThreadsText = it },
                enabled = false,
                label = {
                    Text("Parallelism Mode")
                },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    Icon(Icons.Default.MoreVert,"Select Convolution Kernel")
                },
                colors = OutlinedTextFieldDefaults.colors().copy(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledIndicatorColor = MaterialTheme.colorScheme.outline,
                    disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant)
            )
            DropdownMenu(
                expanded = useThreadsExpanded,
                onDismissRequest = { useThreadsExpanded = false }
            ) {
                val coroutinesText = "Coroutines"
                val threadsText = "Threads"
                DropdownMenuItem(
                    text = { Text(coroutinesText) },
                    onClick = {
                        useThreadsText = coroutinesText
                        viewModel?.setUseThreads(false)
                    }
                )
                DropdownMenuItem(
                    text = { Text(threadsText) },
                    onClick = {
                        useThreadsText = threadsText
                        viewModel?.setUseThreads(true)
                    }
                )
            }
        }
    }
}

@Composable
fun CameraSettings(viewModel: RendererViewModel? = null) {
    Column(
        Modifier.padding(16.dp)
    ) {
        Text(
            text= "Camera Settings",
            fontSize = 20.sp,
        )
        Spacer(
            modifier = Modifier.height(5.dp)
        )
        Row {
            var eyeXText by remember { mutableStateOf("3.8") }
            var eyeYText by remember { mutableStateOf("6.9") }
            var eyeZText by remember { mutableStateOf("4.1")}
            TextField(
                value = eyeXText,
                onValueChange = {
                    eyeXText = it
                    viewModel?.setEyeX(eyeXText.toFloatOrNull())
                },
                modifier = Modifier
                    .fillMaxWidth(0.33f)
                    .padding(end=5.dp),
                label = {
                    Text("Eye X")
                },
                maxLines = 1,
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number)
            )
            TextField(
                value = eyeYText,
                onValueChange = {
                    eyeYText = it
                    viewModel?.setEyeY(eyeYText.toFloatOrNull())
                },
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .padding(start=5.dp, end=5.dp),
                label = {
                    Text("Eye Y")
                },
                maxLines = 1,
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
            )
            TextField(
                value = eyeZText,
                onValueChange = {
                    eyeZText = it
                    viewModel?.setEyeZ(eyeZText.toFloatOrNull())
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start=5.dp),
                label = {
                    Text("Eye Z")
                },
                maxLines = 1,
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
            )
        }
        Spacer(
            modifier = Modifier.height(10.dp)
        )
        Row {
            var lookAtXText by remember { mutableStateOf("0.0") }
            var lookAtYText by remember { mutableStateOf("-0.067") }
            var lookAtZText by remember { mutableStateOf("-0.11")}
            TextField(
                value = lookAtXText,
                onValueChange = {
                    lookAtXText = it
                    viewModel?.setLookAtX(lookAtXText.toFloatOrNull())
                },
                modifier = Modifier
                    .fillMaxWidth(0.33f)
                    .padding(end=5.dp),
                label = {
                    Text("Look At X")
                },
                maxLines = 1,
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number)
            )
            TextField(
                value = lookAtYText,
                onValueChange = {
                    lookAtYText = it
                    viewModel?.setLookAtY(lookAtYText.toFloatOrNull())
                },
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .padding(start=5.dp, end=5.dp),
                label = {
                    Text("Look At Y")
                },
                maxLines = 1,
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
            )
            TextField(
                value = lookAtZText,
                onValueChange = {
                    lookAtZText = it
                    viewModel?.setLookAtZ(lookAtZText.toFloatOrNull())
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start=5.dp),
                label = {
                    Text("Look At Z")
                },
                maxLines = 1,
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
            )
        }
        Spacer(
            modifier = Modifier.height(10.dp)
        )
        Row {
            var upXText by remember { mutableStateOf("0.25") }
            var upYText by remember { mutableStateOf("0.40") }
            var upZText by remember { mutableStateOf("0.88")}
            TextField(
                value = upXText,
                onValueChange = {
                    upXText = it
                    viewModel?.setUpX(upXText.toFloatOrNull())
                },
                modifier = Modifier
                    .fillMaxWidth(0.33f)
                    .padding(end=5.dp),
                label = {
                    Text("Up X")
                },
                maxLines = 1,
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number)
            )
            TextField(
                value = upYText,
                onValueChange = {
                    upYText = it
                    viewModel?.setUpY(upYText.toFloatOrNull())
                },
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .padding(start=5.dp, end=5.dp),
                label = {
                    Text("Up Y")
                },
                maxLines = 1,
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
            )
            TextField(
                value = upZText,
                onValueChange = {
                    upZText = it
                    viewModel?.setUpZ(upZText.toFloatOrNull())
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start=5.dp),
                label = {
                    Text("Up Z")
                },
                maxLines = 1,
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
            )
        }
        Spacer(
            modifier = Modifier.height(10.dp)
        )
        Row {
            var nearClipText by remember { mutableStateOf("-1.86") }
            var farClipText by remember { mutableStateOf("1.805") }
            TextField(
                value = nearClipText,
                onValueChange = {
                    nearClipText = it
                    viewModel?.setNearClip(nearClipText.toFloatOrNull())
                },
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .padding(end=5.dp),
                label = {
                    Text("Near Clip")
                },
                maxLines = 1,
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number)
            )
            TextField(
                value = farClipText,
                onValueChange = {
                    farClipText = it
                    viewModel?.setFarClip(farClipText.toFloatOrNull())
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start=5.dp),
                label = {
                    Text("Far Clip")
                },
                maxLines = 1,
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
            )
        }
        Spacer(
            modifier = Modifier.height(2.dp)
        )
        Row {
            var fovText by remember { mutableStateOf("-1.86") }
            TextField(
                value = fovText,
                onValueChange = {
                    fovText = it
                    viewModel?.setFOV(fovText.toFloatOrNull())
                },
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .padding(end=5.dp, top=8.dp),
                label = {
                    Text("Field of View")
                },
                maxLines = 1,
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number)
            )
            var projectionTypeText by remember { mutableStateOf("Perspective") }
            var projectionTypeDropdownExpanded by remember { mutableStateOf(false) }
            Box(modifier = Modifier
                .padding(start=5.dp)
                .fillMaxWidth()
                .clickable { projectionTypeDropdownExpanded = !projectionTypeDropdownExpanded }) {
                OutlinedTextField(
                    value = projectionTypeText,
                    onValueChange = { projectionTypeText = it },
                    enabled = false,
                    label = {
                        Text("Projection Type")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        Icon(Icons.Default.MoreVert,"Select Blend Mode")
                    },
                    colors = OutlinedTextFieldDefaults.colors().copy(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledIndicatorColor = MaterialTheme.colorScheme.outline,
                        disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant)
                )
                DropdownMenu(
                    expanded = projectionTypeDropdownExpanded,
                    onDismissRequest = { projectionTypeDropdownExpanded = false }
                ) {
                    val orthographicText = "Orthographic"
                    val perspectiveText = "Perspective"
                    DropdownMenuItem(
                        text = { Text(orthographicText) },
                        onClick = {
                            projectionTypeText = orthographicText
                            viewModel?.setIsOrthographic(true)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(perspectiveText) },
                        onClick = {
                            projectionTypeText = perspectiveText
                            viewModel?.setIsOrthographic(false)
                        }
                    )
                }
            }
        }
        Spacer(
            modifier = Modifier.height(10.dp)
        )
        Row {
            var widthText by remember { mutableStateOf("185") }
            var heightText by remember { mutableStateOf("190") }
            TextField(
                value = widthText,
                onValueChange = {
                    widthText = it
                    viewModel?.setWidth(widthText.toIntOrNull())
                },
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .padding(end=5.dp),
                label = {
                    Text("Width")
                },
                maxLines = 1,
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number)
            )
            TextField(
                value = heightText,
                onValueChange = {
                    heightText = it
                    viewModel?.setHeight(heightText.toIntOrNull())
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start=5.dp),
                label = {
                    Text("Height")
                },
                maxLines = 1,
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
            )
        }
    }
}

@Composable
fun VolumeSettings(viewModel: RendererViewModel? = null) {
    Column(
        Modifier.padding(16.dp)
    ) {
        Text(
            text = "Volume Settings",
            fontSize = 20.sp
        )
        Spacer(
            modifier = Modifier.height(5.dp)
        )
        var volumeFileText by remember { mutableStateOf("Cube RL") }
        var volumeDropdownExpanded by remember { mutableStateOf(false) }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { volumeDropdownExpanded = !volumeDropdownExpanded },
        ) {

            OutlinedTextField(
                value = volumeFileText,
                onValueChange = { volumeFileText = it },
                enabled = false,
                label = {
                    Text("Volume File")
                },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    Icon(Icons.Default.MoreVert, "Select Volume File")
                },
                colors = OutlinedTextFieldDefaults.colors().copy(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledIndicatorColor = MaterialTheme.colorScheme.outline,
                    disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            DropdownMenu(
                expanded = volumeDropdownExpanded,
                onDismissRequest = { volumeDropdownExpanded = false }
            ) {
                val cubeRLText = "Cube RL"
                DropdownMenuItem(
                    text = { Text(cubeRLText) },
                    onClick = {
                        volumeFileText = cubeRLText
                        viewModel?.updateVolume(cubeRLText)
                    }
                )
            }
        }
    }
}

@Composable
fun TransferFunctionSettings(viewModel: RendererViewModel? = null) {
    Column(
        modifier = Modifier
            .padding(16.dp)
    ) {
        Text(
            text= "Transfer Function Settings",
            fontSize = 20.sp,
        )
        Spacer(
            modifier = Modifier.height(5.dp)
        )
        Row {
            var unitStepText by remember { mutableStateOf("0.03") }
            var alphaNearOne by remember { mutableStateOf("1.0") }
            TextField(
                value = unitStepText,
                onValueChange = {
                    unitStepText = it
                    viewModel?.setUnitStep(unitStepText.toFloatOrNull())
                },
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .padding(end=5.dp),
                label = {
                    Text("Unit Step")
                },
                maxLines = 1,
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number)
            )
            TextField(
                value = alphaNearOne,
                onValueChange = {
                    alphaNearOne = it
                    viewModel?.setAlphaNearOne(alphaNearOne.toFloatOrNull())
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start=5.dp),
                label = {
                    Text("Alpha Near One")
                },
                maxLines = 1,
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
            )
        }
        Row {
            var lutFileText by remember { mutableStateOf("Cube Lut 1") }
            var lutFileDropdownExpanded by remember { mutableStateOf(false) }
            Box(
                modifier = Modifier
                    .padding(end=5.dp)
                    .fillMaxWidth(0.5f)
                    .clickable { lutFileDropdownExpanded = !lutFileDropdownExpanded },
            ) {
                OutlinedTextField(
                    value = lutFileText,
                    onValueChange = { lutFileText = it },
                    enabled = false,
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text("LUT File")
                    },
                    trailingIcon = {
                        Icon(Icons.Default.MoreVert,"Select LUT File")
                    },
                    colors = OutlinedTextFieldDefaults.colors().copy(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledIndicatorColor = MaterialTheme.colorScheme.outline,
                        disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant)
                )
                DropdownMenu(
                    expanded = lutFileDropdownExpanded,
                    onDismissRequest = { lutFileDropdownExpanded = false }
                ) {
                    val cubeLut1Text = "Cube Lut 1"
                    DropdownMenuItem(
                        text = { Text(cubeLut1Text) },
                        onClick = {
                            lutFileText = cubeLut1Text
                            viewModel?.setLutFile(cubeLut1Text)
                        }
                    )
                }
            }
            var levoyFileText by remember { mutableStateOf("Cube Levoy 3") }
            var levoyFileDropdownExpanded by remember { mutableStateOf(false) }
            Box(modifier = Modifier
                .padding(start=5.dp)
                .fillMaxWidth()
                .clickable { levoyFileDropdownExpanded = !levoyFileDropdownExpanded }) {
                OutlinedTextField(
                    value = levoyFileText,
                    onValueChange = { levoyFileText = it },
                    enabled = false,
                    label = {
                        Text("Levoy File")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        Icon(Icons.Default.MoreVert,"Select Levoy File")
                    },
                    colors = OutlinedTextFieldDefaults.colors().copy(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledIndicatorColor = MaterialTheme.colorScheme.outline,
                        disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant)
                )
                DropdownMenu(
                    expanded = levoyFileDropdownExpanded,
                    onDismissRequest = { levoyFileDropdownExpanded = false }
                ) {
                    val cubeLevoy3Text = "Cube Levoy 3"
                    DropdownMenuItem(
                        text = { Text(cubeLevoy3Text) },
                        onClick = {
                            levoyFileText = cubeLevoy3Text
                            viewModel?.setLevoyFile(cubeLevoy3Text)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun LightingSettings(viewModel: RendererViewModel? = null) {
    Column(
        Modifier.padding(16.dp)
    ) {
        Text(
            text = "Lighting Settings",
            fontSize = 20.sp
        )
        Spacer(
            modifier = Modifier.height(5.dp)
        )
        var lightingFileText by remember { mutableStateOf("Lighting 1") }
        var lightingDropdownExpanded by remember { mutableStateOf(false) }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { lightingDropdownExpanded = !lightingDropdownExpanded },
        ) {

            OutlinedTextField(
                value = lightingFileText,
                onValueChange = { lightingFileText = it },
                enabled = false,
                label = {
                    Text("Lighting File")
                },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    Icon(Icons.Default.MoreVert, "Select Volume File")
                },
                colors = OutlinedTextFieldDefaults.colors().copy(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledIndicatorColor = MaterialTheme.colorScheme.outline,
                    disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            DropdownMenu(
                expanded = lightingDropdownExpanded,
                onDismissRequest = { lightingDropdownExpanded = false }
            ) {
                val lighting1Text = "Lighting 1"
                DropdownMenuItem(
                    text = { Text(lighting1Text) },
                    onClick = {
                        lightingFileText = lighting1Text
                        viewModel?.setLightingFile(lighting1Text)
                    }
                )
            }
        }
        Spacer(
            modifier = Modifier.height(10.dp)
        )
        Row {
            var kAmbientText by remember { mutableStateOf("0.2") }
            var kDiffuseText by remember { mutableStateOf("0.8") }
            TextField(
                value = kAmbientText,
                onValueChange = {
                    kAmbientText = it
                    viewModel?.setKAmbient(kAmbientText.toFloatOrNull())
                },
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .padding(end=5.dp),
                label = {
                    Text("Ambient Factor")
                },
                maxLines = 1,
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number)
            )
            TextField(
                value = kDiffuseText,
                onValueChange = {
                    kDiffuseText = it
                    viewModel?.setKDiffuse(kDiffuseText.toFloatOrNull())
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start=5.dp),
                label = {
                    Text("Diffuse Factor")
                },
                maxLines = 1,
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
            )
        }
        Spacer(
            modifier = Modifier.height(10.dp)
        )
        Row {
            var kSpecularText by remember { mutableStateOf("0.1") }
            var shininessText by remember { mutableStateOf("150") }
            TextField(
                value = kSpecularText,
                onValueChange = {
                    kSpecularText = it
                    viewModel?.setKSpecular(kSpecularText.toFloatOrNull())
                },
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .padding(end=5.dp),
                label = {
                    Text("Specular Factor")
                },
                maxLines = 1,
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number)
            )
            TextField(
                value = shininessText,
                onValueChange = {
                    shininessText = it
                    viewModel?.setShininess(shininessText.toFloatOrNull())
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start=5.dp),
                label = {
                    Text("Shininess Exponent")
                },
                maxLines = 1,
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
            )
        }
        Spacer(
            modifier = Modifier.height(10.dp)
        )
        Row {
            var depthCueNearXText by remember { mutableStateOf("1.1") }
            var depthCueNearYText by remember { mutableStateOf("1.1") }
            var depthCueNearZText by remember { mutableStateOf("1.1")}
            TextField(
                value = depthCueNearXText,
                onValueChange = {
                    depthCueNearXText = it
                    viewModel?.setDepthCueNearX(depthCueNearXText.toFloatOrNull())
                },
                modifier = Modifier
                    .fillMaxWidth(0.33f)
                    .padding(end=5.dp),
                label = {
                    Text("Depth Cue Near X")
                },
                maxLines = 1,
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number)
            )
            TextField(
                value = depthCueNearYText,
                onValueChange = {
                    depthCueNearYText = it
                    viewModel?.setDepthCueNearY(depthCueNearYText.toFloatOrNull())
                },
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .padding(start=5.dp, end=5.dp),
                label = {
                    Text("Depth Cue Near Y")
                },
                maxLines = 1,
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
            )
            TextField(
                value = depthCueNearZText,
                onValueChange = {
                    depthCueNearZText = it
                    viewModel?.setDepthCueNearZ(depthCueNearZText.toFloatOrNull())
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start=5.dp),
                label = {
                    Text("Depth Cue Near Z")
                },
                maxLines = 1,
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
            )
        }
        Spacer(
            modifier = Modifier.height(10.dp)
        )
        Row {
            var depthCueFarXText by remember { mutableStateOf("0.4") }
            var depthCueFarYText by remember { mutableStateOf("0.4") }
            var depthCueFarZText by remember { mutableStateOf("0.4")}
            TextField(
                value = depthCueFarXText,
                onValueChange = {
                    depthCueFarXText = it
                    viewModel?.setDepthCueFarX(depthCueFarXText.toFloatOrNull())
                },
                modifier = Modifier
                    .fillMaxWidth(0.33f)
                    .padding(end=5.dp),
                label = {
                    Text("Depth Cue Far X")
                },
                maxLines = 1,
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number)
            )
            TextField(
                value = depthCueFarYText,
                onValueChange = {
                    depthCueFarYText = it
                    viewModel?.setDepthCueFarY(depthCueFarYText.toFloatOrNull())
                },
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .padding(start=5.dp, end=5.dp),
                label = {
                    Text("Depth Cue Far Y")
                },
                maxLines = 1,
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
            )
            TextField(
                value = depthCueFarZText,
                onValueChange = {
                    depthCueFarZText = it
                    viewModel?.setDepthCueFarZ(depthCueFarZText.toFloatOrNull())
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start=5.dp),
                label = {
                    Text("Depth Cue Far \nZ")
                },
                maxLines = 1,
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
            )
        }
    }
}

@Composable
fun Settings(viewModel: RendererViewModel? = null, maxHeight: Float = 0.4f) {
    LazyColumn(
        modifier = Modifier.fillMaxHeight(maxHeight)
    ) {
        item {
            VolumeSettings()
        }
        item {
            RendererSettings(viewModel)
        }
        item {
            CameraSettings(viewModel)
        }
        item {
            TransferFunctionSettings()
        }
        item {
            LightingSettings()
        }
    }
}

@Composable
fun RenderButton(viewModel: RendererViewModel? = null, applicationContext: android.content.Context? = null, loading: MutableState<Boolean>? = null) {
    Button(
        onClick =  {
            applicationContext?.let {
                viewModel?.renderVolume(it)
                loading?.value = true
            }
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Render")
    }
}

@Preview(showBackground = true)
@Composable
fun RenderSettingsPreview() {
    RendererSettings()
}

@Preview(showBackground = true)
@Composable
fun CameraSettingsPreview() {
    CameraSettings()
}

@Preview(showBackground = true)
@Composable
fun VolumeSettingsPreview() {
    VolumeSettings()
}

@Preview(showBackground = true)
@Composable
fun TransferFunctionSettingsPreview() {
    TransferFunctionSettings()
}

@Preview(showBackground = true)
@Composable
fun LightingSettingsPreview() {
    LightingSettings()
}

@Preview(showBackground = true)
@Composable
fun SettingsPreview() {
    Settings(maxHeight = 1f)
}

@Preview(showBackground = true)
@Composable
fun RenderButtonPreview() {
    RenderButton()
}

@Preview(showBackground = true)
@Composable
fun AppPreview() {
    val width = 200 // example width
    val height = 300 // example height
    val config = Bitmap.Config.ARGB_8888 // example config, see options below
    val bitmap: Bitmap = createBitmap(width, height, config)
    val pixels = IntArray(bitmap.width * bitmap.height)
    bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
    for (y in 0..<bitmap.height) {
        for (x in 0..<bitmap.width) {
            pixels[x + bitmap.width * y] = (255 shl 24) or (0 shl 16) or (0  shl 8) or ((255 - (x.toFloat()) / bitmap.width.toFloat() * 255).toInt())
        }
    }
    bitmap.setPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
    DrawScreen(bitmap)
}
