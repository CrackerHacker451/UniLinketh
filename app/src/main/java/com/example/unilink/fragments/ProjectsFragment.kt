package com.example.unilink.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.unilink.activities.ChatActivity
import com.example.unilink.adapters.ProjectAdapter
import com.example.unilink.databinding.FragmentProjectsBinding
import com.example.unilink.models.Project
import com.example.unilink.repository.ProjectRepository
import com.google.firebase.auth.FirebaseAuth

class ProjectsFragment : Fragment() {

    private var _binding: FragmentProjectsBinding? = null
    private val binding get() = _binding!!
    private val projectRepository = ProjectRepository()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProjectsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        fetchProjects()
    }

    private fun setupRecyclerView() {
        binding.rvProjects.layoutManager = LinearLayoutManager(context)
    }

    private fun fetchProjects() {
        projectRepository.getAllProjects { projects ->
            val adapter = ProjectAdapter(projects) { project ->
                handleProjectClick(project)
            }
            binding.rvProjects.adapter = adapter
        }
    }

    private fun handleProjectClick(project: Project) {
        val currentUid = auth.currentUser?.uid ?: return
        if (project.members.contains(currentUid)) {
            // Already a member, maybe open project chat or details
            Toast.makeText(context, "You are a member of ${project.title}", Toast.LENGTH_SHORT).show()
        } else {
            projectRepository.joinProject(project.projectId) { success ->
                if (success) {
                    Toast.makeText(context, "Joined ${project.title} successfully!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Failed to join project", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
