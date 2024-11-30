package com.travelsketch.ui.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.fragment.app.FragmentActivity
import com.travelsketch.ui.composable.HostMapAndMapViewFragments

class MapViewActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val initialFragment = intent.getStringExtra("FRAGMENT") ?: "MAP_VIEW"

        setContent {
            HostMapAndMapViewFragments(
                fragmentManager = supportFragmentManager,
                initialFragment = initialFragment
            )
        }
    }

    fun navigateToListViewActivity() {
        val intent = Intent(this, ListViewActivity::class.java)
        startActivity(intent)
    }
}