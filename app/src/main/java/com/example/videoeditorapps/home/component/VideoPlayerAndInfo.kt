package com.example.videoeditorapps.home.component

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.videoeditorapps.home.ExoplayerVIew

@Composable
fun VideoPlayerAndInfo(
    selectedVideoUri: Uri?,
    durationText: String,
    context: Context,
    onDurationReady: (Long) -> Unit,
    curPosition: (Long) -> Unit
) {
    if (selectedVideoUri != null) {
        ExoplayerVIew(
            context = context,
            uri = selectedVideoUri,
            onDurationReady = onDurationReady,
            curPosition = curPosition
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("00:00:00", fontSize = 14.sp)
            Text(durationText, fontSize = 14.sp)
        }
    }
}

