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

class PlaylistsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: PlaylistAdapter
    private lateinit var noPlaylistsView: TextView
    private var playlists = mutableListOf<Playlist>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_playlists, container, false)
        recyclerView = view.findViewById(R.id.playlist_list)
        noPlaylistsView = view.findViewById(R.id.no_playlists_view)

        // Mock data for now, in a real app this would come from a database or shared preferences
        if (playlists.isEmpty()) {
            playlists.add(Playlist(1, "Hello", mutableListOf()))
            playlists.add(Playlist(2, "Favorites", mutableListOf()))
        }

        adapter = PlaylistAdapter(playlists, { playlist ->
            // Click to enter playlist
            (activity as? MainActivity)?.showPlaylistSongs(playlist)
        }, { playlist ->
            // Long click for options
            showPlaylistOptions(playlist)
        })

        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter

        updateUI()
        return view
    }

    private fun updateUI() {
        if (playlists.isEmpty()) {
            noPlaylistsView.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            noPlaylistsView.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }

    private fun showPlaylistOptions(playlist: Playlist) {
        val options = arrayOf("Remove playlist", "Rename playlist", "Details")
        AlertDialog.Builder(context)
            .setTitle(playlist.name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> removePlaylist(playlist)
                    1 -> renamePlaylist(playlist)
                    2 -> showPlaylistDetails(playlist)
                }
            }
            .show()
    }

    private fun removePlaylist(playlist: Playlist) {
        playlists.remove(playlist)
        adapter.updatePlaylists(playlists)
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
                    playlist.name = newName
                    adapter.notifyDataSetChanged()
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
