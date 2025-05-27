package com.example.videoeditorapps.home

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.videoeditorapps.home.component.VideoControls
import com.example.videoeditorapps.home.component.VideoPlayerAndInfo
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.launch


@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@SuppressLint("IntentReset")
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: VideoEditorViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var selectedVideoUri = viewModel.selectedVideoUri.collectAsState()
    val durationText by viewModel.durationText.collectAsState()
    val videoDuration by viewModel.videoDuration.collectAsState()
    val sliderRange by viewModel.sliderRange.collectAsState()
    var showAlertDialog by remember { mutableStateOf(false) }
    var showCroppedDialog = viewModel.showCropDialog.collectAsState()
    var trimmedVideoUri by remember { mutableStateOf<Uri?>(null) }
    var croppedVideoUri by remember { mutableStateOf<Uri?>(null) }
    var showCropButton = showCroppedDialog.value
    var showRangeBar by remember { mutableStateOf(false) }

    var isCompress = viewModel.isCompressing.collectAsState()
    var isCutting = viewModel.isCutting.collectAsState()
    var isCroppingLoading = viewModel.isCropping.collectAsState()
    var audioUri by remember { mutableStateOf<Uri?>(null) }
    var mergedAudio by remember { mutableStateOf<Uri?>(null) }
    var isMerging = viewModel.isMerging.collectAsState()


    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val newUri = result.data?.data
            if (newUri != null) {
                viewModel.onVideoPicked(newUri)
                showRangeBar = false
            }
        }
    }

    val selectedAudioUri by viewModel.selectedAudioUri.collectAsState()
    val audioPickLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val newUri = result.data?.data

            newUri?.let {
                audioUri = it
                viewModel.onAudioPicked(uri = it)

            }

        }
    }
    LaunchedEffect(Unit) {
        Log.d("Merge", "selectedVideoUri = ${selectedVideoUri.value}")
        Log.d("Merge", "audioUri = $audioUri")
        Log.d("Merge", "Selected audio = $selectedAudioUri")
    }
    var curPosition by remember { mutableLongStateOf(0L) }

    var cropX by remember { mutableStateOf("") }
    var cropY by remember { mutableStateOf("") }
    var cropWidth by remember { mutableStateOf("100") }
    var cropHeight by remember { mutableStateOf("100") }
    var saveCropText by remember { mutableStateOf("") }

    var cutTextValue by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()



    if (showAlertDialog) {
        AlertDialog(
            onDismissRequest = {
                showAlertDialog = false
            },
            modifier = Modifier,
            content = {
                Column(
                    modifier = Modifier
                        .background(Color.White)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    TextField(
                        singleLine = true,
                        onValueChange = {
                            cutTextValue = it
                        },
                        placeholder = { Text(text = "Enter your file name") },
                        modifier = Modifier.border(width = 1.dp, color = Color.LightGray),
                        value = cutTextValue, colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                        )
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        enabled = cutTextValue.isNotEmpty(),
                        onClick = {
                            selectedVideoUri.value?.let { uri ->

                                val startMs = (sliderRange.start * 1000).toInt()
                                val endMs = (sliderRange.endInclusive * 1000).toInt()
                                val fileName = cutTextValue
                                scope.launch {

                                viewModel.trimVideo(
                                    startMs = startMs,
                                    endMs = endMs,
                                    fileName = fileName,
                                    context = context,
                                    uri = uri,
                                    onSuccess = {
                                        trimmedVideoUri = Uri.fromFile(it)
                                        viewModel.onVideoPicked(trimmedVideoUri ?: "".toUri())
                                    }
                                )
                                }
                            } ?: Toast.makeText(
                                context,
                                "Please upload a video first",
                                Toast.LENGTH_SHORT
                            )
                                .show()
                            showAlertDialog = false

                        }, modifier = Modifier.height(48.dp)
                    ) { Text("Save Video") }
                }
            }
        )
    }

    if (showCropButton) {
        AlertDialog(
            onDismissRequest = {
                viewModel.showCropDialog(false)
            },
            modifier = Modifier,
            content = {
                Column(
                    modifier = Modifier
                        .background(color = Color.White)
                        .padding(8.dp)
                ) {
                    CustomDialogContent(
                        modifier = Modifier,
                        textValue = cropX,
                        onValueChange = {
                            cropX = it
                        },
                        placeHolderText = "CropX"
                    )
                    CustomDialogContent(
                        modifier = Modifier,
                        textValue = cropY,
                        onValueChange = {
                            cropY = it
                        },
                        placeHolderText = "CropY"
                    )
                    CustomDialogContent(
                        modifier = Modifier,
                        textValue = cropWidth,
                        onValueChange = {
                            cropWidth = it
                        },
                        placeHolderText = "Crop Width"
                    )
                    CustomDialogContent(
                        modifier = Modifier,
                        textValue = cropHeight,
                        onValueChange = {
                            cropHeight = it
                        },
                        placeHolderText = "Crop Height"
                    )

                    CustomDialogContent(
                        modifier = Modifier,
                        textValue = saveCropText,
                        onValueChange = {
                            saveCropText = it
                        },
                        placeHolderText = "Enter filename"
                    )

                    Spacer(Modifier.height(8.dp))
                    Button(onClick = {
                        val uri = selectedVideoUri.value
                        uri?.let {
                            viewModel.cropVideo(
                                x = cropX.toIntOrNull() ?: 0,
                                y = cropY.toIntOrNull() ?: 0,
                                context = context,
                                uri = uri,
                                onSuccess = {
                                    Toast.makeText(
                                        context,
                                        "Video Cropped successfully",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    croppedVideoUri = Uri.fromFile(it)
                                    viewModel.onVideoPicked(croppedVideoUri ?: "".toUri())
                                },
                                height = cropHeight.toIntOrNull() ?: 100,
                                width = cropWidth.toIntOrNull() ?: 100,
                                fileName = saveCropText.trim()
                            )
                        } ?: Toast.makeText(context, "Cropped failed", Toast.LENGTH_SHORT).show()

                        viewModel.showCropDialog(false)
                        cropX = ""
                        cropY = ""
                        cropWidth = ""
                        cropHeight = ""
                        saveCropText = ""
                        viewModel.clearText()
                    }, modifier = Modifier.fillMaxWidth()) { Text("Save") }
                }
            }
        )
    }

    val permission = rememberMultiplePermissionsState(
        permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            listOf(
                Manifest.permission.READ_MEDIA_VIDEO,
            )
        } else {
            listOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
            )
        }
    )

    LaunchedEffect(Unit) {
        permission.launchMultiplePermissionRequest()
    }
    val windowSize = rememberWindowSize()

    val bottomSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    val showBottomSheet = isMerging.value || isCompress.value || isCutting.value || isCroppingLoading.value

    if (showBottomSheet){
        ModalBottomSheet(
            onDismissRequest = {
                viewModel.cancelAllTask()
            },
            sheetState = bottomSheetState,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
        ) {
            if (isMerging.value) {
                Progress(
                    modifier = Modifier,
                    text = "Merging video please wait..."
                )
            }

            if (isCompress.value) {
                Progress(
                    modifier = Modifier,
                    text = "compressing Video please wait..."
                )
            }
            if (isCutting.value) {
                Progress(
                    modifier = Modifier,
                    text = "Cutting video please wait..."
                )
            }
            if (isCroppingLoading.value) {
                Progress(
                    modifier = Modifier,
                    text = "Cropping video please wait..."
                )
            }
        }

    }

    Scaffold(

    ){


            if (windowSize.width >= WindowType.EXPANDED) {
                // Large screen: Side-by-side layout
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(it)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Player and Info
                    Column(modifier = Modifier.weight(1f)) {
                        VideoPlayerAndInfo(
                            selectedVideoUri = selectedVideoUri.value,
                            durationText = durationText,
                            context = context,
                            onDurationReady = { durationMs ->
                                viewModel.updateDuration(durationMs)
                            },
                            curPosition = { pos -> curPosition = pos }
                        )
                    }
                    Spacer(Modifier.width(20.dp))
                    // Right: Controls
                    Column(modifier = Modifier.weight(1f)) {
                        VideoControls(
                            showRangeBar = showRangeBar,
                            sliderRange = sliderRange,
                            videoDuration = videoDuration,
                            onUploadClick = {
                                val intent = Intent(
                                    Intent.ACTION_PICK,
                                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                                )
                                intent.setType("video/*")
                                videoPickerLauncher.launch(intent)
                            },
                            onCutClick = {
                                if (selectedVideoUri.value == null) {
                                    Toast.makeText(
                                        context,
                                        "Please select a video first",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    return@VideoControls
                                }

                                showRangeBar = true


                                showCropButton = false
                            },
                            onCropClick = {
                                if (selectedVideoUri.value == null) {
                                    Toast.makeText(
                                        context,
                                        "Please select a video first",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    return@VideoControls
                                }

                                viewModel.showCropDialog(true) // Show the crop dialog with dimension inputs
                                showRangeBar = false // Hide range bar if opening crop dialog
                            },
                            onCompressClick = {
                                if (selectedVideoUri.value == null) {
                                    Toast.makeText(
                                        context,
                                        "Please select a video first",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    return@VideoControls
                                }
                                selectedVideoUri.value?.let { uri ->
                                    viewModel.compressVideo(
                                        context = context,
                                        uri = uri,
                                        onSuccess = { file ->
                                            Toast.makeText(
                                                context,
                                                "Video Compressed Successfully",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            viewModel.onVideoPicked(Uri.fromFile(file))
                                        },
                                    )
                                }
                                showRangeBar = false // Hide range bar if compressing
                            },
                            onSliderChange = { newRange -> viewModel.updateSliderRange(newRange) },
                            onSliderDone = {
                                showRangeBar = false
                                showAlertDialog = true // Show dialog to enter filename for trimming
                            },
                            onSliderCancel = { showRangeBar = false },
                            onMergeVideo = {
                                if (selectedVideoUri.value != null && selectedAudioUri != null) {
                                    viewModel.mergingVideo(
                                        context = context,
                                        videoUri = selectedVideoUri.value ?: "".toUri(),
                                        audioUri = selectedAudioUri ?: "".toUri(),
                                        onSuccess = {
                                            Toast.makeText(
                                                context,
                                                "Video merged successfully!",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            mergedAudio = Uri.fromFile(it)
                                            viewModel.onVideoPicked(mergedAudio ?: "".toUri())
                                        }
                                    )
                                } else {
                                    Toast.makeText(
                                        context,
                                        "Select both video and audio first!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            addAudio = { val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                                addCategory(Intent.CATEGORY_OPENABLE)
                                type = "audio/*"
                            }
                                audioPickLauncher.launch(intent)}
                        )
                    }
                }
            } else {
                // Compact/Medium screen: Stacked layout
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(it)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    VideoPlayerAndInfo(
                        selectedVideoUri = selectedVideoUri.value ?: "".toUri(),
                        durationText = durationText,
                        context = context,
                        onDurationReady = { durationMs ->
                            viewModel.updateDuration(durationMs)
                        },
                        curPosition = { pos -> curPosition = pos }
                    )
                    Spacer(Modifier.height(16.dp)) // Add some space between player and controls
                    VideoControls(
                        showRangeBar = showRangeBar,
                        sliderRange = sliderRange,
                        videoDuration = videoDuration,
                        onUploadClick = {
                            val intent = Intent(
                                Intent.ACTION_PICK,
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                            )
                            intent.setType("video/*")
                            videoPickerLauncher.launch(intent)
                        },
                        onCutClick = {
                            if (selectedVideoUri.value == null) {
                                Toast.makeText(
                                    context,
                                    "Please select a video first",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@VideoControls
                            }
                            showRangeBar = true
                            showCropButton = false
                        },
                        onCropClick = {
                            if (selectedVideoUri.value == null) {
                                Toast.makeText(
                                    context,
                                    "Please select a video first",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@VideoControls
                            }
                            viewModel.showCropDialog(true) // Show the crop dialog
                            showRangeBar = false
                        },
                        onCompressClick = {
                            if (selectedVideoUri.value == null) {
                                Toast.makeText(
                                    context,
                                    "Please select a video first",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@VideoControls
                            }
                            selectedVideoUri.value?.let { uri ->
                                viewModel.compressVideo(
                                    context = context,
                                    uri = uri,
                                    onSuccess = { file ->
                                        Toast.makeText(
                                            context,
                                            "Video Compressed Successfully",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        viewModel.onVideoPicked(Uri.fromFile(file))
                                    },
                                )
                            }
                            showRangeBar = false
                        },
                        onSliderChange = { newRange -> viewModel.updateSliderRange(newRange) },
                        onSliderDone = {
                            showRangeBar = false
                            showAlertDialog = true
                        },
                        onSliderCancel = { showRangeBar = false },
                        onMergeVideo = {
                            if (selectedVideoUri.value != null && selectedAudioUri != null) {
                                viewModel.mergingVideo(
                                    context = context,
                                    videoUri = selectedVideoUri.value ?: "".toUri(),
                                    audioUri = selectedAudioUri ?: "".toUri(),
                                    onSuccess = {
                                        Toast.makeText(
                                            context,
                                            "Video merged successfully!",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        mergedAudio = Uri.fromFile(it)
                                        viewModel.onVideoPicked(mergedAudio ?: "".toUri())
                                    }
                                )
                            } else {
                                Toast.makeText(
                                    context,
                                    "Select both video and audio first!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        addAudio = {
                            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                                addCategory(Intent.CATEGORY_OPENABLE)
                                type = "audio/*"
                            }
                            audioPickLauncher.launch(intent)
                        }
                    )

                }
            }


    }

}

@Composable
fun CustomDialogContent(
    modifier: Modifier = Modifier,
    textValue: String,
    onValueChange: (String) -> Unit,
    placeHolderText: String,
) {
    Column(
        modifier = Modifier
            .background(Color.White)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TextField(
            singleLine = true,
            onValueChange = {
                onValueChange(it)
            },
            placeholder = { Text(text = placeHolderText) },
            modifier = Modifier.border(width = 1.dp, color = Color.LightGray),
            value = textValue, colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
            )
        )

    }
}


@Composable
fun Progress(modifier: Modifier = Modifier,text : String) {
   Column(
       modifier = Modifier.fillMaxWidth(),
       horizontalAlignment = Alignment.CenterHorizontally,
       verticalArrangement = Arrangement.Center) {
       CircularProgressIndicator(
           modifier = Modifier
       )
       Spacer(Modifier.height(8.dp))
       Text(text = text, fontSize = 14.sp)
       Spacer(Modifier.height(8.dp))

   }
    
}