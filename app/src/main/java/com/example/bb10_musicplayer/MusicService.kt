package com.example.bb10_musicplayer

import android.app.Service
import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.media.AudioAttributes
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat

class MusicService : Service(), MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener {

    private var player: MediaPlayer? = null
    private var songs: List<Song>? = null
    private var songPos = 0
    private val musicBind = MusicBinder()
    private var mediaSession: MediaSessionCompat? = null

    override fun onCreate() {
        super.onCreate()
        songPos = 0
        initMusicPlayer()
        
        mediaSession = MediaSessionCompat(this, "MusicService").apply {
            setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() { go() }
                override fun onPause() { pausePlayer() }
                override fun onSkipToNext() { playNext() }
                override fun onSkipToPrevious() { playPrev() }
                override fun onSeekTo(pos: Long) { seek(pos.toInt()) }
            })
            isActive = true
        }
    }

    private fun initMusicPlayer() {
        player = MediaPlayer()
        player?.apply {
            setWakeMode(applicationContext, PowerManager.PARTIAL_WAKE_LOCK)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
            } else {
                @Suppress("DEPRECATION")
                setAudioStreamType(AudioManager.STREAM_MUSIC)
            }
            setOnPreparedListener(this@MusicService)
            setOnCompletionListener(this@MusicService)
            setOnErrorListener(this@MusicService)
        }
    }

    fun setList(theSongs: List<Song>) {
        songs = theSongs
    }

    inner class MusicBinder : Binder() {
        fun getService(): MusicService = this@MusicService
    }

    override fun onBind(intent: Intent): IBinder {
        return musicBind
    }

    override fun onUnbind(intent: Intent): Boolean {
        player?.stop()
        player?.release()
        mediaSession?.release()
        return false
    }

    fun playSong() {
        player?.reset()
        val playSong = songs?.getOrNull(songPos) ?: return
        
        try {
            player?.setDataSource(applicationContext, playSong.uri)
            updateMetadata(playSong)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        player?.prepareAsync()
        updatePlaybackState(PlaybackStateCompat.STATE_BUFFERING)
    }

    private fun updateMetadata(song: Song) {
        val metadata = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, song.title)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, song.artist)
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, song.duration)
            .build()
        mediaSession?.setMetadata(metadata)
    }

    fun setSong(songIndex: Int) {
        songPos = songIndex
    }

    override fun onPrepared(mp: MediaPlayer) {
        mp.start()
        updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)
    }

    override fun onCompletion(mp: MediaPlayer) {
        if (player!!.currentPosition > 0) {
            mp.reset()
            playNext()
        }
    }

    override fun onError(mp: MediaPlayer, what: Int, extra: Int): Boolean {
        mp.reset()
        updatePlaybackState(PlaybackStateCompat.STATE_ERROR)
        return false
    }

    fun getPosn(): Int = player?.currentPosition ?: 0
    fun getDur(): Int = player?.duration ?: 0
    fun isPng(): Boolean = player?.isPlaying ?: false
    
    fun pausePlayer() { 
        player?.pause()
        updatePlaybackState(PlaybackStateCompat.STATE_PAUSED)
    }
    
    fun seek(posn: Int) { player?.seekTo(posn) }
    
    fun go() { 
        player?.start()
        updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)
    }
    
    fun playPrev() {
        songPos--
        if (songPos < 0) songPos = (songs?.size ?: 1) - 1
        playSong()
    }

    fun playNext() {
        songPos++
        if (songPos >= (songs?.size ?: 0)) songPos = 0
        playSong()
    }

    private fun updatePlaybackState(state: Int) {
        val playbackState = PlaybackStateCompat.Builder()
            .setActions(
                PlaybackStateCompat.ACTION_PLAY or 
                PlaybackStateCompat.ACTION_PAUSE or 
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT or 
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                PlaybackStateCompat.ACTION_SEEK_TO
            )
            .setState(state, getPosn().toLong(), 1.0f)
            .build()
        mediaSession?.setPlaybackState(playbackState)
    }
}
