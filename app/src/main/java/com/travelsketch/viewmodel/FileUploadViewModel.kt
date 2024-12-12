package com.travelsketch.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.travelsketch.data.dao.FirebaseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class MediaItem(val path: String, val type: String, val url: String)

class FileUploadViewModel : ViewModel() {

    private val repository = FirebaseRepository()

    private val _mediaList = MutableStateFlow<List<MediaItem>>(emptyList())
    val mediaList: StateFlow<List<MediaItem>> = _mediaList

    private val _uploadState = MutableStateFlow<String?>(null)
    val uploadState: StateFlow<String?> = _uploadState

    var fileType = "image" // 현재 처리 중인 파일 유형 (image, video, audio)

    fun uploadFile(fileUri: Uri, fileType: String) {
        viewModelScope.launch {
            val filePath = when (fileType) {
                "image" -> "media/images/${fileUri.lastPathSegment ?: "image_${System.currentTimeMillis()}.jpg"}"
                "video" -> "media/videos/${fileUri.lastPathSegment ?: "video_${System.currentTimeMillis()}.mp4"}"
                "audio" -> "media/audio/${fileUri.lastPathSegment ?: "audio_${System.currentTimeMillis()}.mp3"}"
                else -> throw IllegalArgumentException("Unsupported file type")
            }
            val downloadUrl = repository.uploadFile(fileUri, filePath)
            if (downloadUrl != null) {
                val newItem = MediaItem(path = filePath, type = fileType, url = downloadUrl)
                _mediaList.value = _mediaList.value + newItem
            }
        }
    }


    // 파일 삭제
    fun deleteMedia(mediaItem: MediaItem) {
        viewModelScope.launch {
            val success = repository.deleteFile(mediaItem.path)
            if (success) {
                _mediaList.value = _mediaList.value.filter { it != mediaItem } // 삭제된 미디어 제거
            }
        }
    }
}
