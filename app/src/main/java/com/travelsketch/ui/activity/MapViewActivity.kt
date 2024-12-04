package com.travelsketch.ui.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.fragment.app.FragmentActivity
import com.google.firebase.auth.FirebaseAuth
import com.travelsketch.ui.composable.HostMapAndMapViewFragments

class MapViewActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val initialFragment = intent.getStringExtra("FRAGMENT") ?: "MAP_VIEW"
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            // 로그인되지 않은 경우 처리
            Log.d("MapViewActivity", "사용자가 로그인되어 있지 않습니다.")
            finish() // 액티비티 종료
        }

        setContent {
            HostMapAndMapViewFragments(
                fragmentManager = supportFragmentManager,
                initialFragment = initialFragment,
                userId = userId!! // 사용자 ID 전달
            )
        }
    }

    fun navigateToListViewActivity() {
        val intent = Intent(this, ListViewActivity::class.java)
        startActivity(intent)
    }
}