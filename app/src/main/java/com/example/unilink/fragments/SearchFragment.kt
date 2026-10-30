package com.example.unilink.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.unilink.R
import com.example.unilink.activities.ProfileActivity
import com.example.unilink.adapters.StudentGridAdapter
import com.example.unilink.databinding.FragmentSearchBinding
import com.example.unilink.models.User

class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!
    private val userRepository = com.example.unilink.repository.UserRepository()
    private var allUsers = listOf<User>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupGrid()
        setupCategoryClickListeners()
        setupSearchListener()
    }

    private fun setupSearchListener() {
        binding.etSearchStudents.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (_binding == null) return
                val query = s.toString().trim()
                if (query.isEmpty()) {
                    updateGrid(allUsers)
                } else {
                    val filtered = allUsers.filter { 
                        it.name.contains(query, ignoreCase = true) || 
                        it.username.contains(query, ignoreCase = true) ||
                        it.branch?.contains(query, ignoreCase = true) == true ||
                        it.skills?.any { skill -> skill.contains(query, ignoreCase = true) } == true
                    }
                    updateGrid(filtered)
                }
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
    }

    private fun setupGrid() {
        userRepository.getAllUsers { users ->
            if (_binding == null) return@getAllUsers
            // Show all users for suggestion as requested
            allUsers = users.shuffled()
            updateGrid(allUsers)
        }
    }

    private fun updateGrid(users: List<User>) {
        if (_binding == null) return
        binding.rvStudentGrid.adapter = StudentGridAdapter(users) { user ->
            val intent = Intent(context, ProfileActivity::class.java)
            intent.putExtra("userId", user.uid)
            startActivity(intent)
        }
    }

    private fun setupCategoryClickListeners() {
        binding.catAll.setOnClickListener { filterUsers(null) }
        binding.catEngineering.setOnClickListener { filterUsers("Engineering") }
        binding.catDesign.setOnClickListener { filterUsers("Design") }
        binding.catMedical.setOnClickListener { filterUsers("Medical") }
    }

    private fun filterUsers(category: String?) {
        if (_binding == null) return
        
        val defaultBg = R.drawable.bg_category_inactive
        val activeBg = R.drawable.bg_neon_ring
        
        binding.catAll.setBackgroundResource(if (category == null) activeBg else defaultBg)
        binding.catEngineering.setBackgroundResource(if (category == "Engineering") activeBg else defaultBg)
        binding.catDesign.setBackgroundResource(if (category == "Design") activeBg else defaultBg)
        binding.catMedical.setBackgroundResource(if (category == "Medical") activeBg else defaultBg)

        if (category == null) {
            updateGrid(allUsers)
        } else {
            val filtered = allUsers.filter { 
                it.branch?.contains(category, ignoreCase = true) == true || 
                it.skills?.any { skill -> skill.contains(category, ignoreCase = true) } == true
            }
            updateGrid(filtered)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
