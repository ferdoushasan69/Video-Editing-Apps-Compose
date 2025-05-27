package com.example.videoeditorapps.home

import android.content.Context
import android.net.Uri
import android.util.Log
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.arthenica.mobileffmpeg.Config.TAG

@OptIn(UnstableApi::class)
@Composable
fun ExoplayerVIew(
    modifier: Modifier = Modifier,
    context: Context,
    uri: Uri,
    onDurationReady: (Long) -> Unit,
    curPosition: (Long) -> Unit
) {
    val player = remember(uri) {

        val rendersFactory = DefaultRenderersFactory(context)
            .setEnableDecoderFallback(true)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF)


        ExoPlayer.Builder(context)
            .setRenderersFactory(rendersFactory)
            .build().apply {
            val mediaItem = MediaItem.fromUri(uri)
            setMediaItem(mediaItem)
            repeatMode = Player.REPEAT_MODE_ALL
            curPosition(currentPosition)
            prepare()
            playWhenReady = true
        }

    }

    DisposableEffect(key1 = uri) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                super.onPlaybackStateChanged(playbackState)
                if (playbackState == Player.STATE_READY) {
                    val duration  = player.duration
                    onDurationReady(duration)
                }
            }
        }

        player.addListener(listener)
        onDispose {
            try {
                player.removeListener(listener)
                player.release()
            }catch (e : Exception){
                Log.d(TAG, "ExoplayerVIew: Error ${e.message}")
            }
        }
    }
    AndroidView(
        factory = {

            PlayerView(context).apply {
                this.player = player
                useController = true
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT // বা RESIZE_MODE_ZOOM
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,

                    )
            }
        },
        update = {
            it.player = player
        },
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f),
    )

}
