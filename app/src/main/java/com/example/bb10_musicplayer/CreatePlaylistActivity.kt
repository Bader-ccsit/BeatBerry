package com.example.bb10_musicplayer

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class CreatePlaylistActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_playlist)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val editName: EditText = findViewById(R.id.edit_playlist_name)
        val btnCreate: Button = findViewById(R.id.btn_create_playlist)

        btnCreate.setOnClickListener {
            val name = editName.text.toString()
            if (name.isNotEmpty()) {
                Toast.makeText(this, "Playlist '$name' created", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "Please enter a name", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
