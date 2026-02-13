package com.example.bb10_musicplayer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class HomeFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: SongAdapter
    private lateinit var emptyView: TextView
    private var songs: List<Song> = emptyList()
    private var onSongClick: ((Int) -> Unit)? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        recyclerView = view.findViewById(R.id.song_list)
        emptyView = view.findViewById(R.id.empty_view)
        
        recyclerView.layoutManager = LinearLayoutManager(context)
        adapter = SongAdapter(songs, { position -> 
            onSongClick?.invoke(position) 
        }, { song ->
            (activity as? MainActivity)?.showSongOptions(song)
        })
        recyclerView.adapter = adapter
        
        updateUI()
        return view
    }

    fun setSongs(newSongs: List<Song>, clickListener: (Int) -> Unit) {
        songs = newSongs
        onSongClick = clickListener
        if (::adapter.isInitialized) {
            adapter.updateSongs(songs)
            updateUI()
        }
    }

    private fun updateUI() {
        if (::emptyView.isInitialized) {
            emptyView.visibility = if (songs.isEmpty()) View.VISIBLE else View.GONE
        }
    }
}
