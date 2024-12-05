import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

@Composable
fun SquareButton(iconId: Int, size: Int = 40) {
    Button(
        onClick = {  },
        modifier = Modifier.size(size.dp), // 크기
        shape = RoundedCornerShape(4.dp), // 모서리 설정
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent) // 버튼 배경 투명
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = iconId),
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
