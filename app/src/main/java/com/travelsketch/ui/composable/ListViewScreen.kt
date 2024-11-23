
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

data class ListElementData(
    val title: String,
)

@Composable
fun ListViewScreen(
    items: List<ListElementData>,
    onNavigateToListView: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // 리스트는 상단부터 시작
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
        ) {
            items(items) { item ->
                ListElement(
                    title = item.title
                )
            }
        }

        // 버튼을 화면 하단에 고정
        Button(
            onClick = { onNavigateToListView() },
            modifier = Modifier
                .absoluteOffset(y = (-32).dp) // 화면 하단에서 32dp 위로
                .align(Alignment.BottomCenter) // 하단 중앙 정렬
                .width(115.dp) // 너비 설정
                .height(48.dp), // 높이 설정
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFD6D6D6), // 밝은 회색 배경
                contentColor = Color.Black // 텍스트 색상
            )
        ) {
            Text(text = "Map View")
        }
    }
}
