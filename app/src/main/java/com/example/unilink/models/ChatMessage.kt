package com.example.unilink.models

import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

@IgnoreExtraProperties
data class ChatMessage(
    val messageId: String = "",
    val senderId: String = "",
    val senderUsername: String = "",
    val receiverId: String = "",
    val messageText: String = "",
    @ServerTimestamp val timestamp: Date? = null,
    val status: String = "sent", // sent, delivered, seen
    val type: String = "text", // text, poll, doubt, token, code, voice
    val replyToId: String? = null,
    val replyToText: String? = null,
    val reactions: Map<String, List<String>> = emptyMap(), // emoji -> list of userIds
    
    // Metadata for special cards
    val pollQuestion: String? = null,
    val pollOptions: Map<String, Int> = emptyMap(), // optionText -> voteCount
    val userVotes: Map<String, String> = emptyMap(), // userId -> optionText
    
    val doubtReward: Int? = null,
    val isDoubtResolved: Boolean = false,
    val isEdited: Boolean = false,
    
    val uniCoinAmount: Int? = null,
    val tokenReason: String? = null
)
