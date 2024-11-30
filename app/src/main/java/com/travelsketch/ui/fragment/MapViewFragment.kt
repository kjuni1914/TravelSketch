package com.travelsketch.ui.fragment

import MapViewScreen
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.fragment.app.viewModels
import com.google.android.gms.maps.model.LatLng
import com.travelsketch.ui.activity.MapViewActivity
import com.travelsketch.viewmodel.MapViewModel

class MapViewFragment : Fragment() {

    private val mapViewModel: MapViewModel by viewModels()
    private lateinit var initialPosition: LatLng // 초기 위치

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 안전하게 arguments를 가져오도록 수정
        initialPosition = arguments?.getParcelable(ARG_LAT_LNG) ?: LatLng(37.5665, 126.9780)


        mapViewModel.updateInitialPosition(initialPosition)
        mapViewModel.fetchAllCanvasData() // Firebase에서 모든 캔버스 데이터 가져오기
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                MapViewScreen(
                    viewModel = mapViewModel,
                    onNavigateToListView = { navigateToListViewActivity() },
                    onNavigateToMapSetup = { navigateToMapSetupFragment() } // Fragment 전환 콜백
                )
            }
        }
    }

    private fun navigateToListViewActivity() {
        (activity as? MapViewActivity)?.navigateToListViewActivity()    }

    private fun navigateToMapSetupFragment() {
        parentFragmentManager.commit {
            replace(
                android.R.id.content, // Fragment를 대체할 컨테이너 ID
                MapSetupFragment()
            )
            addToBackStack(null) // 뒤로 가기 버튼 지원
        }
    }


    companion object {
        private const val ARG_LAT_LNG = "arg_lat_lng"

        fun newInstance(latLng: LatLng): MapViewFragment {
            return MapViewFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_LAT_LNG, latLng) // 정확한 키와 데이터 추가
                }
            }
        }
    }
}
