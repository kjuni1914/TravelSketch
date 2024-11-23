package com.travelsketch.ui.fragment

import MapViewScreen
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.model.LatLng
import com.travelsketch.ui.activity.MapViewActivity
import com.travelsketch.viewmodel.MapViewModel

class MapViewFragment : Fragment() {

    private val mapViewModel = MapViewModel()
    private lateinit var initialPosition: LatLng

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            initialPosition = it.getParcelable(ARG_LAT_LNG) ?: LatLng(37.7749, -122.4194)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mapViewModel.updateInitialPosition(initialPosition)
        return ComposeView(requireContext()).apply {
            setContent {
                MapViewScreen(
                    viewModel = mapViewModel,
                    onNavigateToListView = { navigateToListViewActivity() }
                )
            }
        }
    }

    private fun navigateToListViewActivity() {
        (activity as? MapViewActivity)?.navigateToListViewActivity()
    }

    companion object {
        private const val ARG_LAT_LNG = "arg_lat_lng"

        fun newInstance(latLng: LatLng): MapViewFragment {
            return MapViewFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_LAT_LNG, latLng)
                }
            }
        }
    }
}
