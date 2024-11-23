package com.travelsketch.ui.fragment

import MapSetupScreen
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.fragment.app.viewModels
import com.google.android.gms.maps.model.LatLng
import com.travelsketch.viewmodel.MapViewModel

class MapSetupFragment : Fragment() {
    private val mapViewModel: MapViewModel by viewModels()
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                MapSetupScreen(
                    mapViewModel = mapViewModel,
                    onLocationConfirmed = { latLng ->
                        navigateToMapViewFragment(latLng)
                    }
                )
            }
        }
    }

    private fun navigateToMapViewFragment(latLng: LatLng) {
        parentFragmentManager.commit {
            replace(
                android.R.id.content, // 컨테이너 ID
                MapViewFragment.newInstance(latLng)
            )
            addToBackStack(null) // 뒤로 가기 지원
        }
    }
}
