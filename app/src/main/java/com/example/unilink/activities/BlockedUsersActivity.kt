package com.example.unilink.activities

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.unilink.adapters.BlockedUserAdapter
import com.example.unilink.databinding.ActivityBlockedUsersBinding
import com.example.unilink.models.User
import com.example.unilink.repository.UserRepository

class BlockedUsersActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBlockedUsersBinding
    private val userRepository = UserRepository()
    private lateinit var adapter: BlockedUserAdapter
    private var blockedUsers = mutableListOf<User>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBlockedUsersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        setupRecyclerView()
        fetchBlockedUsers()
    }

    private fun setupRecyclerView() {
        adapter = BlockedUserAdapter(blockedUsers) { user ->
            unblockUser(user)
        }
        binding.rvBlockedUsers.layoutManager = LinearLayoutManager(this)
        binding.rvBlockedUsers.adapter = adapter
    }

    private fun fetchBlockedUsers() {
        userRepository.getCurrentUser { currentUser ->
            val blockedIds = currentUser?.blockedUsers ?: emptyList()
            if (blockedIds.isEmpty()) {
                blockedUsers.clear()
                adapter.updateList(blockedUsers)
                Toast.makeText(this, "No blocked users", Toast.LENGTH_SHORT).show()
                return@getCurrentUser
            }

            // This is slightly inefficient but works for smaller blocked lists
            val tempUsers = mutableListOf<User>()
            var count = 0
            blockedIds.forEach { id ->
                userRepository.getUserById(id) { user ->
                    user?.let { tempUsers.add(it) }
                    count++
                    if (count == blockedIds.size) {
                        blockedUsers.clear()
                        blockedUsers.addAll(tempUsers)
                        adapter.updateList(blockedUsers)
                    }
                }
            }
        }
    }

    private fun unblockUser(user: User) {
        userRepository.unblockUser(user.uid) { success ->
            if (success) {
                Toast.makeText(this, "${user.name} unblocked", Toast.LENGTH_SHORT).show()
                fetchBlockedUsers()
            } else {
                Toast.makeText(this, "Failed to unblock", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
