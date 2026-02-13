package com.example.bb10_musicplayer

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import java.util.Locale

class SettingsFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.activity_settings, container, false)
        
        val switchDarkMode: SwitchCompat = view.findViewById(R.id.switch_dark_mode)
        val radioGroupLang: RadioGroup = view.findViewById(R.id.radio_group_lang)
        val btnClearCache: Button = view.findViewById(R.id.btn_clear_cache)

        val prefs = requireContext().getSharedPreferences("settings", Context.MODE_PRIVATE)

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
        val lang = prefs.getString("lang", "en") ?: "en"
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
                activity?.recreate() 
            }
        }

        btnClearCache.setOnClickListener {
            try {
                requireContext().cacheDir.deleteRecursively()
                Toast.makeText(context, "Cache cleared", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to clear cache", Toast.LENGTH_SHORT).show()
            }
        }

        return view
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
}
