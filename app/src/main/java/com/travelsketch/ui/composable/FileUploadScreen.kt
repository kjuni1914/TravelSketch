package com.travelsketch.ui.composable

import android.media.MediaPlayer
import android.net.Uri
import android.widget.Toast
import android.widget.VideoView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberImagePainter
import com.travelsketch.viewmodel.FileUploadViewModel
import com.travelsketch.viewmodel.MediaItem
import kotlinx.coroutines.launch

@Composable
fun FileUploadScreen(viewModel: FileUploadViewModel = viewModel()) {
    var selectedMediaItem by remember { mutableStateOf<MediaItem?>(null) }
    val mediaList by viewModel.mediaList.collectAsState()
    val context = LocalContext.current

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            when (viewModel.fileType) {
                "image" -> viewModel.uploadFile(it, "image")
                "video" -> viewModel.uploadFile(it, "video")
                "audio" -> viewModel.uploadFile(it, "audio")
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            val uri = saveBitmapToUri(context, bitmap)
            uri?.let { viewModel.uploadFile(it, "image") }
        }
    }

    val videoUri = remember { mutableStateOf<Uri?>(null) }
    val videoCaptureLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CaptureVideo()) { success ->
        if (success) {
            videoUri.value?.let { uri ->
                viewModel.uploadFile(uri, "video")
            }
        }
    }




    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 카메라/갤러리 이동 버튼
        CameraAndGalleryButtons(
            onOpenCamera = {
                if (viewModel.fileType == "image") {
                    cameraLauncher.launch(null)
                } else if (viewModel.fileType == "video") {
                    val uri = createVideoUri(context)
                    if (uri != null) {
                        videoUri.value = uri
                        videoCaptureLauncher.launch(uri)
                    } else {
                        Toast.makeText(context, "Failed to create video file", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onOpenGallery = { galleryLauncher.launch("${viewModel.fileType}/*") },
            onFileTypeChange = { viewModel.fileType = it }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 미디어 리스트 섹션
        MediaListSection(
            mediaList = mediaList,
            selectedMedia = selectedMediaItem,
            onMediaSelected = { selectedMediaItem = it },
            onDeleteMedia = { mediaItem ->
                if (mediaItem != null) {
                    viewModel.deleteMedia(mediaItem)
                    selectedMediaItem = null
                }
            }
        )
    }
}
@Composable
fun CameraAndGalleryButtons(
    onOpenCamera: () -> Unit,
    onOpenGallery: () -> Unit,
    onFileTypeChange: (String) -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = {
                    onFileTypeChange("image")
                    onOpenCamera()
                }
            ) {
                Text("Open Camera")
            }
            Button(
                onClick = {
                    onFileTypeChange("image")
                    onOpenGallery()
                }
            ) {
                Text("Open Gallery")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = {
                    onFileTypeChange("video")
                    onOpenCamera()
                }
            ) {
                Text("Capture Video")
            }
            Button(
                onClick = {
                    onFileTypeChange("video")
                    onOpenGallery()
                }
            ) {
                Text("Select Video")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = {
                    onFileTypeChange("audio")
                    onOpenGallery()
                }
            ) {
                Text("Select Audio")
            }
        }
    }
}


@Composable
fun MediaListSection(
    mediaList: List<MediaItem>,
    selectedMedia: MediaItem?,
    onMediaSelected: (MediaItem) -> Unit,
    onDeleteMedia: (MediaItem?) -> Unit
) {
    LazyColumn {
        items(mediaList) { mediaItem ->
            MediaItemRow(
                mediaItem = mediaItem,
                isSelected = selectedMedia == mediaItem,
                onMediaClicked = { onMediaSelected(mediaItem) },
                onDeleteClicked = { onDeleteMedia(mediaItem) }
            )
        }
    }
}

@Composable
fun MediaItemRow(
    mediaItem: MediaItem,
    isSelected: Boolean,
    onMediaClicked: () -> Unit,
    onDeleteClicked: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // 미디어 썸네일 또는 미디어 플레이어
        when (mediaItem.type) {
            "image" -> Image(
                painter = rememberImagePainter(data = mediaItem.url),
                contentDescription = null,
                modifier = Modifier.size(100.dp),
                contentScale = ContentScale.Crop
            )
            "video" -> VideosPlayer(mediaItem.url)
            "audio" -> AudioPlayer(mediaItem.url)
        }

        Spacer(modifier = Modifier.width(8.dp))

        // 선택 상태
        if (isSelected) {
            Text("Selected")
        }

        Spacer(modifier = Modifier.width(8.dp))

        // 삭제 버튼
        Button(onClick = { onDeleteClicked() }) {
            Text("Delete")
        }
    }
}

@Composable
fun VideosPlayer(url: String) {
    AndroidView(
        factory = { context ->
            VideoView(context).apply {
                setVideoURI(Uri.parse(url))
                start()
            }
        },
        modifier = Modifier.size(100.dp)
    )
}

@Composable
fun AudioPlayer(url: String) {
    val context = LocalContext.current
    val mediaPlayer = remember { MediaPlayer() }
    val coroutineScope = rememberCoroutineScope()
    var isPlaying by remember { mutableStateOf(false) }

    Row {
        Button(
            onClick = {
                coroutineScope.launch {
                    if (isPlaying) {
                        mediaPlayer.stop()
                        isPlaying = false
                    } else {
                        mediaPlayer.reset()
                        mediaPlayer.setDataSource(context, Uri.parse(url))
                        mediaPlayer.prepare()
                        mediaPlayer.start()
                        isPlaying = true
                    }
                }
            }
        ) {
            Text(if (isPlaying) "Stop" else "Play")
        }
    }
}
