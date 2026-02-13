package com.example.bb10_musicplayer

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class RescanActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rescan)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val btnStartScan: Button = findViewById(R.id.btn_start_scan)
        val btnManualAdd: Button = findViewById(R.id.btn_manual_add)
        val progressBar: ProgressBar = findViewById(R.id.scan_progress)

        btnStartScan.setOnClickListener {
            progressBar.visibility = View.VISIBLE
            // In a real app, this would trigger a MediaStore scan
            // For now, we simulate a delay
            btnStartScan.postDelayed({
                progressBar.visibility = View.GONE
                Toast.makeText(this, R.string.rescan_message, Toast.LENGTH_SHORT).show()
                finish() // Go back after scanning
            }, 2000)
        }

        btnManualAdd.setOnClickListener {
            Toast.makeText(this, "Manual add clicked", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
