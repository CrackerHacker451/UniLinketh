package com.example.unilink.models

import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class Post(
    val postId: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorMajor: String = "",
    val content: String = "",
    val imageUrl: String? = null,
    val timestamp: Long = 0,
    val likes: Int = 0,
    val likedBy: List<String> = emptyList()
)
