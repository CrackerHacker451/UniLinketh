package com.example.unilink.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.unilink.databinding.ItemFocusRoomCardBinding
import com.example.unilink.models.FocusRoom

class FocusRoomAdapter(private val rooms: List<FocusRoom>) : RecyclerView.Adapter<FocusRoomAdapter.FocusViewHolder>() {

    class FocusViewHolder(val binding: ItemFocusRoomCardBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FocusViewHolder {
        val binding = ItemFocusRoomCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FocusViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FocusViewHolder, position: Int) {
        val room = rooms[position]
        holder.binding.tvRoomName.text = room.name
        holder.binding.tvRoomName.setTextColor(Color.parseColor(room.color))
        
        holder.itemView.setOnClickListener {
            // Handle join click
        }
    }

    override fun getItemCount(): Int = rooms.size
}
