package com.example.bb10_musicplayer

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.Locale

class PlaylistsFragment : Fragment(), Searchable, Sortable {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: PlaylistAdapter
    private lateinit var noPlaylistsView: TextView
    private var displayedPlaylists = mutableListOf<Playlist>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_playlists, container, false)
        recyclerView = view.findViewById(R.id.playlist_list)
        noPlaylistsView = view.findViewById(R.id.no_playlists_view)

        updateDisplayedList()

        adapter = PlaylistAdapter(displayedPlaylists, { playlist ->
            (activity as? MainActivity)?.showPlaylistSongs(playlist)
        }, { playlist ->
            showPlaylistOptions(playlist)
        })

        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter

        updateUI()
        return view
    }

    private fun updateDisplayedList() {
        val allPlaylists = (activity as? MainActivity)?.getPlaylists() ?: mutableListOf()
        displayedPlaylists.clear()
        displayedPlaylists.addAll(allPlaylists)
    }

    private fun updateUI() {
        if (displayedPlaylists.isEmpty()) {
            noPlaylistsView.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            noPlaylistsView.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }

    override fun filter(query: String) {
        val allPlaylists = (activity as? MainActivity)?.getPlaylists() ?: mutableListOf()
        val lowerCaseQuery = query.toLowerCase(Locale.getDefault())
        displayedPlaylists.clear()
        
        // Always keep Favorites at the top if it matches or if search is empty
        val favorites = allPlaylists.find { it.name == "Favorites" }
        if (favorites != null && (lowerCaseQuery.isEmpty() || favorites.name.toLowerCase(Locale.getDefault()).contains(lowerCaseQuery))) {
            displayedPlaylists.add(favorites)
        }

        for (playlist in allPlaylists) {
            if (playlist.name == "Favorites") continue
            if (playlist.name.toLowerCase(Locale.getDefault()).contains(lowerCaseQuery)) {
                displayedPlaylists.add(playlist)
            }
        }
        adapter.notifyDataSetChanged()
        updateUI()
    }

    override fun sortByName(ascending: Boolean) {
        val favorites = displayedPlaylists.find { it.name == "Favorites" }
        val others = displayedPlaylists.filter { it.name != "Favorites" }.toMutableList()
        
        if (ascending) {
            others.sortBy { it.name.toLowerCase(Locale.getDefault()) }
        } else {
            others.sortByDescending { it.name.toLowerCase(Locale.getDefault()) }
        }
        
        displayedPlaylists.clear()
        favorites?.let { displayedPlaylists.add(it) }
        displayedPlaylists.addAll(others)
        adapter.notifyDataSetChanged()
    }

    override fun sortByDate(newestFirst: Boolean) {
        val favorites = displayedPlaylists.find { it.name == "Favorites" }
        val others = displayedPlaylists.filter { it.name != "Favorites" }.toMutableList()

        if (newestFirst) {
            others.sortByDescending { it.dateCreated }
        } else {
            others.sortBy { it.dateCreated }
        }

        displayedPlaylists.clear()
        favorites?.let { displayedPlaylists.add(it) }
        displayedPlaylists.addAll(others)
        adapter.notifyDataSetChanged()
    }

    private fun showPlaylistOptions(playlist: Playlist) {
        val options = if (playlist.name == "Favorites") {
            arrayOf("Rename playlist", "Details")
        } else {
            arrayOf("Remove playlist", "Rename playlist", "Details")
        }

        AlertDialog.Builder(context)
            .setTitle(playlist.name)
            .setItems(options) { _, which ->
                if (playlist.name == "Favorites") {
                    when (which) {
                        0 -> renamePlaylist(playlist)
                        1 -> showPlaylistDetails(playlist)
                    }
                } else {
                    when (which) {
                        0 -> removePlaylist(playlist)
                        1 -> renamePlaylist(playlist)
                        2 -> showPlaylistDetails(playlist)
                    }
                }
            }
            .show()
    }

    private fun removePlaylist(playlist: Playlist) {
        if (playlist.name == "Favorites") return
        (activity as? MainActivity)?.getPlaylists()?.remove(playlist)
        displayedPlaylists.remove(playlist)
        adapter.notifyDataSetChanged()
        updateUI()
        Toast.makeText(context, "Playlist removed", Toast.LENGTH_SHORT).show()
    }

    private fun renamePlaylist(playlist: Playlist) {
        val input = EditText(context)
        input.setText(playlist.name)
        AlertDialog.Builder(context)
            .setTitle("Rename Playlist")
            .setView(input)
            .setPositiveButton("OK") { _, _ ->
                val newName = input.text.toString()
                if (newName.isNotEmpty()) {
                    if (playlist.name == "Favorites") {
                        Toast.makeText(context, "Cannot rename Favorites playlist", Toast.LENGTH_SHORT).show()
                    } else {
                        playlist.name = newName
                        adapter.notifyDataSetChanged()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showPlaylistDetails(playlist: Playlist) {
        AlertDialog.Builder(context)
            .setTitle("Playlist Details")
            .setMessage("Name: ${playlist.name}\nSongs: ${playlist.songs.size}")
            .setPositiveButton("Close", null)
            .show()
    }
}
