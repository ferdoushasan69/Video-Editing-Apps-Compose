package com.example.videoeditorapps.home

import android.annotation.SuppressLint
import android.content.Context
import android.database.Cursor
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.media3.common.C
import com.arthenica.mobileffmpeg.Config
import com.arthenica.mobileffmpeg.Config.RETURN_CODE_CANCEL
import com.arthenica.mobileffmpeg.Config.RETURN_CODE_SUCCESS
import com.arthenica.mobileffmpeg.Config.TAG
import com.arthenica.mobileffmpeg.ExecuteCallback
import com.arthenica.mobileffmpeg.FFmpeg
import com.arthenica.mobileffmpeg.LogCallback
import com.arthenica.mobileffmpeg.LogMessage
import com.arthenica.mobileffmpeg.Statistics
import com.arthenica.mobileffmpeg.StatisticsCallback
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileOutputStream

class VideoEditorViewModel : ViewModel() {
    private val _trimStatus = MutableStateFlow<String?>(null)
    val trimStatus: StateFlow<String?> = _trimStatus.asStateFlow()
    private val _selectedVideoUri = MutableStateFlow<Uri?>(null)
    val selectedVideoUri = _selectedVideoUri.asStateFlow()

    private val _durationText = MutableStateFlow<String>("")
    val durationText: StateFlow<String> = _durationText.asStateFlow()

    private val _videoDuration = MutableStateFlow<Float>(0f)
    val videoDuration: StateFlow<Float> = _videoDuration.asStateFlow()

    private val _sliderRange = MutableStateFlow(0f..0f)
    val sliderRange = _sliderRange.asStateFlow()


    private val _showCropDialog = MutableStateFlow(false)
    val showCropDialog = _showCropDialog.asStateFlow()

    private val _textValue = MutableStateFlow("")
    val textValue = _textValue.asStateFlow()

    private val _croppedImageUri = MutableStateFlow<Uri?>(null)
    val croppedImageUri: StateFlow<Uri?> = _croppedImageUri.asStateFlow()

    private val _isCompressing = MutableStateFlow<Boolean>(false)
    val isCompressing: StateFlow<Boolean> = _isCompressing.asStateFlow()

    private val _isCutting = MutableStateFlow<Boolean>(false)
    val isCutting: StateFlow<Boolean> = _isCutting.asStateFlow()

    private val _isCropping = MutableStateFlow<Boolean>(false)
    val isCropping: StateFlow<Boolean> = _isCropping.asStateFlow()

    private val _selectedAudioUri = MutableStateFlow<Uri?>(null)
    val selectedAudioUri: StateFlow<Uri?> = _selectedAudioUri.asStateFlow()

    private val _isMerging = MutableStateFlow<Boolean>(false)
    val isMerging: StateFlow<Boolean> = _isMerging.asStateFlow()

    fun onAudioPicked(uri: Uri) {
        _selectedAudioUri.value = uri
    }


    fun setCroppedImageUri(uri: Uri?) {
        _croppedImageUri.value = uri
    }

    fun onVideoPicked(uri: Uri) {
        _selectedVideoUri.value = uri
    }

    fun updateSliderRange(range: ClosedFloatingPointRange<Float>) {
        _sliderRange.value = range
    }

    fun updateDuration(durationMs: Long) {
        if (durationMs > 0 && durationMs != C.TIME_UNSET) {
            val durationSeconds = durationMs / 1000f
            _videoDuration.value = durationSeconds
            _sliderRange.value = 0f..durationSeconds
            _durationText.value = formatDuration(durationMs)
        }
    }

    fun onTextValueChange(text: String) {
        _textValue.value = text
    }


    fun showCropDialog(boolean: Boolean) {
        _showCropDialog.value = boolean
    }

    fun clearText() {
        _textValue.value = ""
    }

    fun trimVideo(
        startMs: Int,
        endMs: Int,
        fileName: String,
        context: Context,
        uri: Uri,
        onSuccess: (File) -> Unit
    ): File? {
        _isCutting.value = true
        val trimDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "TrimVideo"
        )

        if (!trimDir.exists()) trimDir.mkdir()
        val fileNameClean = if (fileName.isNotBlank()) {
            fileName.replace(".mp4", "", ignoreCase = true)
        } else {
            "trimmed_video_${System.currentTimeMillis()}"
        }

        val dest = File(trimDir, "${fileNameClean}.mp4")
        val originalPath = getRealFromUri(context, uri = uri)

