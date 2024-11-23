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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.travelsketch.R
import com.travelsketch.data.model.BoxType

@Composable
fun Editor(
    mode: BoxType? = null
) {
    var btnLst = when (mode) {
        null -> {
            mutableListOf(
                R.drawable.text_btn,
                R.drawable.img_btn,
                R.drawable.record_btn
            )
        }
        BoxType.IMAGE -> TODO()
        BoxType.VIDEO -> TODO()
        BoxType.TEXT -> TODO()
        BoxType.RECORD -> TODO()
    }

    btnLst.add(R.drawable.select_btn)

    Row(modifier = Modifier
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
                    .clickable { },
                painter = painterResource(res),
                contentDescription = "Button Image"
            )
        }
    }
}