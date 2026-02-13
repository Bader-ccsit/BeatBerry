package com.example.bb10_musicplayer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment

class CreatePlaylistFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.activity_create_playlist, container, false)

        val editName: EditText = view.findViewById(R.id.edit_playlist_name)
        val btnCreate: Button = view.findViewById(R.id.btn_create_playlist)

        btnCreate.setOnClickListener {
            val name = editName.text.toString()
            if (name.isNotEmpty()) {
                Toast.makeText(context, "Playlist '$name' created", Toast.LENGTH_SHORT).show()
                // Optionally navigate back to HomeFragment here
            } else {
                Toast.makeText(context, "Please enter a name", Toast.LENGTH_SHORT).show()
            }
        }
        return view
    }
}
