package com.travelsketch.ui.fragment

import MapSetupScreen
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresExtension
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.gms.maps.model.LatLng
import com.travelsketch.ui.activity.CanvasActivity
import com.travelsketch.viewmodel.MapViewModel

class MapSetupFragment : Fragment() {
    private val mapViewModel: MapViewModel by viewModels()

    @RequiresExtension(extension = Build.VERSION_CODES.S, version = 7)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                MapSetupScreen(
                    mapViewModel = mapViewModel,
                    onLocationConfirmed = { latLng, canvasId, isEditable ->
                        navigateToCanvasActivity(canvasId, isEditable)
                    }
                )
            }
        }
    }

    private fun navigateToCanvasActivity(canvasId: String, isEditable: Boolean) {
        val intent = Intent(requireContext(), CanvasActivity::class.java).apply {
            putExtra("CANVAS_ID", canvasId)
            putExtra("EDITABLE", isEditable)
        }
        startActivity(intent)
    }
}
