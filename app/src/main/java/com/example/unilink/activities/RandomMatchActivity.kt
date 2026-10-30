package com.example.unilink.activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.unilink.databinding.ActivityRandomMatchBinding
import com.example.unilink.utils.ThemeUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class RandomMatchActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRandomMatchBinding
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val userRepository = com.example.unilink.repository.UserRepository()
    
    private val handler = Handler(Looper.getMainLooper())
    private var isMatching = false
    private var matchListener: ListenerRegistration? = null
    private var myName: String = "Student"

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeUtils.applyTheme(this)
        super.onCreate(savedInstanceState)
        binding = ActivityRandomMatchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.btnStartMatch.setOnClickListener {
            if (!isMatching) startMatching()
        }

        binding.btnCancelMatch.setOnClickListener {
            stopMatching()
        }

        // Pre-fetch my name for faster matching
        userRepository.getCurrentUser { user ->
            myName = user?.name ?: "Student"
        }

        listenForMatch()
    }

    private fun listenForMatch() {
        val uid = auth.currentUser?.uid ?: return
        // Listen for matches where I am the receiver (someone found me)
        matchListener = db.collection("random_matches").document(uid).addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.e("RandomMatch", "Listen failed", e)
                return@addSnapshotListener
            }
            
            if (snapshot != null && snapshot.exists() && isMatching) {
                val hostId = snapshot.getString("hostId") ?: return@addSnapshotListener
                val hostName = snapshot.getString("hostName") ?: "Student"
                
                Log.d("RandomMatch", "Received match request from $hostName ($hostId)")
                
                // Cleanup the match record
                db.collection("random_matches").document(uid).delete()
                
                finalizeMatch(hostId, hostName, false)
            }
        }
    }

    private fun startMatching() {
        val uid = auth.currentUser?.uid ?: return
        isMatching = true
        
        binding.btnStartMatch.visibility = View.GONE
        binding.btnCancelMatch.visibility = View.VISIBLE
        binding.tvStatus.text = "Searching for students..."
        binding.viewPulse.visibility = View.VISIBLE
        
        val pulse = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
        pulse.duration = 1000
        pulse.repeatCount = -1
        binding.viewPulse.startAnimation(pulse)

        db.collection("users").document(uid).update(
            "isLookingForRandomMatch", true
        ).addOnSuccessListener {
            Log.d("RandomMatch", "Search status active in Firestore")
            findMatch()
        }.addOnFailureListener { e ->
            Log.e("RandomMatch", "Failed to start search", e)
            Toast.makeText(this, "Connection error. Try again.", Toast.LENGTH_SHORT).show()
            stopMatching()
        }
    }

    private fun findMatch() {
        if (!isMatching) return
        val myUid = auth.currentUser?.uid ?: return

        db.collection("users")
            .whereEqualTo("isLookingForRandomMatch", true)
            .limit(15)
            .get()
            .addOnSuccessListener { snapshots ->
                if (!isMatching) return@addOnSuccessListener
                
                val potentialMatches = snapshots.documents.filter { it.id != myUid }

                if (potentialMatches.isNotEmpty()) {
                    val match = potentialMatches.random()
                    val matchId = match.id
                    val matchName = match.getString("name") ?: "Student"
                    
                    Log.d("RandomMatch", "Attempting to host match with $matchName ($matchId)")
                    
                    val matchData = mapOf(
                        "hostId" to myUid,
                        "hostName" to myName,
                        "receiverId" to matchId,
                        "receiverName" to matchName,
                        "status" to "pending",
                        "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                    )

                    // Try to claim the receiver's doc slot
                    db.collection("random_matches").document(matchId).set(matchData)
                        .addOnSuccessListener {
                            if (isMatching) {
                                finalizeMatch(matchId, matchName, true)
                            }
                        }
                        .addOnFailureListener {
                            // Slot might be taken, retry
                            handler.postDelayed({ findMatch() }, 2000)
                        }
                } else {
                    // No one found, retry in 3 seconds
                    handler.postDelayed({ findMatch() }, 3000)
                }
            }
            .addOnFailureListener { e ->
                Log.e("RandomMatch", "Query failed", e)
                if (isMatching) {
                    handler.postDelayed({ findMatch() }, 5000)
                }
            }
    }

    private fun finalizeMatch(partnerUid: String, partnerName: String, isOffer: Boolean) {
        if (!isMatching) return
        
        isMatching = false
        handler.removeCallbacksAndMessages(null)

        val myUid = auth.currentUser?.uid ?: return
        
        // Turn off search status in background
        db.collection("users").document(myUid).update("isLookingForRandomMatch", false)
        
        Toast.makeText(this, "Matched with $partnerName!", Toast.LENGTH_SHORT).show()
        
        val channelId = if (myUid < partnerUid) "${myUid}_$partnerUid" else "${partnerUid}_$myUid"

        val intent = Intent(this, VideoCallActivity::class.java).apply {
            putExtra("partnerId", partnerUid)
            putExtra("partnerName", partnerName)
            putExtra("channelId", channelId)
            putExtra("isOffer", isOffer)
        }
        startActivity(intent)
        finish()
    }

    private fun stopMatching() {
        val uid = auth.currentUser?.uid ?: return
        isMatching = false
        binding.btnStartMatch.visibility = View.VISIBLE
        binding.btnCancelMatch.visibility = View.GONE
        binding.tvStatus.text = "Ready to meet someone new?"
        binding.viewPulse.clearAnimation()
        binding.viewPulse.visibility = View.GONE
        
        db.collection("users").document(uid).update("isLookingForRandomMatch", false)
        handler.removeCallbacksAndMessages(null)
    }

    override fun onDestroy() {
        super.onDestroy()
        matchListener?.remove()
        stopMatching()
    }
}