        val command = arrayOf(
            "-y",
            "-ss",
            (startMs / 1000).toString(),
            "-i",
            originalPath,
            "-ss",
            (startMs / 1000).toString(),
            "-t",
            ((endMs - startMs) / 1000).toString(),
            "-c:v",
            "libx264",
            "-preset",
            "ultrafast",
            "-crf",
            "23",            // Optional: quality control (lower is better quality, 18–28 range)
            "-c:a",
            "aac",
            "-b:a",
            "128k",
            dest.absolutePath
        )
        Log.d("FFmpeg", "Original video path: $originalPath")

        if (!File(originalPath).exists() || originalPath.isBlank()) {
            Toast.makeText(context, "Original video file not found!", Toast.LENGTH_SHORT).show()
            return null
        }
        if (endMs <= startMs) {
            Toast.makeText(context, "Invalid trim range", Toast.LENGTH_SHORT).show()
            return null
        }
        executeFfmpegBinary(
            command,
            context = context,
            outputFile = trimDir,
            onSuccess = {
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(dest.absolutePath),
                    arrayOf("video/mp4"),
                    null
                )
                _isCutting.value = false
                onSuccess(dest)
            },
            onFailure = {
                Toast.makeText(context, "Trim failed: $it", Toast.LENGTH_SHORT).show()
            }
        )
        Toast.makeText(context, "Video Trimmed Success", Toast.LENGTH_SHORT).show()
        return dest
    }


    fun compressVideo(
        context: Context, uri: Uri,
        onSuccess: (File) -> Unit
    ) {
        _isCompressing.value = true
        val videosDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "CompressVideos"
        )

        if (!videosDir.exists()) videosDir.mkdirs()
        val originalFileName =
            getFileNameFromUri(context, uri)?.substringBeforeLast(".") ?: "compress"
//        val fileName = "compress_file"
        val fileExt = ".mp4"

        val inputPath = getRealFromUri(context = context, uri = uri)
        val dest = File(videosDir, originalFileName + fileExt)

        if (!File(inputPath).exists()) {
            Log.e("Compress", "compressVideo: Input path is not exits on file path")
            return
        }
        val command = arrayOf(
            "-y",
            "-i",
            inputPath,
            "-s",
            "160x120",
            "-r",
            "25",
            "-vcodec",
            "mpeg4",
            "-b:v",
            "150k",
            "-b:a",
            "48000",
            "-ac",
            "2",
            "-ar",
            "22050",
            dest.absolutePath
        )

        executeFfmpegBinary(
            command = command,
            context = context,
            outputFile = dest,
            onSuccess = { file ->
                Log.d("Compress", "compressVideo: Success -$file")
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(dest.absolutePath),
                    null,
                    null
                )
                _isCompressing.value = false
                onSuccess(file)
            },
            onFailure = {
                Log.e("Compress", "compressVideo: Failed - $it")
            }
        )
    }

    fun mergingVideo(
        context: Context,
        videoUri: Uri,
        audioUri: Uri,
        onSuccess: (File) -> Unit
    ) {
        _isMerging.value = true
        val outPutDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "MergingVideos"
        )

        if (!outPutDir.exists()) outPutDir.mkdir()

        val fileName = "merged_video"
        val fileExt = ".mp4"
        val dest = File(outPutDir, fileName + fileExt)
        val audioFileAbsolutePath = copyAudioDownload(context, uri = audioUri)
        val audioPath = audioFileAbsolutePath.absolutePath
        val videoPath = getRealFromUri(context = context, uri = videoUri)

        val command = arrayOf(
            "-i", videoPath,
            "-i", audioPath,
            "-c:v", "libx264",
            "-c:a", "aac",
            "-pix_fmt", "yuv420p",
            "-shortest",
            dest.absolutePath
        )

