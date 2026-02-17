package com.example.bb10_musicplayer

import android.Manifest
import android.app.AlertDialog
import android.content.*
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.database.Cursor
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.google.android.material.navigation.NavigationView
import java.io.File
import java.util.Locale
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private var musicService: MusicService? = null
    private var playIntent: Intent? = null
    private var musicBound = false
    private var songList = mutableListOf<Song>()
    private var playlists = mutableListOf<Playlist>()
    private lateinit var seekBar: SeekBar
    private lateinit var txtCurrentTime: TextView
    private lateinit var txtTotalDuration: TextView
    private lateinit var btnPlayPause: ImageButton
    private lateinit var controlPanel: View
    private val handler = Handler(Looper.getMainLooper())
    private var currentFragment: Fragment? = null

    private lateinit var searchBarLayout: RelativeLayout
    private lateinit var editSearch: EditText
    private lateinit var btnCloseSearch: ImageButton

    private val musicConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val binder = service as MusicService.MusicBinder
            musicService = binder.getService()
            musicService?.setList(songList)
            musicBound = true
            updateHomeFragment()
        }

        override fun onServiceDisconnected(name: ComponentName) {
            musicBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        applySettings()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        loadPlaylists()

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        val drawer: DrawerLayout = findViewById(R.id.drawer_layout)
        val toggle = ActionBarDrawerToggle(
            this, drawer, toolbar, R.string.app_name, R.string.app_name
        )
        drawer.addDrawerListener(toggle)
        toggle.syncState()

        val navigationView: NavigationView = findViewById(R.id.nav_view)
        navigationView.setNavigationItemSelectedListener(this)

        controlPanel = findViewById(R.id.control_panel)
        seekBar = findViewById(R.id.seek_bar)
        txtCurrentTime = findViewById(R.id.txt_current_time)
        txtTotalDuration = findViewById(R.id.txt_total_duration)
        btnPlayPause = findViewById(R.id.btn_play_pause)
        val btnPrev: ImageButton = findViewById(R.id.btn_prev)
        val btnNext: ImageButton = findViewById(R.id.btn_next)

        searchBarLayout = findViewById(R.id.search_bar_layout)
        editSearch = findViewById(R.id.edit_search)
        btnCloseSearch = findViewById(R.id.btn_close_search)

        btnPlayPause.setOnClickListener {
            if (musicService?.isPng() == true) {
                musicService?.pausePlayer()
                btnPlayPause.setImageResource(android.R.drawable.ic_media_play)
            } else {
                musicService?.go()
                btnPlayPause.setImageResource(android.R.drawable.ic_media_pause)
            }
        }

        btnPrev.setOnClickListener { musicService?.playPrev() }
        btnNext.setOnClickListener { musicService?.playNext() }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    musicService?.seek(progress)
                    txtCurrentTime.text = formatTime(progress.toLong())
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        btnCloseSearch.setOnClickListener {
            hideSearchBar()
        }

        editSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                performSearch(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        if (savedInstanceState == null) {
            showFragment(HomeFragment(), "HOME")
        }

        checkPermissions()
        startSeekBarUpdate()
    }

    private fun loadPlaylists() {
        val prefs = getSharedPreferences("playlists_prefs", Context.MODE_PRIVATE)
        val playlistNames = prefs.getStringSet("playlist_names", null) ?: setOf("Favorites")
        
        playlists.clear()
        
        // Ensure Favorites is always first
        val sortedNames = playlistNames.toMutableList().sorted().toMutableList()
        if (sortedNames.contains("Favorites")) {
            sortedNames.remove("Favorites")
            sortedNames.add(0, "Favorites")
        }

        sortedNames.forEach { name ->
            val id = if (name == "Favorites") 0L else prefs.getLong("playlist_id_$name", System.currentTimeMillis())
            val songsStr = prefs.getString("playlist_songs_$name", "") ?: ""
            val songsList = if (songsStr.isEmpty()) mutableListOf<Long>() else songsStr.split(",").map { it.toLong() }.toMutableList()
            val dateCreated = prefs.getLong("playlist_date_$name", System.currentTimeMillis())
            playlists.add(Playlist(id, name, songsList, dateCreated))
        }
        
        if (playlists.none { it.name == "Favorites" }) {
            playlists.add(0, Playlist(0L, "Favorites", mutableListOf(), 0L))
            savePlaylists()
        }
    }

    fun savePlaylists() {
        val prefs = getSharedPreferences("playlists_prefs", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        val names = playlists.map { it.name }.toSet()
        editor.putStringSet("playlist_names", names)
        
        playlists.forEach { playlist ->
            editor.putLong("playlist_id_${playlist.name}", playlist.id)
            editor.putString("playlist_songs_${playlist.name}", playlist.songs.joinToString(","))
            editor.putLong("playlist_date_${playlist.name}", playlist.dateCreated)
        }
        editor.apply()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val fragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        val tag = fragment?.tag
        when (tag) {
            "HOME" -> {
                menuInflater.inflate(R.menu.home_options, menu)
                return true
            }
            "PLAYLISTS" -> {
                menuInflater.inflate(R.menu.playlists_options, menu)
                return true
            }
            "PLAYLIST_SONGS" -> {
                menuInflater.inflate(R.menu.playlist_songs_options, menu)
                return true
            }
            "BIG_CONTROLS" -> {
                menu.add(0, 100, 0, getString(R.string.action_add_to_playlist))
                menu.add(0, 101, 0, getString(R.string.action_add_to_favorite))
                return true
            }
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val fragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        when (item.itemId) {
            R.id.action_search_songs, R.id.action_search_playlists, R.id.action_search_playlist_songs -> {
                showSearchBar()
                return true
            }
            R.id.action_rearrange_playlist -> {
                (fragment as? PlaylistSongsFragment)?.enableRearrange()
                return true
            }
            R.id.sort_name_asc -> (fragment as? Sortable)?.sortByName(true)
            R.id.sort_name_desc -> (fragment as? Sortable)?.sortByName(false)
            R.id.sort_newest -> (fragment as? Sortable)?.sortByDate(true)
            R.id.sort_oldest -> (fragment as? Sortable)?.sortByDate(false)
            100 -> { // Add to playlist from Big Controls
                musicService?.getCurrentSong()?.let { addToPlaylist(it) }
                return true
            }
            101 -> { // Add to favorite from Big Controls
                val favorites = playlists.find { it.name == "Favorites" }
                val currentSong = musicService?.getCurrentSong()
                if (favorites != null && currentSong != null) {
                    if (!favorites.songs.contains(currentSong.id)) {
                        favorites.songs.add(currentSong.id)
                        savePlaylists()
                        Toast.makeText(this, getString(R.string.action_add_to_favorite), Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Already in favorites", Toast.LENGTH_SHORT).show()
                    }
                }
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showSearchBar() {
        searchBarLayout.visibility = View.VISIBLE
        editSearch.requestFocus()
    }

    private fun hideSearchBar() {
        searchBarLayout.visibility = View.GONE
        editSearch.setText("")
        performSearch("")
    }

    private fun performSearch(query: String) {
        val fragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        (fragment as? Searchable)?.filter(query)
    }

    fun switchToPlaylists() {
        showFragment(PlaylistsFragment(), "PLAYLISTS")
        val navigationView: NavigationView = findViewById(R.id.nav_view)
        navigationView.setCheckedItem(R.id.nav_view_playlists)
    }

    fun createPlaylist(name: String) {
        if (name.equals("Favorites", ignoreCase = true)) {
            Toast.makeText(this, "Cannot create a playlist named Favorites", Toast.LENGTH_SHORT).show()
            return
        }
        val id = (playlists.maxByOrNull { it.id }?.id ?: 0) + 1
        playlists.add(Playlist(id, name))
        savePlaylists()
    }

    fun getPlaylists(): MutableList<Playlist> = playlists

    fun getMusicService(): MusicService? = musicService

    private fun showFragment(fragment: Fragment, tag: String) {
        currentFragment = fragment
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment, tag)
            .commit()
        
        handler.post { 
            if (tag == "HOME") updateHomeFragment()
            invalidateOptionsMenu() 
            
            // Hide control panel when in Big Controls
            if (tag == "BIG_CONTROLS") {
                controlPanel.visibility = View.GONE
            } else {
                controlPanel.visibility = View.VISIBLE
            }
        }
        hideSearchBar()
    }

    private fun updateHomeFragment() {
        val homeFragment = supportFragmentManager.findFragmentByTag("HOME") as? HomeFragment
        homeFragment?.setSongs(songList) { position ->
            playSongsList(songList, position)
        }
    }

    fun playSong(position: Int) {
        musicService?.setSong(position)
        musicService?.playSong()
        btnPlayPause.setImageResource(android.R.drawable.ic_media_pause)
    }

    fun playSongsList(list: List<Song>, position: Int) {
        musicService?.setList(list)
        musicService?.setSong(position)
        musicService?.playSong()
        btnPlayPause.setImageResource(android.R.drawable.ic_media_pause)
    }

    fun showPlaylistSongs(playlist: Playlist) {
        val fragment = PlaylistSongsFragment.newInstance(playlist)
        showFragment(fragment, "PLAYLIST_SONGS")
    }

    private fun applySettings() {
        val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
        val lang = prefs.getString("lang", "en") ?: "en"
        val locale = Locale(lang)
        Locale.setDefault(locale)
        val config = Configuration()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            config.setLocale(locale)
        } else {
            @Suppress("DEPRECATION")
            config.locale = locale
        }
        @Suppress("DEPRECATION")
        baseContext.resources.updateConfiguration(config, baseContext.resources.displayMetrics)
    }

    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 1)
        } else {
            getSongList()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getSongList()
        }
    }

    private fun getSongList() {
        songList.clear()
        val musicResolver: ContentResolver = contentResolver
        val musicUri: Uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val musicCursor: Cursor? = musicResolver.query(musicUri, null, null, null, null)

        val artistOverrides = getSharedPreferences("artist_overrides", Context.MODE_PRIVATE)

        if (musicCursor != null && musicCursor.moveToFirst()) {
            val titleColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.TITLE)
            val idColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media._ID)
            val artistColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)
            val pathColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.DATA)
            val durationColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.DURATION)
            val albumIdColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)
            val dateAddedColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED)

            do {
                val thisId = musicCursor.getLong(idColumn)
                val thisTitle = musicCursor.getString(titleColumn) ?: "Unknown"
                var thisArtist = musicCursor.getString(artistColumn) ?: "Unknown"
                
                // Apply persistent override if exists
                thisArtist = artistOverrides.getString(thisId.toString(), thisArtist) ?: thisArtist

                val thisPath = musicCursor.getString(pathColumn) ?: ""
                val thisDuration = musicCursor.getLong(durationColumn)
                val thisAlbumId = musicCursor.getLong(albumIdColumn)
                val thisDateAdded = musicCursor.getLong(dateAddedColumn)
                songList.add(Song(thisId, thisTitle, thisArtist, thisPath, thisDuration, thisAlbumId, thisDateAdded))
            } while (musicCursor.moveToNext())
        }
        musicCursor?.close()
        updateHomeFragment()
    }

    fun getAllSongs(): List<Song> = songList

    override fun onStart() {
        super.onStart()
        if (playIntent == null) {
            playIntent = Intent(this, MusicService::class.java)
            bindService(playIntent!!, musicConnection, Context.BIND_AUTO_CREATE)
            startService(playIntent)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_home -> showFragment(HomeFragment(), "HOME")
            R.id.nav_big_controls -> showFragment(BigControlsFragment(), "BIG_CONTROLS")
            R.id.nav_create_playlist -> showFragment(CreatePlaylistFragment(), "CREATE_PLAYLIST")
            R.id.nav_view_playlists -> showFragment(PlaylistsFragment(), "PLAYLISTS")
            R.id.nav_rescan -> showFragment(RescanFragment(), "RESCAN")
            R.id.nav_settings -> showFragment(SettingsFragment(), "SETTINGS")
            R.id.nav_about -> showFragment(AboutFragment(), "ABOUT")
        }
        val drawer: DrawerLayout = findViewById(R.id.drawer_layout)
        drawer.closeDrawer(GravityCompat.START)
        return true
    }

    private fun startSeekBarUpdate() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                musicService?.let {
                    if (it.isPng()) {
                        val pos = it.getPosn()
                        val dur = it.getDur()
                        seekBar.max = dur
                        seekBar.progress = pos
                        txtCurrentTime.text = formatTime(pos.toLong())
                        txtTotalDuration.text = formatTime(dur.toLong())
                    }
                }
                handler.postDelayed(this, 1000)
            }
        }, 1000)
    }

    private fun formatTime(millis: Long): String {
        return String.format(Locale.getDefault(), "%02d:%02d", 
            TimeUnit.MILLISECONDS.toMinutes(millis),
            TimeUnit.MILLISECONDS.toSeconds(millis) % TimeUnit.MINUTES.toSeconds(1)
        )
    }

    fun showSongOptions(song: Song) {
        val options = arrayOf("Rename song", "Change Artist name", "Add to playlist", "Add to favorite", "Delete song")
        AlertDialog.Builder(this)
            .setTitle(song.title)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> renameSong(song)
                    1 -> changeArtistName(song)
                    2 -> addToPlaylist(song)
                    3 -> {
                        val favorites = playlists.find { it.name == "Favorites" }
                        if (favorites != null) {
                            if (!favorites.songs.contains(song.id)) {
                                favorites.songs.add(song.id)
                                savePlaylists()
                                Toast.makeText(this, getString(R.string.action_add_to_favorite), Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(this, "Already in favorites", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    4 -> deleteSong(song)
                }
            }
            .show()
    }

    private fun changeArtistName(song: Song) {
        val input = EditText(this)
        input.setText(song.artist)
        AlertDialog.Builder(this)
            .setTitle("Change Artist Name")
            .setView(input)
            .setPositiveButton("OK") { _, _ ->
                val newName = input.text.toString()
                if (newName.isNotEmpty()) {
                    // Save persistently
                    val artistOverrides = getSharedPreferences("artist_overrides", Context.MODE_PRIVATE)
                    artistOverrides.edit().putString(song.id.toString(), newName).commit()
                    
                    song.artist = newName
                    // Update the song list in the service
                    musicService?.setList(songList)
                    // Refresh current fragment
                    val fragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
                    if (fragment is HomeFragment) {
                        updateHomeFragment()
                    } else if (fragment is PlaylistSongsFragment) {
                        (fragment as? Searchable)?.filter("")
                    }
                    Toast.makeText(this, "Artist changed to $newName", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun addToPlaylist(song: Song) {
        if (playlists.size <= 1) { // Only Favorites exists
            Toast.makeText(this, "No other playlists found. Create one first.", Toast.LENGTH_SHORT).show()
            return
        }

        val otherPlaylists = playlists.filter { it.name != "Favorites" }
        val playlistNames = otherPlaylists.map { it.name }.toTypedArray()
        
        AlertDialog.Builder(this)
            .setTitle("Add '${song.title}' to:")
            .setItems(playlistNames) { _, which ->
                val selectedPlaylist = otherPlaylists[which]
                if (!selectedPlaylist.songs.contains(song.id)) {
                    selectedPlaylist.songs.add(song.id)
                    savePlaylists()
                    Toast.makeText(this, "Added to ${selectedPlaylist.name}", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Already in ${selectedPlaylist.name}", Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }

    private fun renameSong(song: Song) {
        val input = EditText(this)
        val file = File(song.path)
        input.setText(file.name)
        AlertDialog.Builder(this)
            .setTitle("Rename Song File")
            .setView(input)
            .setPositiveButton("OK") { _, _ ->
                val newName = input.text.toString()
                if (newName.isNotEmpty()) {
                    val newFile = File(file.parent, newName)
                    if (file.renameTo(newFile)) {
                        Toast.makeText(this, "Renamed to $newName", Toast.LENGTH_SHORT).show()
                        getSongList()
                    } else {
                        Toast.makeText(this, "Failed to rename", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteSong(song: Song) {
        AlertDialog.Builder(this)
            .setTitle("Delete Song")
            .setMessage("Are you sure you want to delete ${song.title}?")
            .setPositiveButton("Delete") { _, _ ->
                val file = File(song.path)
                if (file.delete()) {
                    Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show()
                    getSongList()
                } else {
                    Toast.makeText(this, "Failed to delete", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroy() {
        if (musicBound) {
            unbindService(musicConnection)
            musicBound = false
        }
        stopService(playIntent)
        musicService = null
        super.onDestroy()
    }
}

interface Searchable {
    fun filter(query: String)
}

interface Sortable {
    fun sortByName(ascending: Boolean)
    fun sortByDate(newestFirst: Boolean)
}
