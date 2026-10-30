package com.example.unilink.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.unilink.databinding.ItemStudentCardBinding
import com.example.unilink.models.User

class StudentGridAdapter(
    private val users: List<User>,
    private val onItemClick: (User) -> Unit
) : RecyclerView.Adapter<StudentGridAdapter.GridViewHolder>() {

    class GridViewHolder(val binding: ItemStudentCardBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GridViewHolder {
        val binding = ItemStudentCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return GridViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GridViewHolder, position: Int) {
        val user = users[position]
        holder.binding.tvStudentCardName.text = user.name
        holder.binding.tvStudentCardMajor.text = user.branch ?: "Student"
        holder.binding.tvStudentStatus.text = user.status
        holder.binding.tvStudentSkills.text = if (!user.skills.isNullOrEmpty()) user.skills.joinToString(", ") else "Networking"
        
        holder.itemView.setOnClickListener { onItemClick(user) }
        holder.binding.btnConnect.setOnClickListener { onItemClick(user) }
    }

    override fun getItemCount(): Int = users.size
}
