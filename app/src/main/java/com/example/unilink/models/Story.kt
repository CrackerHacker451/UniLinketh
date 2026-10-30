package com.example.unilink.models

import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class Story(
    val storyId: String = "",
    val userId: String = "",
    val userName: String = "",
    val imageUrl: String = ""
)
