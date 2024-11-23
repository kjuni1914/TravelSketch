import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.travelsketch.R

@Composable
fun ListElement(
    title: String,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // 카드 배경
        Card(
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp) // 고정 높이 설정
                .padding(10.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFECECEC)) // 약간 더 진한 배경색
        ) {}

        // 텍스트와 버튼 배치
        Box(modifier = Modifier.fillMaxSize()) {
            // 제목 텍스트 (상단 고정, 테두리와 배경 포함)
            Box(
                modifier = Modifier
                    .absoluteOffset(x = 20.dp, y = 5.dp) // 상단 왼쪽 고정
                    .background(color = Color.White, shape = RoundedCornerShape(5.dp))
                    .border(width = 1.dp, color = Color.Black, shape = RoundedCornerShape(5.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp) // 텍스트 주변 패딩
            ) {
                Text(
                    text = title,
                    style = TextStyle(
                        fontSize = 20.sp,
                        color = Color.Black,
                        fontWeight = FontWeight.Bold // 텍스트 굵게 설정
                    )
                )
            }
        }

        // Edit 버튼 (우측 상단 고정)
        Image(
            painter = painterResource(id = R.drawable.edit),
            contentDescription = "Edit",
            modifier = Modifier
                .absoluteOffset(x = 300.dp, y = 5.dp) // 상단 우측 고정
                .size(40.dp) // 크기 고정
        )

        // Share 버튼 (Edit 버튼 오른쪽)
        Image(
            painter = painterResource(id = R.drawable.share),
            contentDescription = "Share",
            modifier = Modifier
                .absoluteOffset(x = 345.dp, y = 5.dp) // Edit 버튼 오른쪽 고정
                .size(40.dp) // 크기 고정
        )
    }
}
