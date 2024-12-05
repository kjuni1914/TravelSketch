package com.travelsketch.ui.fragment

import MapViewScreen
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresExtension
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
        val userId = arguments?.getString(ARG_USER_ID)
        if (userId.isNullOrEmpty()) {
            Log.e("MapViewFragment", "사용자 ID가 제공되지 않았습니다.")
            return
        }
        initialPosition = arguments?.getParcelable(ARG_LAT_LNG) ?: LatLng(37.5665, 126.9780)
        mapViewModel.updateInitialPosition(initialPosition)
        mapViewModel.readUserMapCanvasData(userId) // 사용자 관련 Canvas 데이터 로드
    }

    @RequiresExtension(extension = Build.VERSION_CODES.S, version = 7)
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
        private const val ARG_USER_ID = "arg_user_id"

        fun newInstance(userId: String): MapViewFragment {
            return MapViewFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_USER_ID, userId) // 사용자 ID 전달
                }
            }
        }
    }
}
