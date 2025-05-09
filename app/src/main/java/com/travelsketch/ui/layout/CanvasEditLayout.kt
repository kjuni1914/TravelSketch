package com.travelsketch.ui.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun CanvasEditLayout(
    canvas: @Composable () -> Unit,
    editor: @Composable () -> Unit,
    button: @Composable () -> Unit,
    statusBar: @Composable () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        canvas()

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .wrapContentSize()
        ) {
            button()
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .wrapContentSize()
        ) {
            editor()
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .wrapContentSize()
                .background(Color.Red)
                .height(40.dp)
        ) {
            statusBar()
        }
    }
}
