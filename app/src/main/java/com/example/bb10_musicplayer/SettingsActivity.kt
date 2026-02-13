package com.example.bb10_musicplayer

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SwitchCompat
import java.util.Locale

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
        val lang = prefs.getString("lang", "en") ?: "en"
        setLocale(lang)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val switchDarkMode: SwitchCompat = findViewById(R.id.switch_dark_mode)
        val radioGroupLang: RadioGroup = findViewById(R.id.radio_group_lang)
        val btnClearCache: Button = findViewById(R.id.btn_clear_cache)

        // Dark mode logic
        val isDarkMode = prefs.getBoolean("dark_mode", false)
        switchDarkMode.isChecked = isDarkMode
        switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("dark_mode", isChecked).apply()
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
        }

        // Language logic
        if (lang == "ar") {
            radioGroupLang.check(R.id.radio_ar)
        } else {
            radioGroupLang.check(R.id.radio_en)
        }

        radioGroupLang.setOnCheckedChangeListener { _, checkedId ->
            val newLang = if (checkedId == R.id.radio_ar) "ar" else "en"
            if (newLang != prefs.getString("lang", "en")) {
                prefs.edit().putString("lang", newLang).apply()
                setLocale(newLang)
                recreate() 
            }
        }

        btnClearCache.setOnClickListener {
            try {
                cacheDir.deleteRecursively()
                Toast.makeText(this, "Cache cleared", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Failed to clear cache", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setLocale(lang: String) {
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
        resources.updateConfiguration(config, resources.displayMetrics)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
