package com.example.unilink.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.unilink.R
import com.example.unilink.databinding.ActivityMainBinding
import com.example.unilink.fragments.HomeFragment
import com.example.unilink.fragments.MessagesFragment
import com.example.unilink.fragments.SearchFragment
import com.example.unilink.fragments.ProjectsFragment
import com.example.unilink.utils.ThemeUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val auth = FirebaseAuth.getInstance()
    private val userRepository = com.example.unilink.repository.UserRepository()
    private var currentFragmentTag: String? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            updateFcmToken()
        } else {
            Toast.makeText(this, "Notifications disabled.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            ThemeUtils.applyTheme(this)
        } catch (e: Exception) {
            Log.e("MainActivity", "Theme application failed", e)
        }
        
        super.onCreate(savedInstanceState)
        
        if (auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        try {
            Log.d("MainActivity", "Inflating layout")
            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)

            Log.d("MainActivity", "Setting up navigation")
            setupNavigation(savedInstanceState)
            
            Log.d("MainActivity", "Setting up click listeners")
            setupClickListeners()

            Log.d("MainActivity", "Updating extra services")
            askNotificationPermission()
            updateOnlineStatus(true)
            
            // Sync theme
            userRepository.getCurrentUser { user ->
                user?.appTheme?.let {
                    if (it != ThemeUtils.getSelectedTheme(this)) {
                        ThemeUtils.saveTheme(this, it)
                    }
                }
            }
            
            Log.d("MainActivity", "onCreate completed successfully")
        } catch (e: Throwable) {
            Log.e("MainActivity", "CRASH in onCreate: ${e.message}", e)
            val errorMsg = e.message ?: e.toString()
            Toast.makeText(this, "Something went wrong: $errorMsg", Toast.LENGTH_LONG).show()
            // Try to recover by at least showing the background
            try { setContentView(R.layout.activity_main) } catch (ignore: Exception) {}
        }
    }

    private fun setupNavigation(savedInstanceState: Bundle?) {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            try {
                when (item.itemId) {
                    R.id.nav_home -> {
                        loadFragment(HomeFragment())
                        true
                    }
                    R.id.nav_search -> {
                        loadFragment(SearchFragment())
                        true
                    }
                    R.id.nav_projects -> {
                        loadFragment(ProjectsFragment())
                        true
                    }
                    R.id.nav_messages -> {
                        loadFragment(MessagesFragment())
                        true
                    }
                    R.id.nav_profile -> {
                        startActivity(Intent(this, ProfileActivity::class.java))
                        false
                    }
                    else -> false
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Navigation error", e)
                false
            }
        }

        if (savedInstanceState == null) {
            binding.bottomNavigation.selectedItemId = R.id.nav_messages
        }
    }

    private fun setupClickListeners() {
    }

    private fun updateOnlineStatus(online: Boolean) {
        val uid = auth.currentUser?.uid ?: return
        FirebaseFirestore.getInstance().collection("users").document(uid)
            .update("isOnline", online)
            .addOnFailureListener { e ->
                Log.e("MainActivity", "Status update failed: ${e.message}")
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (auth.currentUser != null) {
            updateOnlineStatus(false)
        }
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                updateFcmToken()
            } else {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            updateFcmToken()
        }
    }

    private fun loadFragment(fragment: Fragment) {
        val tag = fragment::class.java.simpleName
        if (currentFragmentTag == tag) return
        currentFragmentTag = tag

        supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                android.R.anim.fade_in,
                android.R.anim.fade_out,
                android.R.anim.fade_in,
                android.R.anim.fade_out
            )
            .replace(R.id.fragmentContainer, fragment, tag)
            .commit()
    }

    private fun updateFcmToken() {
        val uid = auth.currentUser?.uid ?: return
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            FirebaseFirestore.getInstance().collection("users").document(uid)
                .update("fcmToken", token)
        }.addOnFailureListener { e ->
            Log.e("MainActivity", "FCM token update failed: ${e.message}")
        }
    }
}
