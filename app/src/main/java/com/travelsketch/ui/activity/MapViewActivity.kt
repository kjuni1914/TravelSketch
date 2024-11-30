package com.travelsketch.ui.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import com.travelsketch.ui.composable.HostMapAndMapViewFragments

class MapViewActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Intent를 통해 로드할 Fragment 결정
        val initialFragment = intent.getStringExtra("FRAGMENT") ?: "MAP_VIEW"

        setContent {
            HostMapAndMapViewFragments(
                fragmentManager = supportFragmentManager,
                initialFragment = initialFragment // 초기 Fragment 전달
            )
        }
    }

    fun navigateToListViewActivity() {
        val intent = Intent(this, ListViewActivity::class.java)
        startActivity(intent)
    }
}
