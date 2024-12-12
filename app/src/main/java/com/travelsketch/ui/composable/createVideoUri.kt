package com.travelsketch.ui.composable

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore

fun createVideoUri(context: Context): Uri? {
    val contentResolver = context.contentResolver
    val videoCollection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

    val videoDetails = ContentValues().apply {
        put(MediaStore.Video.Media.DISPLAY_NAME, "video_${System.currentTimeMillis()}.mp4")
        put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
    }

    return contentResolver.insert(videoCollection, videoDetails)
}
