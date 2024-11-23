package com.travelsketch.ui.activity

import ListElementData
import ListViewScreen
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

class ListViewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ListViewScreen(
                items = listOf(
                    ListElementData("일본 여행"),
                    ListElementData("스페인 여행"),
                    ListElementData("일본 여행"),
                    ListElementData("스페인 여행"),
                    ListElementData("일본 여행"),
                    ListElementData("스페인 여행")

                ),
                onNavigateToListView = { navigateToMapViewActivity() }
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
}
