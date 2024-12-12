package com.travelsketch.ui.activity

import ListElementData
import ListViewScreen
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.travelsketch.viewmodel.ListViewModel

class ListViewActivity : ComponentActivity() {

    private val viewModel: ListViewModel by viewModels()
    private lateinit var imagePickerLauncher: androidx.activity.result.ActivityResultLauncher<Intent>
    private var selectedCanvasId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 사용자 ID 가져오기
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Log.d("ListViewActivity", "사용자가 로그인되어 있지 않습니다.")
            finish()
            return
        }

        imagePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val data: Intent? = result.data
                val uri: Uri? = data?.data
                if (uri != null) {
                    uploadImageToFirebase(selectedCanvasId, uri)
                }
            }
        }
        viewModel.readUserMapCanvasData(userId)
        viewModel.readFriendMapCanvasData(userId) // 친구 데이터 가져오기 추가

        setContent {
            val canvasList by viewModel.canvasList.collectAsState() // 전체 Canvas 데이터 관찰
            val friendCanvasList by viewModel.friendCanvasDataList.collectAsState() // 친구 데이터 관찰

            val items = canvasList.map { ListElementData(it.title, it.canvasId, it.is_visible) }
            val friendItems = friendCanvasList.map { ListElementData(it.title, it.canvasId, it.is_visible) }

            ListViewScreen(
                items = items,
                friendItems = friendItems,
                onNavigateToListView = { navigateToMapViewActivity() },
                onNavigateToMapSetup = { navigateToMapSetupActivity() }, // 새로운 콜백
                onAddFriend = { email ->
                    viewModel.addFriendByEmail(userId, email) // 친구 추가 처리
                },
                onToggleVisibility = { canvasId, newVisibility ->
                    viewModel.toggleCanvasVisibility(canvasId, newVisibility,userId)
                },
                onNavigateToCanvas = { canvasId -> navigateToCanvas(canvasId) },
                onUpdateTitle = { canvasId, newTitle ->
                    viewModel.updateCanvasTitle(canvasId, newTitle) // 제목 업데이트 처리
                },
                onUpdateCoverImage = { canvasId ->
                    launchImagePicker(canvasId)
                },
                onDeleteCanvas = { canvasId ->
                    viewModel.deleteCanvas(canvasId)
                },
                onLogout = {
                    viewModel.userLogout()
                    finish()
                }
            )
        }
    }

    private fun launchImagePicker(canvasId: String) {
        selectedCanvasId = canvasId
        val intent = Intent(Intent.ACTION_PICK).apply {
            type = "image/*"
        }
        imagePickerLauncher.launch(intent)
    }


    private fun uploadImageToFirebase(canvasId: String, imageUri: Uri) {
        // .jpg 확장자로 파일 이름 생성
        val fileName = "${System.currentTimeMillis()}.jpg"
        val storageRef = FirebaseStorage.getInstance().reference.child("media/images/$fileName")

        // Storage에 파일 업로드
        storageRef.putFile(imageUri).addOnSuccessListener {
            storageRef.downloadUrl.addOnSuccessListener { _ ->
                // Firebase Database 업데이트: 확장자를 제외한 파일 이름 저장
                val imageNameWithoutExtension = fileName.substringBeforeLast(".")
                viewModel.updatePreviewImage(canvasId, imageNameWithoutExtension)
            }.addOnFailureListener { exception ->
                Log.e("ListViewActivity", "Failed to get download URL", exception)
            }
        }.addOnFailureListener { exception ->
            Log.e("ListViewActivity", "Failed to upload image", exception)
        }
    }


    fun navigateToMapViewActivity() {
        // MapViewActivity로 전환하며 MapViewFragment 요청
        val intent = Intent(this, MapViewActivity::class.java).apply {
            putExtra("FRAGMENT", "MAP_VIEW") // MapViewFragment 요청
        }
        startActivity(intent)
    }

    fun navigateToMapSetupActivity() {
        val intent = Intent(this, MapViewActivity::class.java).apply {
            putExtra("FRAGMENT", "MAP_SETUP") // MapSetupFragment 요청
        }
        startActivity(intent)
    }

    private fun navigateToCanvas(canvasId: String) {
        val intent = Intent(this, CanvasActivity::class.java).apply {
            putExtra("CANVAS_ID", canvasId)
        }
        startActivity(intent)
    }

    companion object {
        private const val REQUEST_IMAGE_PICK = 1
    }
}
