package com.example.bb10_musicplayer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.Locale

class HomeFragment : Fragment(), Searchable, Sortable {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: SongAdapter
    private lateinit var emptyView: TextView
    private var allSongs: List<Song> = emptyList()
    private var displayedSongs: MutableList<Song> = mutableListOf()
    private var onSongClick: ((Int) -> Unit)? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        recyclerView = view.findViewById(R.id.song_list)
        emptyView = view.findViewById(R.id.empty_view)
        
        recyclerView.layoutManager = LinearLayoutManager(context)
        adapter = SongAdapter(displayedSongs, { position -> 
            val song = displayedSongs[position]
            val originalIndex = allSongs.indexOf(song)
            if (originalIndex != -1) {
                onSongClick?.invoke(originalIndex)
            }
        }, { song ->
            (activity as? MainActivity)?.showSongOptions(song)
        })
        recyclerView.adapter = adapter
        
        updateUI()
        return view
    }

    fun setSongs(newSongs: List<Song>, clickListener: (Int) -> Unit) {
        allSongs = newSongs
        displayedSongs.clear()
        displayedSongs.addAll(allSongs)
        onSongClick = clickListener
        if (::adapter.isInitialized) {
            adapter.updateSongs(displayedSongs)
            updateUI()
        }
    }

    override fun filter(query: String) {
        val lowerCaseQuery = query.toLowerCase(Locale.getDefault())
        displayedSongs.clear()
        if (lowerCaseQuery.isEmpty()) {
            displayedSongs.addAll(allSongs)
        } else {
            for (song in allSongs) {
                if (song.title.toLowerCase(Locale.getDefault()).contains(lowerCaseQuery) ||
                    song.artist.toLowerCase(Locale.getDefault()).contains(lowerCaseQuery)) {
                    displayedSongs.add(song)
                }
            }
        }
        adapter.updateSongs(displayedSongs)
        updateUI()
    }

    override fun sortByName(ascending: Boolean) {
        if (ascending) {
            displayedSongs.sortBy { it.title.toLowerCase(Locale.getDefault()) }
        } else {
            displayedSongs.sortByDescending { it.title.toLowerCase(Locale.getDefault()) }
        }
        adapter.updateSongs(displayedSongs)
    }

    override fun sortByDate(newestFirst: Boolean) {
        if (newestFirst) {
            displayedSongs.sortByDescending { it.dateAdded }
        } else {
            displayedSongs.sortBy { it.dateAdded }
        }
        adapter.updateSongs(displayedSongs)
    }

    private fun updateUI() {
        if (::emptyView.isInitialized) {
            emptyView.visibility = if (displayedSongs.isEmpty()) View.VISIBLE else View.GONE
        }
    }
}
