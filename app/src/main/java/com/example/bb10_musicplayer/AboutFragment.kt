package com.example.bb10_musicplayer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.SpannableString
import android.text.style.UnderlineSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment

class AboutFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.activity_about, container, false)
        
        val githubLink: TextView = view.findViewById(R.id.about_github_link)
        val content = SpannableString(getString(R.string.about_github))
        content.setSpan(UnderlineSpan(), 0, content.length, 0)
        githubLink.text = content
        
        githubLink.setOnClickListener {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Bader-ccsit"))
            startContextIntent(browserIntent)
        }
        
        return view
    }

    private fun startContextIntent(intent: Intent) {
        try {
            startActivity(intent)
        } catch (e: Exception) {
            // Handle cases where no browser is installed
        }
    }
}
