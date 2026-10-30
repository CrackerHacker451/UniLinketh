package com.example.unilink.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.example.unilink.R
import com.example.unilink.databinding.ActivitySettingsBinding
import com.example.unilink.repository.UserRepository
import com.example.unilink.utils.NotificationHelper
import com.example.unilink.utils.ThemeUtils
import com.google.firebase.auth.FirebaseAuth

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private val auth = FirebaseAuth.getInstance()
    private val userRepository = UserRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeUtils.applyTheme(this)
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        setupListeners()
        setupThemeSelectors()
        loadPreferences()
    }

    private fun setupListeners() {
        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
        }

        binding.switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            val status = if (isChecked) "enabled" else "disabled"
            Toast.makeText(this, "Notifications $status", Toast.LENGTH_SHORT).show()
        }

        binding.rlTestNotification.setOnClickListener {
            NotificationHelper.showNotification(this, "Uni Link", "This is a test notification!")
        }

        binding.rlCopyToken.setOnClickListener {
            com.google.firebase.messaging.FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                val clipboard = getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val clip = android.content.ClipData.newPlainText("FCM Token", token)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this, "Token copied to clipboard!", Toast.LENGTH_SHORT).show()
            }
        }

        binding.rlLogout.setOnClickListener {
            auth.signOut()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        binding.rlDeleteAccount.setOnClickListener {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Delete Account?")
                .setMessage("This action is permanent and will delete all your data.")
                .setPositiveButton("Delete") { _, _ ->
                    userRepository.deleteAccount { success ->
                        if (success) {
                            Toast.makeText(this, "Account deleted", Toast.LENGTH_SHORT).show()
                            val intent = Intent(this, LoginActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish()
                        } else {
                            Toast.makeText(this, "Failed to delete. Try logging in again.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        binding.rlBlockedUsers.setOnClickListener {
            startActivity(Intent(this, BlockedUsersActivity::class.java))
        }

        binding.rlPrivacy.setOnClickListener {
            Toast.makeText(this, "Privacy Policy coming soon", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupThemeSelectors() {
        val currentTheme = ThemeUtils.getSelectedTheme(this)
        
        // Helper to update selection UI
        fun updateSelection() {
            val activeTheme = ThemeUtils.getSelectedTheme(this)
            val strokeColor = getColor(R.color.white)
            val inactiveColor = android.graphics.Color.TRANSPARENT
            
            binding.themeCyber.cardThemeColor.strokeColor = if (activeTheme == "Cyber") strokeColor else inactiveColor
            binding.themeIndigo.cardThemeColor.strokeColor = if (activeTheme == "Indigo Liquid") strokeColor else inactiveColor
            binding.themeTeal.cardThemeColor.strokeColor = if (activeTheme == "Ocean Glass") strokeColor else inactiveColor
            binding.themeRose.cardThemeColor.strokeColor = if (activeTheme == "Rose Crystal") strokeColor else inactiveColor
            binding.themeAmber.cardThemeColor.strokeColor = if (activeTheme == "Amber Glow") strokeColor else inactiveColor
        }

        // Theme Cyber (Default Pink/Cyan)
        binding.themeCyber.apply {
            viewColor.setBackgroundResource(R.drawable.grad_cyber)
            root.setOnClickListener { applyThemeAndRestart("Cyber") }
        }
        
        // Theme Indigo
        binding.themeIndigo.apply {
            viewColor.setBackgroundColor(getColor(R.color.liquid_indigo))
            root.setOnClickListener { applyThemeAndRestart("Indigo Liquid") }
        }
        
        // Theme Teal
        binding.themeTeal.apply {
            viewColor.setBackgroundColor(getColor(R.color.liquid_teal))
            root.setOnClickListener { applyThemeAndRestart("Ocean Glass") }
        }
        
        // Theme Rose
        binding.themeRose.apply {
            viewColor.setBackgroundColor(getColor(R.color.liquid_rose))
            root.setOnClickListener { applyThemeAndRestart("Rose Crystal") }
        }
        
        // Theme Amber
        binding.themeAmber.apply {
            viewColor.setBackgroundColor(getColor(R.color.liquid_amber))
            root.setOnClickListener { applyThemeAndRestart("Amber Glow") }
        }
        
        updateSelection()
    }

    private fun applyThemeAndRestart(themeName: String) {
        if (ThemeUtils.getSelectedTheme(this) == themeName) return
        
        ThemeUtils.saveTheme(this, themeName)
        Toast.makeText(this, "Applied $themeName Theme!", Toast.LENGTH_SHORT).show()
        
        // Restart activity to apply theme
        recreate()
    }

    private fun loadPreferences() {
        val isNightMode = AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES
        binding.switchDarkMode.isChecked = isNightMode
    }
}
