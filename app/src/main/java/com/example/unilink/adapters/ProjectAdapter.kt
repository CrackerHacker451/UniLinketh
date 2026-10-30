package com.example.unilink.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.unilink.databinding.ItemProjectBinding
import com.example.unilink.models.Project

class ProjectAdapter(
    private val projects: List<Project>,
    private val onItemClick: (Project) -> Unit
) : RecyclerView.Adapter<ProjectAdapter.ProjectViewHolder>() {

    class ProjectViewHolder(val binding: ItemProjectBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProjectViewHolder {
        val binding = ItemProjectBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ProjectViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProjectViewHolder, position: Int) {
        val project = projects[position]
        val ensBadge = project.ensSubname ?: "${project.title.lowercase().replace(" ", "-")}.unilink.eth"
        holder.binding.tvProjectTitle.text = "${project.title} · $ensBadge"
        holder.binding.tvProjectDescription.text = buildString {
            append(project.description)
            if (!project.arkivEntityKey.isNullOrEmpty()) {
                append("\n[Arkiv: ${project.arkivEntityKey} · Swarm AES-GCM · HCS ${project.hederaHcsTopicId}]")
            }
        }
        holder.binding.tvProjectStatus.text = if (project.worldIdVerifiedLead) "🪪 World ID · ${project.status}" else project.status
        holder.binding.tvProgressPercent.text = "Progress: ${project.progress}%"
        holder.binding.projectProgress.progress = project.progress
        holder.binding.tvTeamCount.text = "${project.members.size}/${project.maxTeamSize}"

        holder.binding.btnOpen.setOnClickListener { onItemClick(project) }
        holder.itemView.setOnClickListener { onItemClick(project) }
    }

    override fun getItemCount(): Int = projects.size
}
