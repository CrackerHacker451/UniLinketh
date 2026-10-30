package com.example.unilink.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import com.example.unilink.databinding.ActivitySplashBinding
import com.example.unilink.utils.ThemeUtils
import com.google.firebase.auth.FirebaseAuth

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private var _binding: ActivitySplashBinding? = null
    private val binding get() = _binding!!
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeUtils.applyTheme(this)
        super.onCreate(savedInstanceState)
        com.example.unilink.utils.NotificationHelper.init(this)
        try {
            _binding = ActivitySplashBinding.inflate(layoutInflater)
            setContentView(binding.root)

            // Liquid Entrance Animations
            val fadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
            fadeIn.duration = 1000
            
            _binding?.cardLogo?.startAnimation(fadeIn)
            _binding?.tvLogo?.startAnimation(fadeIn)
            _binding?.tvBranding?.startAnimation(fadeIn)

            // Animated Progress
            _binding?.splashProgress?.alpha = 0f
            _binding?.splashProgress?.animate()?.alpha(1f)?.setDuration(500)?.setStartDelay(500)?.start()

            // Delay for 2.5 seconds (slightly faster) then check login status
            Handler(Looper.getMainLooper()).postDelayed({
                if (isFinishing || isDestroyed) return@postDelayed
                
                try {
                    val currentUser = auth.currentUser
                    if (currentUser != null) {
                        startActivity(Intent(this, MainActivity::class.java))
                    } else {
                        startActivity(Intent(this, LoginActivity::class.java))
                    }
                    finish()
                } catch (e: Throwable) {
                    Log.e("SplashActivity", "Error during navigation: ${e.message}")
                    safeNavigateToLogin()
                }
            }, 2500)
        } catch (e: Throwable) {
            Log.e("SplashActivity", "Critical error in onCreate: ${e.message}", e)
            safeNavigateToLogin()
        }
    }

    private fun safeNavigateToLogin() {
        try {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        } catch (e: Throwable) {
            Log.e("SplashActivity", "Failed to even navigate to login: ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}
