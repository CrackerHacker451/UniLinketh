package com.example.unilink.models

import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

@IgnoreExtraProperties
data class ChatRoom(
    val chatId: String = "",
    val participants: List<String> = emptyList(),
    val lastMessage: String = "",
    val lastMessageSenderId: String = "",
    @ServerTimestamp val lastTimestamp: Date? = null,
    val isGroup: Boolean = false,
    val groupName: String = "",
    val groupImageUrl: String = "",
    val unreadCounts: Map<String, Int> = emptyMap(), // Track unread messages per user
    val typingStatus: Map<String, Boolean> = emptyMap() // userId -> isTyping
)
