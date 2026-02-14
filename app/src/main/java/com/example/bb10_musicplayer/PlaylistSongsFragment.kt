package com.example.bb10_musicplayer

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.*

class PlaylistSongsFragment : Fragment(), Searchable {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: SongAdapter
    private var playlist: Playlist? = null
    private var allPlaylistSongs = mutableListOf<Song>()
    private var displayedSongs = mutableListOf<Song>()
    private var isRearrangeEnabled = false
    private var itemTouchHelper: ItemTouchHelper? = null

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
        displayedSongs.clear()
        displayedSongs.addAll(allPlaylistSongs)

        setupRearrange()

        adapter = SongAdapter(displayedSongs, { position ->
            (activity as? MainActivity)?.playSongsList(displayedSongs, position)
        }, { song ->
            (activity as? MainActivity)?.showSongOptions(song)
        }, { viewHolder ->
            itemTouchHelper?.startDrag(viewHolder)
        })

        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter

        btnAdd.setOnClickListener { showAddSongsDialog() }
        btnShuffle.setOnClickListener { shuffleSongs() }

        return view
    }

    private fun loadSongs() {
        val allSongs = (activity as? MainActivity)?.getAllSongs() ?: emptyList()
        allPlaylistSongs.clear()
        playlist?.songs?.forEach { id ->
            allSongs.find { it.id == id }?.let { allPlaylistSongs.add(it) }
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
                filter("") // Reset filter
                (activity as? MainActivity)?.savePlaylists()
            }
            .show()
    }

    private fun shuffleSongs() {
        displayedSongs.shuffle()
        adapter.updateSongs(displayedSongs)
        (activity as? MainActivity)?.playSongsList(displayedSongs, 0)
    }

    override fun filter(query: String) {
        val lowerCaseQuery = query.lowercase(Locale.getDefault())
        displayedSongs.clear()
        if (lowerCaseQuery.isEmpty()) {
            displayedSongs.addAll(allPlaylistSongs)
        } else {
            for (song in allPlaylistSongs) {
                if (song.title.lowercase(Locale.getDefault()).contains(lowerCaseQuery) ||
                    song.artist.lowercase(Locale.getDefault()).contains(lowerCaseQuery)) {
                    displayedSongs.add(song)
                }
            }
        }
        adapter.updateSongs(displayedSongs)
    }

    fun enableRearrange() {
        if (isRearrangeEnabled) {
            isRearrangeEnabled = false
            Toast.makeText(context, "Rearrange disabled. Order saved.", Toast.LENGTH_SHORT).show()
            adapter.setShowDragHandle(false)
            // Save the new order back to the playlist object
            playlist?.songs?.clear()
            allPlaylistSongs.forEach { playlist?.songs?.add(it.id) }
            (activity as? MainActivity)?.savePlaylists()
        } else {
            isRearrangeEnabled = true
            adapter.setShowDragHandle(true)
            Toast.makeText(context, "Rearrange enabled. Use handles to reorder.", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupRearrange() {
        val callback = object : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {
            override fun isLongPressDragEnabled(): Boolean = false // Disable long press drag

            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                val fromPos = viewHolder.adapterPosition
                val toPos = target.adapterPosition
                
                Collections.swap(displayedSongs, fromPos, toPos)
                // Also swap in the original list to keep consistency if no filter is active
                val song1 = displayedSongs[fromPos]
                val song2 = displayedSongs[toPos]
                val originalFrom = allPlaylistSongs.indexOf(song1)
                val originalTo = allPlaylistSongs.indexOf(song2)
                if (originalFrom != -1 && originalTo != -1) {
                    Collections.swap(allPlaylistSongs, originalFrom, originalTo)
                }
                
                adapter.notifyItemMoved(fromPos, toPos)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
        }
        itemTouchHelper = ItemTouchHelper(callback)
        itemTouchHelper?.attachToRecyclerView(recyclerView)
    }
}
