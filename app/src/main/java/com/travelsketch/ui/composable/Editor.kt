
package com.travelsketch.ui.composable

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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.travelsketch.R
import com.travelsketch.data.model.BoxType
import com.travelsketch.viewmodel.CanvasViewModel

@Composable
fun Editor(
    canvasViewModel: CanvasViewModel,
    showDialog: MutableState<Boolean>
) {
    val selected by canvasViewModel.selected

    val btnLst = when (selected?.type) {
        BoxType.TEXT.toString() -> {
            mutableListOf(
                R.drawable.pallete_btn,
                R.drawable.delete_btn
            )
        }
        else -> mutableListOf(
            R.drawable.text_btn,
            R.drawable.img_btn,
            R.drawable.record_btn,
            R.drawable.video_btn
        )
    }

    btnLst.add(R.drawable.save_btn)

    Row(
        modifier = Modifier
            .padding(1.dp)
            .border(1.dp, Color.Black),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (res in btnLst) {
            Image(
                modifier = Modifier
                    .width(45.dp)
                    .height(45.dp)
                    .padding(2.dp)
                    .clickable {
                        when (res) {
                            R.drawable.text_btn -> showDialog.value = true
                            R.drawable.save_btn -> canvasViewModel.saveAll()
                            R.drawable.delete_btn -> canvasViewModel.delete()
                        }
                    },
                painter = painterResource(res),
                contentDescription = "Button Image"
            )
        }
    }
}