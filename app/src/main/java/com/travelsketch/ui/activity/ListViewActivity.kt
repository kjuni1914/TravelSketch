package com.travelsketch.ui.activity

import ListElementData
import ListViewScreen
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.google.firebase.auth.FirebaseAuth
import com.travelsketch.viewmodel.ListViewModel

class ListViewActivity : ComponentActivity() {

    private val viewModel: ListViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 사용자 ID 가져오기
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Log.d("ListViewActivity", "사용자가 로그인되어 있지 않습니다.")
            finish()
            return
        }

        viewModel.readUserMapCanvasData(userId)
        viewModel.readFriendMapCanvasData(userId) // 친구 데이터 가져오기 추가

        setContent {
            val canvasList by viewModel.canvasList.collectAsState() // 전체 Canvas 데이터 관찰
            val friendCanvasList by viewModel.friendCanvasDataList.collectAsState() // 친구 데이터 관찰

            val items = canvasList.map { ListElementData(it.title, it.canvasId, it.is_visible) }
            val friendItems = friendCanvasList.map { ListElementData(it.title, it.canvasId, it.is_visible) }

            ListViewScreen(
                items = items,
                friendItems = friendItems,
                onNavigateToListView = { navigateToMapViewActivity() },
                onNavigateToMapSetup = { navigateToMapSetupActivity() }, // 새로운 콜백
                onAddFriend = { email ->
                    viewModel.addFriendByEmail(userId, email) // 친구 추가 처리
                },
                onToggleVisibility = { canvasId, newVisibility ->
                    viewModel.toggleCanvasVisibility(canvasId, newVisibility,userId)
                },
//                onElementClick = { canvasId -> navigateToCanvasTestActivity(canvasId) } // 클릭 이벤트 처리
                onNavigateToCanvas = { canvasId -> navigateToCanvas(canvasId) }

            )
        }

    }

    fun navigateToMapViewActivity() {
        // MapViewActivity로 전환하며 MapViewFragment 요청
        val intent = Intent(this, MapViewActivity::class.java).apply {
            putExtra("FRAGMENT", "MAP_VIEW") // MapViewFragment 요청
        }
        startActivity(intent)
    }

    fun navigateToMapSetupActivity() {
        val intent = Intent(this, MapViewActivity::class.java).apply {
            putExtra("FRAGMENT", "MAP_SETUP") // MapSetupFragment 요청
        }
        startActivity(intent)
    }

    private fun navigateToCanvas(canvasId: String) {
        val intent = Intent(this, CanvasActivity::class.java).apply {
            putExtra("CANVAS_ID", canvasId)
        }
        startActivity(intent)
    }

//    canvas activity에 canvas id 전달
//    private fun navigateToCanvasTestActivity(canvasId: String) {
//        val intent = Intent(this, CanvasTestActivity::class.java).apply {
//            putExtra("CANVAS_ID", canvasId) // canvasId 전달
//        }
//        startActivity(intent)
//    }
}
