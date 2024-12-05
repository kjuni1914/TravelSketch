import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    canvasId: String, // canvasId 추가
    isVisible: Boolean, // 현재 is_visible 값
    isCurrentUserCanvas: Boolean, // 현재 사용자의 캔버스인지 여부
    onToggleVisibility: (String, Boolean) -> Unit, // visibility 변경 콜백 추가
//    onClick: (String) -> Unit // canvasId를 전달하는 콜백
    onNavigateToCanvas: (String) -> Unit
) {
    var isVisibleState by remember { mutableStateOf(isVisible) }

    Box(modifier = Modifier
        .fillMaxSize()
//        .clickable { onClick(canvasId) } // 클릭 시 canvasId 전달
    ) {
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
        if (isCurrentUserCanvas) {
            Image(
                painter = painterResource(id = R.drawable.edit),
                contentDescription = "Edit",
                modifier = Modifier
                    .absoluteOffset(x = 300.dp, y = 5.dp) // 상단 우측 고정
                    .size(40.dp) // 크기 고정
                    .clickable {
                            onNavigateToCanvas(canvasId)
                    }
            )

            // Share 버튼 (Edit 버튼 오른쪽)
            Image(
                painter = painterResource(id = R.drawable.share),
                contentDescription = "Share",
                modifier = Modifier
                    .absoluteOffset(x = 345.dp, y = 5.dp) // Edit 버튼 오른쪽 고정
                    .size(40.dp) // 크기 고정
                    .border(
                        width = if (isVisibleState) 2.dp else 0.dp, // is_visible이 true면 테두리 추가
                        color = if (isVisibleState) Color.Red else Color.Transparent,
                    )
                    .clickable {
                        // is_visible 값 토글
                        isVisibleState = !isVisibleState
                        onToggleVisibility(canvasId, !isVisible)
                    }
            )
        }
    }
}
