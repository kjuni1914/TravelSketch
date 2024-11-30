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
import com.travelsketch.viewmodel.ListViewModel

class ListViewActivity : ComponentActivity() {

    private val viewModel: ListViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.readAllMapCanvasData()

        setContent {
            val canvasList by viewModel.canvasList.collectAsState() // 전체 Canvas 데이터 관찰

            val items = canvasList.map { ListElementData(it.title, it.canvasId) }

            ListViewScreen(
                items = items,
                onNavigateToListView = { navigateToMapViewActivity() },
                onNavigateToMapSetup = { navigateToMapSetupActivity() }, // 새로운 콜백
//                onElementClick = { canvasId -> navigateToCanvasTestActivity(canvasId) } // 클릭 이벤트 처리
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

    // canvas activity에 canvas id 전달
//    private fun navigateToCanvasTestActivity(canvasId: String) {
//        val intent = Intent(this, CanvasTestActivity::class.java).apply {
//            putExtra("CANVAS_ID", canvasId) // canvasId 전달
//        }
//        startActivity(intent)
//    }
}
