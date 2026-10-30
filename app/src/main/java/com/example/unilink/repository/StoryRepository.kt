package com.example.unilink.repository

import com.example.unilink.models.Story
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.util.Date

class StoryRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    fun uploadStory(imageUrl: String, onComplete: (Boolean) -> Unit) {
        val uid = auth.currentUser?.uid ?: return
        val storyId = db.collection("stories").document().id
        
        db.collection("users").document(uid).get().addOnSuccessListener { doc ->
            val name = doc.getString("name") ?: "User"
            
            val story = Story(
                storyId = storyId,
                userId = uid,
                userName = name,
                imageUrl = imageUrl
            )

            // Stories usually expire in 24h, but for simplicity we just store them
            db.collection("stories").document(storyId).set(story)
                .addOnCompleteListener { task ->
                    onComplete(task.isSuccessful)
                }
        }
    }

    fun getActiveStories(onResult: (List<Story>) -> Unit) {
        db.collection("stories")
            .limit(20)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    onResult(emptyList())
                    return@addSnapshotListener
                }
                val stories = snapshot?.mapNotNull { it.toObject(Story::class.java) } ?: emptyList()
                onResult(stories)
            }
    }
}
