package com.example.bb10_musicplayer

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.ComponentName
import android.content.Context
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
import androidx.core.app.NotificationCompat
import androidx.media.session.MediaButtonReceiver

class MusicService : Service(), MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener, AudioManager.OnAudioFocusChangeListener {

    private var player: MediaPlayer? = null
    private var songs: List<Song>? = null
    private var songPos = 0
    private val musicBind = MusicBinder()
    private var mediaSession: MediaSessionCompat? = null
    private lateinit var audioManager: AudioManager
    private lateinit var mediaButtonReceiverComponent: ComponentName

    override fun onCreate() {
        super.onCreate()
        songPos = 0
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        mediaButtonReceiverComponent = ComponentName(this, MediaButtonReceiver::class.java)
        
        initMusicPlayer()
        initMediaSession()
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

    private fun initMediaSession() {
        try {
            mediaSession = MediaSessionCompat(this, "BeatBerryService", mediaButtonReceiverComponent, null).apply {
                @Suppress("DEPRECATION")
                setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)
                
                val mediaButtonIntent = Intent(Intent.ACTION_MEDIA_BUTTON)
                mediaButtonIntent.component = mediaButtonReceiverComponent
                val buttonPendingIntent = PendingIntent.getBroadcast(this@MusicService, 0, mediaButtonIntent, 0)
                setMediaButtonReceiver(buttonPendingIntent)

                setCallback(object : MediaSessionCompat.Callback() {
                    override fun onPlay() { go() }
                    override fun onPause() { pausePlayer() }
                    override fun onSkipToNext() { playNext() }
                    override fun onSkipToPrevious() { playPrev() }
                    override fun onSeekTo(pos: Long) { seek(pos.toInt()) }
                    override fun onStop() { pausePlayer() }
                })
                isActive = true
            }
            
            @Suppress("DEPRECATION")
            audioManager.registerMediaButtonEventReceiver(mediaButtonReceiverComponent)
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        MediaButtonReceiver.handleIntent(mediaSession, intent)
        return START_STICKY
    }

    override fun onAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                if (isPng()) pausePlayer()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                player?.setVolume(0.1f, 0.1f)
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                player?.setVolume(1.0f, 1.0f)
                go()
            }
        }
    }

    private fun buildNotification(song: Song): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, 0)

        return NotificationCompat.Builder(this, "BEATBERRY_CHANNEL")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(song.title)
            .setContentText(song.artist)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setStyle(androidx.media.app.NotificationCompat.MediaStyle()
                .setMediaSession(mediaSession?.sessionToken)
                .setShowActionsInCompactView(0, 1, 2))
            .addAction(android.R.drawable.ic_media_previous, "Previous", MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS))
            .addAction(if (isPng()) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play, "Play/Pause", MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_PLAY_PAUSE))
            .addAction(android.R.drawable.ic_media_next, "Next", MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_SKIP_TO_NEXT))
            .build()
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
        return true
    }

    override fun onDestroy() {
        @Suppress("DEPRECATION")
        audioManager.unregisterMediaButtonEventReceiver(mediaButtonReceiverComponent)
        player?.stop()
        player?.release()
        mediaSession?.release()
        super.onDestroy()
    }

    fun playSong() {
        @Suppress("DEPRECATION")
        val result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
        if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) return

        player?.reset()
        val playSong = songs?.getOrNull(songPos) ?: return
        
        try {
            player?.setDataSource(applicationContext, playSong.uri)
            updateMetadata(playSong)
            startForeground(1, buildNotification(playSong))
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
        songs?.getOrNull(songPos)?.let { startForeground(1, buildNotification(it)) }
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
        songs?.getOrNull(songPos)?.let { startForeground(1, buildNotification(it)) }
        stopForeground(false)
    }
    
    fun seek(posn: Int) { player?.seekTo(posn) }
    
    fun go() { 
        @Suppress("DEPRECATION")
        val result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            player?.start()
            updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)
            songs?.getOrNull(songPos)?.let { startForeground(1, buildNotification(it)) }
        }
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

    fun getCurrentSong(): Song? = songs?.getOrNull(songPos)

    private fun updatePlaybackState(state: Int) {
        val speed = if (state == PlaybackStateCompat.STATE_PLAYING) 1.0f else 0.0f
        val playbackState = PlaybackStateCompat.Builder()
            .setActions(
                PlaybackStateCompat.ACTION_PLAY or 
                PlaybackStateCompat.ACTION_PAUSE or 
                PlaybackStateCompat.ACTION_PLAY_PAUSE or
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT or 
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                PlaybackStateCompat.ACTION_STOP or
                PlaybackStateCompat.ACTION_SEEK_TO
            )
            .setState(state, getPosn().toLong(), speed)
            .build()
        mediaSession?.setPlaybackState(playbackState)
    }
}
