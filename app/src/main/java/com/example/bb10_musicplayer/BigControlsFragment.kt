package com.example.bb10_musicplayer

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import java.util.*
import java.util.concurrent.TimeUnit

class BigControlsFragment : Fragment() {

    private lateinit var txtTitle: TextView
    private lateinit var txtArtist: TextView
    private lateinit var seekBar: SeekBar
    private lateinit var txtCurrentTime: TextView
    private lateinit var txtTotalDuration: TextView
    private lateinit var btnPlayPause: ImageButton
    private lateinit var btnPrev: ImageButton
    private lateinit var btnNext: ImageButton

    private val handler = Handler(Looper.getMainLooper())
    private var updateTask: Runnable? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_big_controls, container, false)

        txtTitle = view.findViewById(R.id.big_song_title)
        txtArtist = view.findViewById(R.id.big_song_artist)
        seekBar = view.findViewById(R.id.big_seek_bar)
        txtCurrentTime = view.findViewById(R.id.big_current_time)
        txtTotalDuration = view.findViewById(R.id.big_total_duration)
        btnPlayPause = view.findViewById(R.id.big_btn_play_pause)
        btnPrev = view.findViewById(R.id.big_btn_prev)
        btnNext = view.findViewById(R.id.big_btn_next)

        val mainActivity = activity as? MainActivity

        btnPlayPause.setOnClickListener {
            mainActivity?.let {
                if (it.getMusicService()?.isPng() == true) {
                    it.getMusicService()?.pausePlayer()
                } else {
                    it.getMusicService()?.go()
                }
            }
        }

        btnPrev.setOnClickListener { mainActivity?.getMusicService()?.playPrev() }
        btnNext.setOnClickListener { mainActivity?.getMusicService()?.playNext() }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    mainActivity?.getMusicService()?.seek(progress)
                    txtCurrentTime.text = formatTime(progress.toLong())
                }
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        startUpdate()
        return view
    }

    private fun startUpdate() {
        updateTask = object : Runnable {
            override fun run() {
                val mainActivity = activity as? MainActivity
                mainActivity?.getMusicService()?.let { service ->
                    val currentSong = service.getCurrentSong()
                    if (currentSong != null) {
                        txtTitle.text = currentSong.title
                        txtArtist.text = "<${currentSong.artist}>"
                        
                        val pos = service.getPosn()
                        val dur = service.getDur()
                        seekBar.max = dur
                        seekBar.progress = pos
                        txtCurrentTime.text = formatTime(pos.toLong())
                        txtTotalDuration.text = formatTime(dur.toLong())
                        
                        if (service.isPng()) {
                            btnPlayPause.setImageResource(android.R.drawable.ic_media_pause)
                        } else {
                            btnPlayPause.setImageResource(android.R.drawable.ic_media_play)
                        }
                    }
                }
                handler.postDelayed(this, 1000)
            }
        }
        handler.post(updateTask!!)
    }

    private fun formatTime(millis: Long): String {
        return String.format(Locale.getDefault(), "%02d:%02d",
            TimeUnit.MILLISECONDS.toMinutes(millis),
            TimeUnit.MILLISECONDS.toSeconds(millis) % TimeUnit.MINUTES.toSeconds(1)
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        updateTask?.let { handler.removeCallbacks(it) }
    }
}
