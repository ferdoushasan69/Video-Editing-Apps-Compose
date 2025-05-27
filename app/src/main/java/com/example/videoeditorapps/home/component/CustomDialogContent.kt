package com.example.videoeditorapps.home.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

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

