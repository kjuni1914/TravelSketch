import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
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
    onNavigateToCanvas: (String) -> Unit,
    onUpdateTitle: (String, String) -> Unit, // 제목 업데이트 콜백 추가
    onDeleteCanvas: (String) -> Unit,
    onUpdateCoverImage: (String) -> Unit, // Set Cover Image 콜백 추가
) {
    var isVisibleState by remember { mutableStateOf(isVisible) }
    var showDialog by remember { mutableStateOf(false) }
    var newTitle by remember { mutableStateOf(title) }
    val TitleFontFamily = FontFamily(
        Font(R.font.typo_crayonm) // 파일 이름은 확장자 없이 사용
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // 카드 배경
        Card(
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp) // 고정 높이 설정
                .padding(10.dp)
                .clickable { onNavigateToCanvas(canvasId) }, // Card 클릭 이벤트
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF9C4)), // 약간 더 진한 배경색
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp) // 그림자 효과 추가
        ) {}

        // 텍스트와 버튼 배치
        Box(modifier = Modifier.fillMaxSize()) {
            // 제목 텍스트 (상단 고정, 테두리와 배경 포함)
            Box(
                modifier = Modifier
                    .absoluteOffset(x = 20.dp, y = 5.dp) // 상단 왼쪽 고정
                    .background(color = Color.White, shape = RoundedCornerShape(5.dp))
                    .border(width = 0.5.dp, color = Color.Black, shape = RoundedCornerShape(5.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp) // 텍스트 주변 패딩
            ) {
                Text(
                    text = title,
                    style = TextStyle(
                        fontSize = 20.sp,
                        color = Color.Black,
                        fontFamily = TitleFontFamily,
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
                        showDialog = true // 다이얼로그 표시
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
                        width = if (isVisibleState) 2.dp else 0.dp,
                        color = if (isVisibleState) Color.Red else Color.Transparent,
                    )
                    .clickable {
                        // is_visible 값 토글
                        isVisibleState = !isVisibleState
                        onToggleVisibility(canvasId, !isVisible)
                    }
            )
        }

        // 다이얼로그
        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = {
                    Text(text = "Edit Options")
                },
                text = {
                    Column {
                        var isEditingTitle by remember { mutableStateOf(false) }

                        if (isEditingTitle) {
                            TextField(
                                value = newTitle,
                                onValueChange = { newTitle = it },
                                label = { Text("New Title") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            Text(
                                text = "1. Edit Title",
                                modifier = Modifier.clickable {
                                    isEditingTitle = true
                                }
                            )
                        }

                        Text(text = "2. Set Cover Image",
                            modifier = Modifier.clickable {
                                onUpdateCoverImage(canvasId)
                                showDialog = false
                            }
                        )
                        Text(text = "3. Delete Canvas",
                            modifier = Modifier.clickable {
                                // Canvas 삭제
                                onDeleteCanvas(canvasId)
                                showDialog = false
                            })
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (newTitle.isNotBlank()) {
                            onUpdateTitle(canvasId, newTitle)
                            showDialog = false
                        }
                    }) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}