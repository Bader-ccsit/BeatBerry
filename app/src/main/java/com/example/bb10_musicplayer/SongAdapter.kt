package com.example.bb10_musicplayer

import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SongAdapter(
    private var songs: List<Song>,
    private val onSongClick: (Int) -> Unit,
    private val onSongLongClick: ((Song) -> Unit)? = null,
    private val startDragListener: ((RecyclerView.ViewHolder) -> Unit)? = null
) : RecyclerView.Adapter<SongAdapter.SongViewHolder>() {

    private var showDragHandle = false

    class SongViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.song_title)
        val artist: TextView = view.findViewById(R.id.song_artist)
        val dragHandle: ImageView = view.findViewById(R.id.drag_handle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.song_item, parent, false)
        return SongViewHolder(view)
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        val song = songs[position]
        holder.title.text = song.title
        holder.artist.text = song.artist
        
        holder.dragHandle.visibility = if (showDragHandle) View.VISIBLE else View.GONE
        
        holder.dragHandle.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                startDragListener?.invoke(holder)
            }
            false
        }

        holder.itemView.setOnClickListener { onSongClick(position) }
        holder.itemView.setOnLongClickListener {
            onSongLongClick?.invoke(song)
            true
        }
    }

    override fun getItemCount(): Int = songs.size

    fun updateSongs(newSongs: List<Song>) {
        songs = newSongs
        notifyDataSetChanged()
    }

    fun setShowDragHandle(show: Boolean) {
        showDragHandle = show
        notifyDataSetChanged()
    }
}
