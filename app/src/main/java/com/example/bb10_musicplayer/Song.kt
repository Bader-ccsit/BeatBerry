package com.example.bb10_musicplayer

import android.net.Uri

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val path: String,
    val duration: Long,
    val albumId: Long
) {
    val uri: Uri get() = Uri.withAppendedPath(
        android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
        id.toString()
    )
}
