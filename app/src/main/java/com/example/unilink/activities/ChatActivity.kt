package com.example.unilink.activities

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.unilink.R
import com.example.unilink.adapters.ChatAdapter
import com.example.unilink.databinding.ActivityChatBinding
import com.example.unilink.models.ChatMessage
import com.example.unilink.utils.ThemeUtils
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.FirebaseAuth

class ChatActivity : AppCompatActivity() {

    private var _binding: ActivityChatBinding? = null
    private val binding get() = _binding!!
    
    private val auth = FirebaseAuth.getInstance()
    private val chatRepository = com.example.unilink.repository.ChatRepository()
    private val userRepository = com.example.unilink.repository.UserRepository()
    
    private lateinit var adapter: ChatAdapter
    private var messageList = mutableListOf<ChatMessage>()
    private var chatId: String? = null
    private var receiverId: String? = null
    private var isGroup: Boolean = false
    private var currentUsername: String = "User"
    
    private var chatRoomListener: com.google.firebase.firestore.ListenerRegistration? = null
    private var replyToMessage: ChatMessage? = null
    private var editingMessage: ChatMessage? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeUtils.applyTheme(this)
        super.onCreate(savedInstanceState)
        _binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        chatId = intent.getStringExtra("chatId")
        receiverId = intent.getStringExtra("receiverId")
        val chatName = intent.getStringExtra("chatName")
        isGroup = intent.getBooleanExtra("isGroup", false)

        if (auth.currentUser == null) { finish(); return }
        if (chatId == null && receiverId != null) chatId = chatRepository.getChatId(auth.currentUser!!.uid, receiverId!!)

        setupToolbar(chatName)
        setupRecyclerView()
        setupInputLogic()
        setupQuickActions()
        listenForMessages()
        listenForTyping()
        fetchCurrentUsername()
        setupCallButton()
        loadPremiumWallpaper()
        
