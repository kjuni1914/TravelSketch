package com.travelsketch.ui.composable

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun StyledButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp), // 모서리를 둥글게 설정
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF6200EE), // 버튼 배경색
            contentColor = Color.White         // 텍스트 색상
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 8.dp, // 기본 그림자
            pressedElevation = 4.dp  // 눌렀을 때 그림자
        )
    ) {
        Text(text = text)
    }
}