//        val command = arrayOf(
//            "-i",videoPath,"-i",audioPath, "-c:v", "copy", "-c:a", "aac","-shortest", dest.absolutePath
//        )
        executeFfmpegBinary(
            command = command,
            context = context,
            outputFile = dest,
            onSuccess = { file ->
                Log.d("Merged_video", "mergingVideo: Success $file")
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(dest.absolutePath),
                    null,
                    null
                )
                _isMerging.value = false
                onSuccess(file)
            },
            onFailure = {
                Log.e(TAG, "mergingVideo: Failed $it")
            }
        )

    }


    fun cropVideo(
        uri: Uri,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        fileName: String,
        onSuccess: (File) -> Unit,
        context: Context
    ) {
        _isCropping.value = true
        val outPutDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "CropVideos"
        )

        if (!outPutDir.exists()) outPutDir.mkdirs()

        val fileName = fileName
        val fileExt = ".mp4"
        val outPutFile = File(outPutDir, fileName + fileExt)
        val inputPath = getRealFromUri(context, uri)

        if (!File(inputPath).exists()) {
            Log.e(TAG, "cropVideo: Input path is not exits on file path ")
            return
        }
        val command = arrayOf(
            "-y",
            "-i", inputPath,
            "-filter:v", "crop=$width:$height:$x:$y",
            "-threads", "5",
            "-preset", "ultrafast",
            "-strict", "-2",
            "-c:a", "copy",
            outPutFile.absolutePath
        )
        executeFfmpegBinary(
            command = command,
            context = context,
            outputFile = outPutFile,
            onSuccess = {
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(outPutFile.absolutePath),
                    null,
                    null
                )
                _isCropping.value = false
                onSuccess(outPutFile)
            },
            onFailure = { message ->
                Log.e(TAG, "cropVideo: failed to crop $message")
            }
        )

    }


    @SuppressLint("Recycle")
    fun getRealFromUri(context: Context, uri: Uri): String {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val file = File(context.cacheDir, "temp_video_${System.currentTimeMillis()}.mp4")

            file.outputStream().use { fileOut ->
                inputStream?.copyTo(fileOut)

            }
            file.absolutePath

        } catch (e: Exception) {
            e.printStackTrace()
            return ""
        }
    }


    fun formatDuration(durationMs: Long): String {
        val totalSeconds = durationMs / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return if (hours > 0) {
            "%d:%02d:%02d".format(hours, minutes, seconds)
        } else {
            "%02d:%02d".format(minutes, seconds)
        }

    }


    fun executeFfmpegBinary(
        command: Array<String>,
        context: Context,
        outputFile: File,
        onSuccess: (File) -> Unit,
        onFailure: (String) -> Unit = { reason -> Log.e(TAG, "executeFfmpegBinary: $reason") }
    ) {
        Config.enableLogCallback(object : LogCallback {

            override fun apply(logMassage: LogMessage?) {
                Log.d(TAG, "apply: ${logMassage?.text}")
            }

        })

        Config.enableStatisticsCallback(object : StatisticsCallback {

            override fun apply(statisticMessage: Statistics?) {
                Log.d(
                    TAG, "apply: ${
                        String.format(
                            "frame : %d,time : %d",
                            statisticMessage?.videoFrameNumber, statisticMessage?.time
                        )
                    }"
                )

                Log.d("TAG", "started Command ffmpeg : ${command.contentToString()}")
            }

        })

        Log.d("TAG", "started Command ffmpeg : ${command.contentToString()}")

        FFmpeg.executeAsync(command, object : ExecuteCallback {

            override fun apply(execuationId: Long, returnCode: Int) {

                when (returnCode) {
                    RETURN_CODE_SUCCESS -> {
                        Log.d("TAG", "Finished command Success: ${command.contentToString()}")
                        Toast.makeText(context, "Video trimmed successfully!", Toast.LENGTH_SHORT)
                            .show()
                        onSuccess(outputFile)

                    }

                    RETURN_CODE_CANCEL -> {
                        Log.e("TAG", "Async command cancel by user")
                        onFailure("Execution Cancel")
                    }

                    else -> {
                        Log.e("TAG", "Async command cancel by return code = %d $returnCode")
                        onFailure("Failed with return code $returnCode")
                    }

                }

            }

        })

    }

    @SuppressLint("NewApi")
    fun getFileNameFromUri(context: Context, uri: Uri): String? {
        return try {

            if (uri.scheme == "content") {
                val cursor =
                    context.contentResolver.query(uri, null, null, null)
                cursor?.use {
                    if (it.moveToFirst()) {
                        val nameIndex =
                            it.getColumnIndex(OpenableColumns.DISPLAY_NAME)

                        if (nameIndex != -1) {
                            return it.getString(nameIndex)
                        }
                    }
                }
            }

            uri.path?.substringAfterLast('/')

        } catch (e: Exception) {
            e.printStackTrace()
            uri.path?.substringAfterLast('/')

        }
    }

    @SuppressLint("Recycle")
    fun copyAudioDownload(context: Context, uri: Uri): File {
         try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val fileName = "picked_audio_${System.currentTimeMillis()}.aac"
            val outputDir = File(context.getExternalFilesDir(null), "PickedAudios")
            if (!outputDir.exists()) outputDir.mkdirs()
            val file = File(outputDir, fileName)
            val outputStream = file.outputStream()
            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()
          return file
        } catch (e: Exception) {
            e.printStackTrace()
             throw RuntimeException("Failed to copy audio", e)
        }

    }

    @SuppressLint("Recycle")
    fun getRealPathFromUri(context: Context, uri: Uri): String?{
        val contentResolver = context.contentResolver
        val projection = arrayOf(MediaStore.Audio.Media.DATA)
        val cursor = contentResolver.query(uri,projection,null,null,null)

        cursor?.use {
            if (it.moveToFirst()){
                val nameIndex = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                return it.getString(nameIndex)
            }
        }
        return null
    }
}
