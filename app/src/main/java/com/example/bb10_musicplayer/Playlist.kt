package com.example.bb10_musicplayer

data class Playlist(
    val id: Long,
    var name: String,
    val songs: MutableList<Long> = mutableListOf()
)
