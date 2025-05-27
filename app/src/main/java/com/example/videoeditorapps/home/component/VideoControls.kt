package com.example.videoeditorapps.home.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.videoeditorapps.R

@Composable
fun VideoControls(
    showRangeBar: Boolean,
    onUploadClick: () -> Unit,
    onCutClick: () -> Unit,
    onMergeVideo: () -> Unit,
    onCropClick: () -> Unit,
    onCompressClick: () -> Unit,
    sliderRange: ClosedFloatingPointRange<Float>,
    videoDuration: Float,
    onSliderChange: (ClosedFloatingPointRange<Float>) -> Unit,
    onSliderDone: () -> Unit,
    onSliderCancel: () -> Unit,
    addAudio:()-> Unit
) {
    if (showRangeBar) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onSliderCancel) {
                Icon(Icons.Default.Close, contentDescription = null)
            }
            RangeSlider(
                value = sliderRange,
                onValueChange = onSliderChange,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp),
                valueRange = 0f..videoDuration,
                steps = 0,
            )
            IconButton(onClick = onSliderDone) {
                Icon(Icons.Default.Check, contentDescription = null)
            }
        }
    }

    Row {

    Button(onClick = onUploadClick) {
        Text("Upload Video")
    }
        IconButton(
            onClick = addAudio) {
            Icon(
                painterResource(R.drawable.outline_music_note_24),
                contentDescription = null
            )
        }
    }

    Spacer(Modifier.height(8.dp))

    Row {
        CusTomIconButton(
            onIconButtonClick = onCutClick,
            painter = painterResource(R.drawable.baseline_content_cut_24),
            text = "Cut"
        )
        Spacer(Modifier.width(8.dp))
        CusTomIconButton(
            onIconButtonClick = onCropClick,
            painter = painterResource(R.drawable.outline_crop_24),
            text = "Crop"
        )
        Spacer(Modifier.width(8.dp))

        CusTomIconButton(
            onIconButtonClick = onCompressClick,
            painter = painterResource(R.drawable.outline_compress_24),
            text = "Compress"
        )
        Spacer(Modifier.width(8.dp))

        CusTomIconButton(
            onIconButtonClick = onMergeVideo,
            painter = painterResource(R.drawable.outline_merge_24),
            text = "Merge"
        )
    }
}

