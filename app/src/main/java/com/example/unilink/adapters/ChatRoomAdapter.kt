package com.example.unilink.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.unilink.databinding.ItemChatRoomBinding
import com.example.unilink.models.ChatRoom
import com.example.unilink.repository.UserRepository

class ChatRoomAdapter(
    private var chatRooms: List<ChatRoom>,
    private val currentUserId: String,
    private val onItemClick: (ChatRoom) -> Unit
) : RecyclerView.Adapter<ChatRoomAdapter.ChatRoomViewHolder>() {

    class ChatRoomViewHolder(val binding: ItemChatRoomBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatRoomViewHolder {
        val binding = ItemChatRoomBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ChatRoomViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChatRoomViewHolder, position: Int) {
        val room = chatRooms[position]
        
        if (room.isGroup) {
            holder.binding.tvChatName.text = room.groupName
        } else {
            val otherUserId = room.participants.firstOrNull { it != currentUserId }
            if (otherUserId != null) {
                UserRepository().getUserById(otherUserId) { user ->
                    holder.binding.tvChatName.text = user?.name ?: "User"
                }
            } else {
                holder.binding.tvChatName.text = "Private Chat"
            }
        }
        
        holder.binding.tvLastMessage.text = room.lastMessage
        
        val sdf = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
        holder.binding.tvChatTime.text = room.lastTimestamp?.let { sdf.format(it) } ?: ""
        
        // Unread logic from production model
        val unreadCount = room.unreadCounts[currentUserId] ?: 0
        if (unreadCount > 0) {
            holder.binding.tvUnreadCount.visibility = android.view.View.VISIBLE
            holder.binding.tvUnreadCount.text = unreadCount.toString()
        } else {
            holder.binding.tvUnreadCount.visibility = android.view.View.GONE
        }
        
        holder.itemView.setOnClickListener { onItemClick(room) }
    }

    override fun getItemCount(): Int = chatRooms.size

    fun updateList(newList: List<ChatRoom>) {
        chatRooms = newList
        notifyDataSetChanged()
    }
}
