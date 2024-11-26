package com.travelsketch.ui.composable

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.commit
import com.travelsketch.ui.fragment.MapSetupFragment
import com.travelsketch.ui.fragment.MapViewFragment

@Composable
fun HostMapAndMapViewFragments(
    fragmentManager: FragmentManager,
    initialFragment: String
) {
    val mapSetupFragmentTag = "MapSetupFragment"
    val mapViewFragmentTag = "MapViewFragment"

    val fragment = remember {
        when (initialFragment) {
            "MAP_VIEW" -> fragmentManager.findFragmentByTag(mapViewFragmentTag)
                ?: MapViewFragment()
            else -> fragmentManager.findFragmentByTag(mapSetupFragmentTag)
                ?: MapSetupFragment()
        }
    }

    AndroidView(factory = { context ->
        val container = android.widget.FrameLayout(context).apply {
            id = android.view.View.generateViewId() // 동적 ID 생성
        }
        fragmentManager.commit {
            replace(container.id, fragment, if (initialFragment == "MAP_VIEW") mapViewFragmentTag else mapSetupFragmentTag)
        }
        container
    })
}

