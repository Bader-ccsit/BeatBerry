package com.example.bb10_musicplayer

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class PlaylistSongsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: SongAdapter
    private var playlist: Playlist? = null
    private var playlistSongs = mutableListOf<Song>()

    companion object {
        fun newInstance(playlist: Playlist): PlaylistSongsFragment {
            val fragment = PlaylistSongsFragment()
            fragment.playlist = playlist
            return fragment
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_playlist_songs, container, false)
        recyclerView = view.findViewById(R.id.playlist_songs_list)
        val btnAdd: Button = view.findViewById(R.id.btn_add_songs)
        val btnShuffle: ImageButton = view.findViewById(R.id.btn_shuffle)

        loadSongs()

        adapter = SongAdapter(playlistSongs, { position ->
            (activity as? MainActivity)?.playSong(position)
        }, { song ->
            (activity as? MainActivity)?.showSongOptions(song)
        })

        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter

        btnAdd.setOnClickListener { showAddSongsDialog() }
        btnShuffle.setOnClickListener { shuffleSongs() }

        return view
    }

    private fun loadSongs() {
        val allSongs = (activity as? MainActivity)?.getAllSongs() ?: emptyList()
        playlistSongs.clear()
        playlist?.songs?.forEach { id ->
            allSongs.find { it.id == id }?.let { playlistSongs.add(it) }
        }
    }

    private fun showAddSongsDialog() {
        val allSongs = (activity as? MainActivity)?.getAllSongs() ?: emptyList()
        val songTitles = allSongs.map { it.title }.toTypedArray()
        val checkedItems = BooleanArray(allSongs.size) { index ->
            playlist?.songs?.contains(allSongs[index].id) ?: false
        }

        AlertDialog.Builder(context)
            .setTitle("Add Songs to Playlist")
            .setMultiChoiceItems(songTitles, checkedItems) { _, which, isChecked ->
                val songId = allSongs[which].id
                if (isChecked) {
                    if (playlist?.songs?.contains(songId) == false) {
                        playlist?.songs?.add(songId)
                    }
                } else {
                    playlist?.songs?.remove(songId)
                }
            }
            .setPositiveButton("Done") { _, _ ->
                loadSongs()
                adapter.updateSongs(playlistSongs)
            }
            .show()
    }

    private fun shuffleSongs() {
        playlistSongs.shuffle()
        adapter.updateSongs(playlistSongs)
    }
}
