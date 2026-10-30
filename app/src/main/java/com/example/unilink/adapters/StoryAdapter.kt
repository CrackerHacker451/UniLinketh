package com.example.unilink.adapters

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.unilink.activities.ProfileActivity
import com.example.unilink.databinding.ItemStoryBinding
import com.example.unilink.models.Story

class StoryAdapter(private val stories: List<Story>) : RecyclerView.Adapter<StoryAdapter.StoryViewHolder>() {

    class StoryViewHolder(val binding: ItemStoryBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StoryViewHolder {
        val binding = ItemStoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return StoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StoryViewHolder, position: Int) {
        val story = stories[position]
        holder.binding.tvStoryName.text = story.userName
        
        if (story.userId == "1") {
            // "My Story" plus icon simulation
            holder.binding.ivStoryAvatar.setImageResource(android.R.drawable.ic_input_add)
            holder.binding.ivStoryAvatar.setOnClickListener {
                // Launch Create Story (Status)
                val intent = Intent(holder.itemView.context, com.example.unilink.activities.CreatePostActivity::class.java)
                intent.putExtra("type", "story")
                holder.itemView.context.startActivity(intent)
            }
        } else {
            holder.itemView.setOnClickListener {
                if (story.userId.isNotEmpty()) {
                    val intent = Intent(holder.itemView.context, ProfileActivity::class.java)
                    intent.putExtra("userId", story.userId)
                    holder.itemView.context.startActivity(intent)
                }
            }
        }
    }

    override fun getItemCount(): Int = stories.size
}
