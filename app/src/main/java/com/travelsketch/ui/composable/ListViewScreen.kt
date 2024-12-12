
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.travelsketch.R

data class ListElementData(
    val title: String,
    val canvasId: String,
    val isVisible: Boolean
)

@Composable
fun ListViewScreen(
    items: List<ListElementData>,
    friendItems: List<ListElementData>,
    onNavigateToListView: () -> Unit,
    onNavigateToMapSetup: () -> Unit,
    onAddFriend: (String) -> Unit, // 친구 추가 콜백\
    onToggleVisibility: (String, Boolean) -> Unit, // visibility 변경 콜백 추가
    onNavigateToCanvas: (String, Boolean) -> Unit, // editable 상태 전달 추가
    onUpdateTitle: (String, String) -> Unit,
    onUpdateCoverImage: (String) -> Unit,
    onDeleteCanvas: (String) -> Unit,
    onLogout: () -> Unit // Logout callback
) {
    val customFontFamily = FontFamily(Font(R.font.waving_at_christmas))
    val titleFontFamily = FontFamily(Font(R.font.typo_crayonm))

    var showPopup by remember { mutableStateOf(false) }
    var emailInput by remember { mutableStateOf(TextFieldValue("")) }

    Box(
        modifier = Modifier
        .fillMaxSize()
        .background(Color(0xFF335577))
    ) {
        Column {


            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                // User's Canvas Section
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "My Canvas List",
                            fontSize = 40.sp,
                            fontFamily = customFontFamily,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }
                items(items) { item ->
                    ListElement(
                        title = item.title,
                        canvasId = item.canvasId,
                        isCurrentUserCanvas = true,
                        isVisible = item.isVisible,
                        onToggleVisibility = onToggleVisibility,
                        onNavigateToCanvas = { onNavigateToCanvas(item.canvasId,true) },
                        onUpdateTitle = onUpdateTitle,
                        onUpdateCoverImage = { onUpdateCoverImage(item.canvasId) },
                        onDeleteCanvas = { onDeleteCanvas(item.canvasId) }
                    )
                }

                // Friends' Canvas Section
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Friends' Canvases",
                            fontSize = 40.sp,
                            fontFamily = customFontFamily,
                            color = Color.White
                        )
                    }
                }
                items(friendItems) { item ->
                    ListElement(
                        title = item.title,
                        canvasId = item.canvasId,
                        isCurrentUserCanvas = false,
                        isVisible = true,
                        onToggleVisibility = { _, _ -> },
                        onNavigateToCanvas = { onNavigateToCanvas(item.canvasId,false) },
                        onUpdateTitle = onUpdateTitle,
                        onUpdateCoverImage = {},
                        onDeleteCanvas = {}
                    )
                }
            }
        }

        // 버튼을 화면 하단에 고정
        Button(
            onClick = { onNavigateToListView() },
            modifier = Modifier
                .absoluteOffset(y = (-32).dp) // 화면 하단에서 32dp 위로
                .align(Alignment.BottomCenter) // 하단 중앙 정렬
                .width(115.dp) // 너비 설정
                .height(48.dp) // 높이 설정
                .border(width = 0.5.dp, color = Color.Black, shape = RoundedCornerShape(25.dp)), // 테두리 추가

        colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFFFDE7)
            , // 밝은 파란색
                contentColor = Color.Black // 텍스트 색상
            )
        ) {
            Text(
                text = "Map",
                fontSize = 25.sp,
                fontFamily = titleFontFamily,
                color = Color(0xFF2196F3)
            )
        }
        FloatingActionButton(
            onClick = { onNavigateToMapSetup() },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = 40.dp),
            containerColor = Color(0xFFB3E5FC),
            contentColor = Color.White,
            shape = CircleShape
        ) {
            Text(
                text = "+",
                color = Color(0xFF001F3F),
                fontSize = 24.sp
            )
        }

        // FloatingActionButton to Add Friend
        FloatingActionButton(
            onClick = { showPopup = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 40.dp),
            containerColor = Color(0xFFB3E5FC),
            contentColor = Color.White,
            shape = CircleShape
        ) {
            Text(
                text = "\uD83D\uDC64",
                color = Color.White,
                fontSize = 20.dp.value.sp
            )
        }
        // 팝업 UI
        if (showPopup) {
            AlertDialog(
                onDismissRequest = { showPopup = false },
                title = { Text("Add Friend") },
                text = {
                    Column {
                        Text("Enter your friend's email:")
                        TextField(
                            value = emailInput,
                            onValueChange = { emailInput = it },
                            placeholder = { Text("Email") }
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        onAddFriend(emailInput.text)
                        showPopup = false
                    }) {
                        Text("Add")
                    }
                },
                dismissButton = {
                    Button(onClick = { showPopup = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
