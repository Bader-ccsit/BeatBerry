package com.example.bb10_musicplayer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment

class RescanFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.activity_rescan, container, false)

        val btnStartScan: Button = view.findViewById(R.id.btn_start_scan)
        val btnManualAdd: Button = view.findViewById(R.id.btn_manual_add)
        val progressBar: ProgressBar = view.findViewById(R.id.scan_progress)

        btnStartScan.setOnClickListener {
            progressBar.visibility = View.VISIBLE
            btnStartScan.postDelayed({
                progressBar.visibility = View.GONE
                Toast.makeText(context, R.string.rescan_message, Toast.LENGTH_SHORT).show()
                // In a real app, you'd trigger the scan in MainActivity or a ViewModel
            }, 2000)
        }

        btnManualAdd.setOnClickListener {
            Toast.makeText(context, "Manual add clicked", Toast.LENGTH_SHORT).show()
        }
        return view
    }
}
