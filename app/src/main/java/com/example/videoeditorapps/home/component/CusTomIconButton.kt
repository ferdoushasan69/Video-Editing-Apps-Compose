package com.example.videoeditorapps.home.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CusTomIconButton(
    modifier: Modifier = Modifier,
    onIconButtonClick: () -> Unit,
    painter: Painter,
    text: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
    ) {
        IconButton(onClick = onIconButtonClick ){
            Icon(
                painter = painter,
                contentDescription = null,
            )
        }
        Spacer(Modifier.height(5.dp))
        Text(text, fontSize = 15.sp, textAlign = TextAlign.Center)
    }

}


