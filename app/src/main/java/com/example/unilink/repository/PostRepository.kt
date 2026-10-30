package com.example.unilink.repository

import com.example.unilink.models.Post
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class PostRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    fun createPost(content: String, imageUrl: String? = null, onComplete: (Boolean) -> Unit) {
        val uid = auth.currentUser?.uid ?: return
        val postId = db.collection("posts").document().id
        
        // Fetch user name for the post
        db.collection("users").document(uid).get().addOnSuccessListener { doc ->
            val name = doc.getString("name") ?: "User"
            val major = doc.getString("branch") ?: ""
            
            val post = Post(
                postId = postId,
                authorId = uid,
                authorName = name,
                authorMajor = major,
                content = content,
                imageUrl = imageUrl,
                timestamp = System.currentTimeMillis(),
                likes = 0
            )

            db.collection("posts").document(postId).set(post)
                .addOnCompleteListener { task ->
                    onComplete(task.isSuccessful)
                }
        }
    }

    fun deletePost(postId: String, onComplete: (Boolean) -> Unit) {
        db.collection("posts").document(postId).delete()
            .addOnCompleteListener { onComplete(it.isSuccessful) }
    }

    fun getFeed(onResult: (List<Post>) -> Unit) {
        db.collection("posts")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    onResult(emptyList())
                    return@addSnapshotListener
                }
                val posts = snapshot?.mapNotNull { doc ->
                    try {
                        doc.toObject(Post::class.java)
                    } catch (ex: Exception) {
                        val timestampObj = doc.get("timestamp")
                        val ts = when (timestampObj) {
                            is com.google.firebase.Timestamp -> timestampObj.toDate().time
                            is Long -> timestampObj
                            is Number -> timestampObj.toLong()
                            else -> 0L
                        }
                        Post(
                            postId = doc.getString("postId") ?: doc.id,
                            authorId = doc.getString("authorId") ?: "",
                            authorName = doc.getString("authorName") ?: "User",
                            authorMajor = doc.getString("authorMajor") ?: "",
                            content = doc.getString("content") ?: "",
                            imageUrl = doc.getString("imageUrl"),
                            timestamp = ts,
                            likes = (doc.getLong("likes") ?: 0L).toInt(),
                            likedBy = (doc.get("likedBy") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                        )
                    }
                } ?: emptyList()
                onResult(posts)
            }
    }

    fun likePost(postId: String, onComplete: (Boolean) -> Unit) {
        val uid = auth.currentUser?.uid ?: return
        val postRef = db.collection("posts").document(postId)
        
        db.runTransaction { transaction ->
            val snapshot = transaction.get(postRef)
            val likedBy = snapshot.get("likedBy") as? List<String> ?: emptyList()
            
            if (!likedBy.contains(uid)) {
                val newLikedBy = likedBy + uid
                val newLikes = (snapshot.getLong("likes") ?: 0) + 1
                transaction.update(postRef, "likedBy", newLikedBy)
                transaction.update(postRef, "likes", newLikes)
            }
        }.addOnCompleteListener { task ->
            onComplete(task.isSuccessful)
        }
    }
}
