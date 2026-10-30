package com.example.unilink.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.unilink.databinding.ItemBlockedUserBinding
import com.example.unilink.models.User

class BlockedUserAdapter(
    private var users: List<User>,
    private val onUnblockClick: (User) -> Unit
) : RecyclerView.Adapter<BlockedUserAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemBlockedUserBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemBlockedUserBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = users[position]
        holder.binding.tvName.text = user.name
        holder.binding.tvUsername.text = "@${user.username}"
        
        holder.binding.btnUnblock.setOnClickListener { onUnblockClick(user) }
    }

    override fun getItemCount(): Int = users.size

    fun updateList(newList: List<User>) {
        users = newList
        notifyDataSetChanged()
    }
}
