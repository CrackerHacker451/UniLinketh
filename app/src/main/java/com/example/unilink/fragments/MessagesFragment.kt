package com.example.unilink.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.unilink.R
import com.example.unilink.activities.ChatActivity
import com.example.unilink.adapters.ChatRoomAdapter
import com.example.unilink.databinding.FragmentMessagesBinding
import com.example.unilink.repository.ChatRepository
import com.google.firebase.auth.FirebaseAuth

class MessagesFragment : Fragment() {

    private var _binding: FragmentMessagesBinding? = null
    private val binding get() = _binding!!
    private val chatRepository = ChatRepository()
    private val userRepository = com.example.unilink.repository.UserRepository()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMessagesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        fetchChats()

        binding.tvNewChatHint.setOnClickListener {
            if (_binding == null) return@setOnClickListener
            // Navigate to Search to find users to chat with
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, SearchFragment())
                .addToBackStack(null)
                .commit()
        }
    }

    private fun setupRecyclerView() {
        if (_binding == null) return
        binding.rvChats.layoutManager = LinearLayoutManager(context)
    }

    private fun fetchChats() {
        chatRepository.getRecentChats { chats ->
            if (_binding == null) return@getRecentChats
            userRepository.getCurrentUser { currentUser ->
                if (_binding == null) return@getCurrentUser
                val blockedByMe = currentUser?.blockedUsers ?: emptyList()
                
                // Filter out chats where the other person is blocked by me
                val filteredChats = chats.filter { room ->
                    if (room.isGroup) true
                    else {
                        val otherId = room.participants.firstOrNull { it != auth.currentUser?.uid }
                        !blockedByMe.contains(otherId)
                    }
                }

                val adapter = ChatRoomAdapter(filteredChats, auth.currentUser?.uid ?: "") { room ->
                    if (_binding == null) return@ChatRoomAdapter
                    val intent = Intent(context, ChatActivity::class.java)
                    intent.putExtra("chatId", room.chatId)
                    intent.putExtra("chatName", if (room.isGroup) room.groupName else "Chat")
                    intent.putExtra("isGroup", room.isGroup)
                    if (!room.isGroup) {
                        val otherUserId = room.participants.firstOrNull { it != auth.currentUser?.uid }
                        intent.putExtra("receiverId", otherUserId)
                    }
                    startActivity(intent)
                }
                binding.rvChats.adapter = adapter
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
