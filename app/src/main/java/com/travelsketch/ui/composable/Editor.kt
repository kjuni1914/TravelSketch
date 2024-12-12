package com.travelsketch.ui.composable

import CanvasViewModel
import android.app.Activity
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.travelsketch.R
import com.travelsketch.data.model.BoxType

@Composable
fun Editor(
    canvasViewModel: CanvasViewModel,
    showDialog: MutableState<Boolean>,
    showImageSourceDialog: MutableState<Boolean>,
    showVideoSourceDialog : MutableState<Boolean>,
    createAndSharePdf: () -> Unit
) {
    val selected = canvasViewModel.selected.value

    val btnLst = mutableListOf<Pair<Int, () -> Unit>>()

    when (selected?.type) {
        BoxType.TEXT.toString() -> {
            btnLst.add(R.drawable.pallete_btn to { /* TODO: Change palette */ })
            btnLst.add(R.drawable.delete_btn to { canvasViewModel.delete() })
        }
        BoxType.IMAGE.toString() -> {
            btnLst.add(R.drawable.delete_btn to { canvasViewModel.delete() })
        }
        BoxType.VIDEO.toString() -> {
            btnLst.add(R.drawable.delete_btn to { canvasViewModel.delete() })
        }
        else -> {
            btnLst.add(R.drawable.text_btn to { showDialog.value = true })
            btnLst.add(R.drawable.img_btn to { showImageSourceDialog.value = true })
            btnLst.add(R.drawable.record_btn to { /* TODO: Implement record */ })
            btnLst.add(R.drawable.video_btn to { showVideoSourceDialog.value = true })
        }
    }
    btnLst.add(R.drawable.share_btn to { createAndSharePdf() })

    btnLst.add(R.drawable.save_btn to { canvasViewModel.saveAll() })

    Row(
        modifier = Modifier
            .padding(1.dp)
            .border(1.dp, Color.Black),
        horizontalArrangement = Arrangement.Center
    ) {
        for ((resId, action) in btnLst) {
            Image(
                painter = painterResource(resId),
                contentDescription = null,
                modifier = Modifier
                    .width(45.dp)
                    .height(45.dp)
                    .padding(2.dp)
                    .clickable { action() }
            )
        }
    }
}