        chatId?.let { chatRepository.markChatAsRead(it) }
    }

    private fun loadPremiumWallpaper() {
        userRepository.getCurrentUser { user ->
            if (_binding == null) return@getCurrentUser
            if (user?.isPremium == true) {
                binding.ivChatWallpaper.visibility = View.VISIBLE
                val wallpaperResId = user.chatWallpaperUrl?.toIntOrNull() ?: R.drawable.bg_mesh_gradient
                if (wallpaperResId != -1) {
                    binding.ivChatWallpaper.setImageResource(wallpaperResId)
                } else {
                    binding.ivChatWallpaper.visibility = View.GONE
                }
            }
        }
    }

    private fun setupToolbar(name: String?) {
        binding.tvChatReceiverName.text = name ?: "Chat"
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        binding.toolbar.setNavigationOnClickListener { finish() }
        
        receiverId?.let { id ->
            userRepository.getUserById(id) { user ->
                if (_binding != null && user != null) {
                    binding.tvChatReceiverName.text = user.name
                    // In real app, load avatar: binding.ivChatReceiverAvatar.load(user.profileImageUrl)
                }
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = ChatAdapter(
            messageList, 
            auth.currentUser!!.uid,
            onMessageClick = { msg, _ -> showMessageActions(msg) },
            onMessageDoubleTap = { msg -> showReactionPicker(msg) },
            onPollVote = { _, _ -> /* Update poll logic */ }
        )
        binding.rvMessages.apply {
            layoutManager = LinearLayoutManager(this@ChatActivity).apply { stackFromEnd = true }
            adapter = this@ChatActivity.adapter
        }
        setupSwipeToReply()
    }

    private fun setupSwipeToReply() {
        val swipeCallback = object : androidx.recyclerview.widget.ItemTouchHelper.SimpleCallback(0, androidx.recyclerview.widget.ItemTouchHelper.RIGHT) {
            override fun onMove(rv: androidx.recyclerview.widget.RecyclerView, vh: androidx.recyclerview.widget.RecyclerView.ViewHolder, target: androidx.recyclerview.widget.RecyclerView.ViewHolder): Boolean = false
            override fun onSwiped(viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val message = messageList[position]
                enterReplyMode(message)
                adapter.notifyItemChanged(position)
            }
        }
        androidx.recyclerview.widget.ItemTouchHelper(swipeCallback).attachToRecyclerView(binding.rvMessages)
    }

    private fun setupInputLogic() {
        binding.etMessage.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val hasText = !s.isNullOrBlank()
                binding.btnSend.visibility = if (hasText) View.VISIBLE else View.GONE
                binding.btnVoice.visibility = if (hasText) View.GONE else View.VISIBLE
                
                chatId?.let { id ->
                    chatRepository.setTypingStatus(id, hasText)
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.btnSend.setOnClickListener { sendMessage() }
        binding.btnVoice.setOnClickListener { 
            userRepository.getCurrentUser { user ->
                if (user?.isPremium == true) {
                    Toast.makeText(this, "🎙️ Sending voice message...", Toast.LENGTH_SHORT).show()
                    chatId?.let { id ->
                        receiverId?.let { rid ->
                            chatRepository.sendVoiceMessage(id, rid) { success ->
                                if (success) Toast.makeText(this, "Voice message sent!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } else {
                    Toast.makeText(this, "Voice messages are a Premium feature! ✨", Toast.LENGTH_LONG).show()
                }
            }
        }
        binding.btnCloseReply.setOnClickListener { cancelReply() }
    }

    private fun setupQuickActions() {
        binding.qaPoll.setOnClickListener { Toast.makeText(this, "📊 Live Poll Sheet", Toast.LENGTH_SHORT).show() }
        binding.qaDoubt.setOnClickListener { Toast.makeText(this, "🧠 Doubt Drop Sheet", Toast.LENGTH_SHORT).show() }
        binding.qaToken.setOnClickListener { Toast.makeText(this, "💰 Token Transfer Sheet", Toast.LENGTH_SHORT).show() }
        binding.qaCode.setOnClickListener { binding.etMessage.append("```\n\n```") }
    }

    private fun showMessageActions(msg: ChatMessage) {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.dialog_message_actions, null)
        
        val isMyMessage = msg.senderId == auth.currentUser?.uid
        val isText = msg.type == "text"

        // Hide Edit for received messages or non-text messages
        view.findViewById<View>(R.id.actionEdit).apply {
            visibility = if (isMyMessage && isText) View.VISIBLE else View.GONE
            setOnClickListener {
                enterEditMode(msg)
                dialog.dismiss()
            }
        }

        // Hide Delete for received messages
        view.findViewById<View>(R.id.actionDelete).apply {
            visibility = if (isMyMessage) View.VISIBLE else View.GONE
            setOnClickListener {
                chatId?.let { id ->
                    val isLast = messageList.lastOrNull()?.messageId == msg.messageId
                    chatRepository.deleteMessage(id, msg.messageId, isLast) { success ->
                        if (success) Toast.makeText(this@ChatActivity, "Message Deleted", Toast.LENGTH_SHORT).show()
                        else Toast.makeText(this@ChatActivity, "Failed to delete", Toast.LENGTH_SHORT).show()
                    }
                }
                dialog.dismiss()
            }
        }

        view.findViewById<View>(R.id.actionReply).setOnClickListener {
            enterReplyMode(msg)
            dialog.dismiss()
        }

        view.findViewById<View>(R.id.actionCopy).setOnClickListener {
            val clipboard = getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("message", msg.messageText)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Text copied to clipboard", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }
        
        dialog.setContentView(view)
        dialog.show()
    }

    private fun showReactionPicker(msg: ChatMessage) {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.dialog_reaction_picker, null)
        
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                val child = view.getChildAt(i)
                if (child is android.widget.TextView) {
                    child.setOnClickListener {
                        chatId?.let { id ->
                            chatRepository.toggleReaction(id, msg.messageId, child.text.toString())
                        }
                        dialog.dismiss()
                    }
                }
            }
        }

        dialog.setContentView(view)
        dialog.show()
    }

    private fun enterReplyMode(msg: ChatMessage) {
        replyToMessage = msg
        editingMessage = null
        binding.replyPreview.visibility = View.VISIBLE
        binding.tvReplyTarget.text = "Replying to: ${msg.messageText}"
        binding.etMessage.requestFocus()
    }

    private fun enterEditMode(msg: ChatMessage) {
        editingMessage = msg
        replyToMessage = null
        binding.replyPreview.visibility = View.VISIBLE
        binding.tvReplyTarget.text = "Editing: ${msg.messageText}"
        binding.etMessage.setText(msg.messageText)
        binding.etMessage.setSelection(msg.messageText.length)
        binding.etMessage.requestFocus()
        binding.btnSend.setImageResource(android.R.drawable.ic_menu_edit) // Visual indicator for edit
    }

    private fun cancelReply() {
        replyToMessage = null
        editingMessage = null
        binding.replyPreview.visibility = View.GONE
        binding.btnSend.setImageResource(android.R.drawable.ic_menu_send) // Reset icon
    }

    private fun sendMessage() {
        val text = binding.etMessage.text.toString().trim()
        if (text.isEmpty()) return

        chatId?.let { id ->
            val currentEdit = editingMessage
            val currentReply = replyToMessage
            
            binding.etMessage.text.clear()
            cancelReply()

            if (currentEdit != null) {
                val isLast = messageList.lastOrNull()?.messageId == currentEdit.messageId
                chatRepository.editMessage(id, currentEdit.messageId, text, isLast) { success ->
                    if (!success) Toast.makeText(this, "Failed to edit", Toast.LENGTH_SHORT).show()
                }
            } else {
                chatRepository.sendMessage(
                    chatId = id,
                    text = text,
                    senderUsername = currentUsername,
                    isGroup = isGroup,
                    receiverId = receiverId,
                    replyToId = currentReply?.messageId,
                    replyToText = currentReply?.messageText
                ) { success ->
                    if (!success) Toast.makeText(this, "Failed to send", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun listenForTyping() {
        chatId?.let { id ->
            chatRoomListener?.remove()
            chatRoomListener = chatRepository.observeChatRoom(id) { room ->
                if (_binding == null || room == null) return@observeChatRoom
                
                val typingUsers = room.typingStatus.filter { it.value && it.key != auth.currentUser?.uid }
                if (typingUsers.isNotEmpty()) {
                    binding.tvChatReceiverStatus.text = "Typing..."
                    binding.tvChatReceiverStatus.setTextColor(android.graphics.Color.parseColor("#EC4899"))
                } else {
                    binding.tvChatReceiverStatus.text = "Online"
                    binding.tvChatReceiverStatus.setTextColor(android.graphics.Color.parseColor("#22D3EE"))
                }
            }
        }
    }

    private fun listenForMessages() {
        chatId?.let { id ->
            chatRepository.listenForMessages(id) { messages ->
                // Sort by timestamp, putting new messages (null timestamp) at the end
                val sorted = messages.sortedBy { it.timestamp?.time ?: Long.MAX_VALUE }
                messageList.clear()
                messageList.addAll(sorted)
                adapter.notifyDataSetChanged()
                if (messageList.isNotEmpty()) {
                    binding.rvMessages.scrollToPosition(messageList.size - 1)
                    
                    val lastMsg = messageList.last()
                    if (lastMsg.senderId != auth.currentUser?.uid) {
                        chatRepository.markChatAsRead(id)
                    }
                }
            }
        }
    }

    private fun fetchCurrentUsername() {
        userRepository.getCurrentUser { user -> user?.let { currentUsername = it.name.ifBlank { it.username } } }
    }

    private fun setupCallButton() {
        if (isGroup) return
        receiverId?.let { id ->
            userRepository.getUserById(id) { user ->
                user?.phoneNumber?.let { phone ->
                    binding.btnCall.visibility = View.VISIBLE
                    binding.btnCall.setOnClickListener {
                        val intent = Intent(Intent.ACTION_DIAL).apply { data = android.net.Uri.parse("tel:$phone") }
                        startActivity(intent)
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        chatRoomListener?.remove()
        chatId?.let { chatRepository.setTypingStatus(it, false) }
        _binding = null
    }
}
