package com.travelsketch.ui.activity

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModelProvider
import com.travelsketch.ui.composable.CanvasScreen
import com.travelsketch.ui.composable.Editor
import com.travelsketch.ui.composable.StatusBar
import com.travelsketch.ui.layout.CanvasEditLayout
import com.travelsketch.viewmodel.CanvasViewModel

class CanvasActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val canvasId = intent.getStringExtra("CANVAS_ID")
        if (canvasId == null) {
            Log.e("CanvasActivity", "No canvas ID provided")
            finish()
            return
        }

        val canvasViewModel = ViewModelProvider(this)[CanvasViewModel::class.java]
        canvasViewModel.initializeCanvas(canvasId)

        setContent {
            val showDialog = remember { mutableStateOf(false) }
            val inputText = remember { mutableStateOf("") }

            CanvasEditLayout(
                canvas = {
                    CanvasScreen(canvasViewModel)
                },
                button = {
                    Button(
                        onClick = {
                            canvasViewModel.toggleIsEditable()
                        }
                    ) {
                        Text("Edit")
                    }
                },
                editor = {
                    if (canvasViewModel.getEditable()) {
                        Editor(canvasViewModel, showDialog)
                    }
                },
                statusBar = {
                    StatusBar(canvasViewModel)
                }
            )

            if (showDialog.value) {
                InputTextDialog(
                    onDismiss = { showDialog.value = false },
                    onConfirm = { text ->
                        showDialog.value = false
                        inputText.value = text
                        canvasViewModel.createText(text)
                    }
                )
            }
        }
    }
}

@Composable
fun InputTextDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val textState = remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = { Text("Enter Text") },
        text = {
            TextField(
                value = textState.value,
                onValueChange = { textState.value = it },
                label = { Text("Text") }
            )
        },
        confirmButton = {
            Button(onClick = { onConfirm(textState.value) }) {
                Text("OK")
            }
        },
        dismissButton = {
            Button(onClick = { onDismiss() }) {
                Text("Cancel")
            }
        }
    )
}
