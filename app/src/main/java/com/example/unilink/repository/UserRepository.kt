package com.example.unilink.repository

import com.example.unilink.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class UserRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    fun observeUser(uid: String, onResult: (User?) -> Unit): com.google.firebase.firestore.ListenerRegistration {
        return db.collection("users").document(uid).addSnapshotListener { snapshot, e ->
            if (e != null) {
                onResult(null)
                return@addSnapshotListener
            }
            onResult(snapshot?.toObject(User::class.java))
        }
    }

    fun observeCurrentUser(onResult: (User?) -> Unit): com.google.firebase.firestore.ListenerRegistration? {
        val uid = auth.currentUser?.uid ?: return null
        return observeUser(uid, onResult)
    }

    fun getCurrentUser(onResult: (User?) -> Unit) {
        val uid = auth.currentUser?.uid ?: return onResult(null)
        db.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                val user = document.toObject(User::class.java)
                if (user != null) {
                    checkAndUpdateStreak(user) { updatedUser ->
                        onResult(updatedUser)
                    }
                } else {
                    onResult(null)
                }
            }
            .addOnFailureListener {
                onResult(null)
            }
    }

    private fun checkAndUpdateStreak(user: User, onComplete: (User) -> Unit) {
        val now = java.util.Calendar.getInstance()
        val lastLogin = user.lastLoginDate?.let {
            val cal = java.util.Calendar.getInstance()
            cal.time = it
            cal
        }

        if (lastLogin == null) {
            // First time login or data missing
            updateUserStreak(user.uid, 1, 50, onComplete, user)
        } else {
            val diff = now.timeInMillis - lastLogin.timeInMillis
            val daysDiff = (diff / (1000 * 60 * 60 * 24)).toInt()

            if (daysDiff == 1) {
                // Consecutive day
                updateUserStreak(user.uid, user.streakDays + 1, 50, onComplete, user)
            } else if (daysDiff > 1) {
                // Streak broken
                updateUserStreak(user.uid, 1, 50, onComplete, user)
            } else {
                // Same day, no update needed
                onComplete(user)
            }
        }
    }

    private fun updateUserStreak(uid: String, streak: Int, xpGain: Int, onComplete: (User) -> Unit, oldUser: User) {
        val updates = mapOf(
            "streakDays" to streak,
            "totalXp" to oldUser.totalXp + xpGain,
            "lastLoginDate" to com.google.firebase.firestore.FieldValue.serverTimestamp()
        )
        db.collection("users").document(uid).update(updates)
            .addOnSuccessListener {
                // Refetch user to get the actual server timestamp and updated data
                db.collection("users").document(uid).get().addOnSuccessListener { doc ->
                    onComplete(doc.toObject(User::class.java) ?: oldUser)
                }
            }
            .addOnFailureListener {
                onComplete(oldUser)
            }
    }

    fun getAllUsers(onResult: (List<User>) -> Unit) {
        val currentUid = auth.currentUser?.uid
        db.collection("users").get()
            .addOnSuccessListener { documents ->
                val users = documents.mapNotNull { it.toObject(User::class.java) }
                    .filter { it.uid != currentUid }
                onResult(users)
            }
            .addOnFailureListener {
                onResult(emptyList())
            }
    }

    fun saveUser(user: User, onComplete: (Boolean) -> Unit) {
        // Use set with merge to preserve fields like linkers/linked if they exist
        db.collection("users").document(user.uid).set(user, SetOptions.merge())
            .addOnCompleteListener { task ->
                onComplete(task.isSuccessful)
            }
    }

    fun isUsernameUnique(username: String, onResult: (Boolean) -> Unit) {
        db.collection("users")
            .whereEqualTo("username", username)
            .get()
            .addOnSuccessListener { documents ->
                onResult(documents.isEmpty)
            }
            .addOnFailureListener {
                onResult(false)
            }
    }

    fun getUserById(uid: String, onResult: (User?) -> Unit) {
        db.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                onResult(document.toObject(User::class.java))
            }
            .addOnFailureListener {
                onResult(null)
            }
    }

    fun blockUser(targetUid: String, onComplete: (Boolean) -> Unit) {
        val currentUid = auth.currentUser?.uid ?: return onComplete(false)
        db.collection("users").document(currentUid)
            .update("blockedUsers", com.google.firebase.firestore.FieldValue.arrayUnion(targetUid))
            .addOnCompleteListener { onComplete(it.isSuccessful) }
    }

    fun unblockUser(targetUid: String, onComplete: (Boolean) -> Unit) {
        val currentUid = auth.currentUser?.uid ?: return onComplete(false)
        db.collection("users").document(currentUid)
            .update("blockedUsers", com.google.firebase.firestore.FieldValue.arrayRemove(targetUid))
            .addOnCompleteListener { onComplete(it.isSuccessful) }
    }

    fun linkUser(targetUid: String, onComplete: (Boolean) -> Unit) {
        val currentUid = auth.currentUser?.uid ?: return onComplete(false)
        val batch = db.batch()
        
        // Add to current user's linked list
        val currentUserRef = db.collection("users").document(currentUid)
        batch.update(currentUserRef, "linked", com.google.firebase.firestore.FieldValue.arrayUnion(targetUid))
        
        // Add to target user's linkers list
        val targetUserRef = db.collection("users").document(targetUid)
        batch.update(targetUserRef, "linkers", com.google.firebase.firestore.FieldValue.arrayUnion(currentUid))
        
        batch.commit().addOnCompleteListener { onComplete(it.isSuccessful) }
    }

    fun unlinkUser(targetUid: String, onComplete: (Boolean) -> Unit) {
        val currentUid = auth.currentUser?.uid ?: return onComplete(false)
        val batch = db.batch()
        
        // Remove from current user's linked list
        val currentUserRef = db.collection("users").document(currentUid)
        batch.update(currentUserRef, "linked", com.google.firebase.firestore.FieldValue.arrayRemove(targetUid))
        
        // Remove from target user's linkers list
        val targetUserRef = db.collection("users").document(targetUid)
        batch.update(targetUserRef, "linkers", com.google.firebase.firestore.FieldValue.arrayRemove(currentUid))
        
        batch.commit().addOnCompleteListener { onComplete(it.isSuccessful) }
    }

    fun redeemPremiumCode(code: String, onComplete: (Boolean, String?) -> Unit) {
        val validCodes = listOf("PREMIUM100", "UNILINK_PRO", "STUDENT_ELITE", "PAPABOL1917", "1234")
        if (code.uppercase() == "PAPABOL1917") {
            val uid = auth.currentUser?.uid ?: return onComplete(false, "User not logged in")
            val updates = mapOf(
                "isPremium" to true,
                "premiumCode" to code.uppercase(),
                "uniCoins" to com.google.firebase.firestore.FieldValue.increment(500) // Bonus coins for this special code
            )
            db.collection("users").document(uid).update(updates)
                .addOnSuccessListener { onComplete(true, "VIP Premium activated! +500 UniCoins awarded.") }
                .addOnFailureListener { onComplete(false, it.message) }
        } else if (code.uppercase() in validCodes) {
            val uid = auth.currentUser?.uid ?: return onComplete(false, "User not logged in")
            val updates = mapOf(
                "isPremium" to true,
                "premiumCode" to code.uppercase()
            )
            db.collection("users").document(uid).update(updates)
                .addOnSuccessListener { onComplete(true, "Premium activated!") }
                .addOnFailureListener { onComplete(false, it.message) }
        } else {
            onComplete(false, "Invalid premium code")
        }
    }

    fun activatePremium(onComplete: (Boolean) -> Unit) {
        val uid = auth.currentUser?.uid ?: return onComplete(false)
        val updates = mapOf(
            "isPremium" to true,
            "premiumCode" to "DIRECT_SUBSCRIPTION"
        )
        // Using set with merge is safer than update if the document might not exist
        db.collection("users").document(uid).set(updates, SetOptions.merge())
            .addOnCompleteListener { onComplete(it.isSuccessful) }
    }

    fun updateUserPremiumSettings(bannerResId: Int?, wallpaperResId: Int?, profilePicUrl: String? = null, appTheme: String? = null, onComplete: (Boolean) -> Unit) {
        val uid = auth.currentUser?.uid ?: return onComplete(false)
        val updates = mutableMapOf<String, Any>()
        
        // In a real app, these would be URLs, but we'll use resource names/IDs for this demo
        bannerResId?.let { updates["profileBannerUrl"] = it.toString() }
        wallpaperResId?.let { updates["chatWallpaperUrl"] = it.toString() }
        profilePicUrl?.let { updates["profileImageUrl"] = it }
        appTheme?.let { updates["appTheme"] = it }
        
        if (updates.isEmpty()) return onComplete(true)
        
        db.collection("users").document(uid).update(updates)
            .addOnCompleteListener { onComplete(it.isSuccessful) }
    }

    fun deleteAccount(onComplete: (Boolean) -> Unit) {
        val user = auth.currentUser ?: return onComplete(false)
        val uid = user.uid
        
        // 1. Delete Firestore data
        db.collection("users").document(uid).delete()
            .addOnSuccessListener {
                // 2. Delete Auth user
                user.delete().addOnCompleteListener { onComplete(it.isSuccessful) }
            }
            .addOnFailureListener { onComplete(false) }
    }
}
