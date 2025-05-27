package com.example.videoeditorapps.home

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration

data class WindowSize(
    val width : WindowType,
    val height : WindowType,
)

enum class WindowType{
    COMPACT,MEDIUM,EXPANDED
}


@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun rememberWindowSize(modifier: Modifier = Modifier) : WindowSize {
    val configuration = LocalConfiguration.current

    return WindowSize(
        width = when{
            configuration.screenWidthDp < 600 -> WindowType.COMPACT
            configuration.screenWidthDp < 840 -> WindowType.MEDIUM
            else-> WindowType.EXPANDED
        },
        height = when{
            configuration.screenHeightDp < 600 -> WindowType.COMPACT
            configuration.screenHeightDp < 840 -> WindowType.MEDIUM
            else -> WindowType.EXPANDED
        }
    )
    
}