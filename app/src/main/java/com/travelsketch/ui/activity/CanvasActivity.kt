package com.travelsketch.ui.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

class CanvasActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            Column (
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Gray),
                verticalArrangement = Arrangement.Center
            ) {
                Canvas(
                    modifier = Modifier
                        .aspectRatio(1.41f)
                        .fillMaxWidth(),
                    onDraw = {
                        drawRect(color = Color.White)
                    }
                )
            }
        }
    }
}