package com.example.unilink.repository

import android.util.Log
import com.example.unilink.models.ChatMessage
import com.example.unilink.models.ChatRoom
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions

class ChatRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    fun sendMessage(
        chatId: String,
        text: String,
        senderUsername: String,
        isGroup: Boolean,
        receiverId: String? = null,
        replyToId: String? = null,
        replyToText: String? = null,
        onComplete: (Boolean) -> Unit
    ) {
        val currentUserId = auth.currentUser?.uid ?: run {
            Log.e("ChatRepo", "Send failed: User not logged in")
            onComplete(false)
            return
        }

        val finalReceiverId = if (!isGroup) {
            receiverId ?: chatId.split("_").firstOrNull { it != currentUserId } ?: ""
        } else ""

        if (!isGroup && finalReceiverId.isEmpty()) {
            Log.e("ChatRepo", "Send failed: Could not determine receiverId for chatId $chatId")
            onComplete(false)
            return
        }

        // Optimized block check: Skip if it's a group
        if (!isGroup) {
            db.collection("users").document(finalReceiverId).get()
                .addOnSuccessListener { doc ->
                    val blockedUsers = doc.get("blockedUsers") as? List<*>
                    if (blockedUsers?.contains(currentUserId) == true) {
                        Log.e("ChatRepo", "Message blocked by receiver")
                        onComplete(false)
                    } else {
                        proceedWithSendMessage(chatId, text, senderUsername, isGroup, finalReceiverId, replyToId, replyToText, onComplete)
                    }
                }
                .addOnFailureListener {
                    // Proceed if fetch fails to avoid blocking legitimate users due to temporary network issues
                    proceedWithSendMessage(chatId, text, senderUsername, isGroup, finalReceiverId, replyToId, replyToText, onComplete)
                }
        } else {
            proceedWithSendMessage(chatId, text, senderUsername, isGroup, "", replyToId, replyToText, onComplete)
        }
    }

    fun sendVoiceMessage(chatId: String, receiverId: String, onComplete: (Boolean) -> Unit) {
        val currentUserId = auth.currentUser?.uid ?: return
        val messageRef = db.collection("chats").document(chatId).collection("messages").document()
        
        val voiceData = hashMapOf(
            "messageId" to messageRef.id,
            "senderId" to currentUserId,
            "receiverId" to receiverId,
            "messageText" to "🎙️ Voice Message",
            "type" to "voice",
            "timestamp" to FieldValue.serverTimestamp(),
            "status" to "sent"
        )
        
        messageRef.set(voiceData).addOnCompleteListener { onComplete(it.isSuccessful) }
    }

    private fun proceedWithSendMessage(
        chatId: String,
        text: String,
        senderUsername: String,
        isGroup: Boolean,
        finalReceiverId: String,
        replyToId: String? = null,
        replyToText: String? = null,
        onComplete: (Boolean) -> Unit
    ) {
        val currentUserId = auth.currentUser?.uid ?: return
        val chatRoomRef = db.collection("chats").document(chatId)
        val messageRef = chatRoomRef.collection("messages").document()
        val msgId = messageRef.id

        val messageData = hashMapOf(
            "messageId" to msgId,
            "senderId" to currentUserId,
            "senderUsername" to senderUsername.ifBlank { "User" },
            "receiverId" to finalReceiverId,
            "messageText" to text,
            "timestamp" to FieldValue.serverTimestamp(),
            "status" to "sent",
            "type" to "text",
            "replyToId" to replyToId,
            "replyToText" to replyToText
        )

        val chatRoomUpdate = mutableMapOf<String, Any>(
            "chatId" to chatId,
            "lastMessage" to text,
            "lastMessageSenderId" to currentUserId,
            "lastTimestamp" to FieldValue.serverTimestamp(),
            "isGroup" to isGroup
        )

        if (isGroup) {
            chatRoomUpdate["participants"] = FieldValue.arrayUnion(currentUserId)
        } else {
            chatRoomUpdate["participants"] = listOf(currentUserId, finalReceiverId)
            // Use FieldValue.increment directly in the set with merge
            chatRoomUpdate["unreadCounts.$finalReceiverId"] = FieldValue.increment(1)
        }

        val batch = db.batch()
        // Using set with merge everywhere ensures the document is created if it's a new chat
        batch.set(chatRoomRef, chatRoomUpdate, SetOptions.merge())
        batch.set(messageRef, messageData)

        batch.commit()
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { e ->
                Log.e("ChatRepo", "Batch commit failed: ${e.message}")
                onComplete(false)
            }
    }

    fun listenForMessages(chatId: String, onMessagesUpdate: (List<ChatMessage>) -> Unit) {
        db.collection("chats").document(chatId).collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) return@addSnapshotListener
                val messages = snapshots?.mapNotNull { it.toObject(ChatMessage::class.java) } ?: emptyList()
                onMessagesUpdate(messages)
            }
    }

    fun getRecentChats(onChatsUpdate: (List<ChatRoom>) -> Unit) {
        val currentUserId = auth.currentUser?.uid ?: return
        db.collection("chats")
            .whereArrayContains("participants", currentUserId)
            .orderBy("lastTimestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) return@addSnapshotListener
                val chats = snapshots?.mapNotNull { it.toObject(ChatRoom::class.java) } ?: emptyList()
                onChatsUpdate(chats)
            }
    }

    fun markChatAsRead(chatId: String) {
        val currentUserId = auth.currentUser?.uid ?: return
        db.collection("chats").document(chatId)
            .update("unreadCounts.$currentUserId", 0)
            .addOnFailureListener { Log.e("ChatRepo", "Mark as read failed") }
            
        // NEW: Mark all received messages in this chat as 'seen'
        // Simplified query to avoid index issues with whereNotEqualTo
        db.collection("chats").document(chatId).collection("messages")
            .whereEqualTo("receiverId", currentUserId)
            .get()
            .addOnSuccessListener { snapshots ->
                val batch = db.batch()
                snapshots.documents.forEach { doc ->
                    if (doc.getString("status") != "seen") {
                        batch.update(doc.reference, "status", "seen")
                    }
                }
                if (!snapshots.isEmpty) {
                    batch.commit()
                }
            }
    }

    fun getChatId(id1: String, id2: String): String {
        return if (id1 < id2) "${id1}_${id2}" else "${id2}_${id1}"
    }

    fun editMessage(chatId: String, messageId: String, newText: String, isLastMessage: Boolean = false, onComplete: (Boolean) -> Unit) {
        val batch = db.batch()
        val msgRef = db.collection("chats").document(chatId).collection("messages").document(messageId)
        
        batch.update(msgRef, mapOf(
            "messageText" to newText,
            "isEdited" to true
        ))

        if (isLastMessage) {
            val roomRef = db.collection("chats").document(chatId)
            batch.update(roomRef, "lastMessage", newText)
        }

        batch.commit().addOnCompleteListener { onComplete(it.isSuccessful) }
    }

    fun deleteMessage(chatId: String, messageId: String, isLastMessage: Boolean = false, onComplete: (Boolean) -> Unit) {
        val batch = db.batch()
        val msgRef = db.collection("chats").document(chatId).collection("messages").document(messageId)
        
        batch.delete(msgRef)

        if (isLastMessage) {
            val roomRef = db.collection("chats").document(chatId)
            batch.update(roomRef, "lastMessage", "🚫 Message deleted")
        }

        batch.commit().addOnCompleteListener { onComplete(it.isSuccessful) }
    }

    fun toggleReaction(chatId: String, messageId: String, emoji: String) {
        val currentUserId = auth.currentUser?.uid ?: return
        val msgRef = db.collection("chats").document(chatId).collection("messages").document(messageId)
        
        // In a real app, you'd check if user already reacted with this emoji to remove it
        // For simplicity, we'll just add it here
        msgRef.update("reactions.$emoji", FieldValue.arrayUnion(currentUserId))
    }

    fun setTypingStatus(chatId: String, isTyping: Boolean) {
        val currentUserId = auth.currentUser?.uid ?: return
        db.collection("chats").document(chatId)
            .update("typingStatus.$currentUserId", isTyping)
    }

    fun observeChatRoom(chatId: String, onUpdate: (ChatRoom?) -> Unit): com.google.firebase.firestore.ListenerRegistration {
        return db.collection("chats").document(chatId).addSnapshotListener { snapshot, _ ->
            onUpdate(snapshot?.toObject(ChatRoom::class.java))
        }
    }

    fun createGroup(name: String, description: String, memberIds: List<String>, onComplete: (Boolean, String?) -> Unit) {
        val currentUserId = auth.currentUser?.uid ?: return
        val groupId = db.collection("chats").document().id
        val allMembers = memberIds.toMutableList().apply { if (!contains(currentUserId)) add(currentUserId) }

        val chatRoomData = hashMapOf(
            "chatId" to groupId,
            "participants" to allMembers,
            "lastMessage" to "Group created",
            "isGroup" to true,
            "groupName" to name,
            "lastTimestamp" to FieldValue.serverTimestamp(),
            "unreadCounts" to allMembers.associateWith { 0 }
        )

        db.collection("chats").document(groupId).set(chatRoomData)
            .addOnCompleteListener { task ->
                onComplete(task.isSuccessful, if (task.isSuccessful) groupId else null)
            }
    }
}
