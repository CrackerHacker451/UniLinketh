package com.example.unilink.activities

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.unilink.R
import com.example.unilink.adapters.ProjectAdapter
import com.example.unilink.databinding.ActivityFindTeamBinding
import com.example.unilink.models.Project
import com.example.unilink.repository.ProjectRepository
import com.example.unilink.utils.ThemeUtils
import com.example.unilink.web3.ArkivSwarmClient
import com.example.unilink.web3.WorldIdAgentPassManager
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class FindTeamActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFindTeamBinding
    private val projectRepository = ProjectRepository()
    private val auth = FirebaseAuth.getInstance()
    private var allProjects = listOf<Project>()

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeUtils.applyTheme(this)
        super.onCreate(savedInstanceState)
        binding = ActivityFindTeamBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        setupSearch()
        setupCategories()
        fetchTeams()

        binding.fabCreateTeam.setOnClickListener {
            startActivity(Intent(this, CreateTeamActivity::class.java))
        }
    }

    private fun setupSearch() {
        binding.etSearchTeam.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterProjects(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupCategories() {
        binding.catAll.setOnClickListener { filterByCategory(null) }
        binding.catHackathon.setOnClickListener { filterByCategory("Hackathon") }
        binding.catStartup.setOnClickListener { filterByCategory("Startup") }
        binding.catStudy.setOnClickListener { filterByCategory("Study") }
    }

    private fun fetchTeams() {
        binding.progressBar.visibility = View.VISIBLE
        projectRepository.getAllProjects { projects ->
            binding.progressBar.visibility = View.GONE
            allProjects = projects
            updateRecyclerView(projects)
        }
    }

    private fun filterProjects(query: String) {
        val filtered = allProjects.filter {
            it.title.contains(query, ignoreCase = true) ||
            it.description.contains(query, ignoreCase = true) ||
            (it.ensSubname?.contains(query, ignoreCase = true) == true) ||
            it.tags.any { tag -> tag.contains(query, ignoreCase = true) } ||
            it.skillsRequired.any { skill -> skill.contains(query, ignoreCase = true) }
        }
        updateRecyclerView(filtered)
    }

    private fun filterByCategory(category: String?) {
        val inactiveBg = R.drawable.bg_quick_action
        val activeBg = R.drawable.bg_neon_ring

        binding.catAll.setBackgroundResource(if (category == null) activeBg else inactiveBg)
        binding.catHackathon.setBackgroundResource(if (category == "Hackathon") activeBg else inactiveBg)
        binding.catStartup.setBackgroundResource(if (category == "Startup") activeBg else inactiveBg)
        binding.catStudy.setBackgroundResource(if (category == "Study") activeBg else inactiveBg)

        lifecycleScope.launch {
            val sqlFilter = ArkivSwarmClient.queryActiveSquadsSql(category, minOpenSeats = 1)
            Toast.makeText(this@FindTeamActivity, "Arkiv Query: $sqlFilter", Toast.LENGTH_SHORT).show()
        }

        if (category == null) {
            updateRecyclerView(allProjects)
        } else {
            val filtered = allProjects.filter { project ->
                project.tags.any { it.contains(category, ignoreCase = true) }
            }
            updateRecyclerView(filtered)
        }
    }

    private fun updateRecyclerView(projects: List<Project>) {
        val adapter = ProjectAdapter(projects) { project ->
            handleProjectClick(project)
        }
        binding.rvTeams.adapter = adapter
    }

    private fun handleProjectClick(project: Project) {
        val currentUid = auth.currentUser?.uid ?: return
        if (project.members.contains(currentUid)) {
            Toast.makeText(this, "You are already in this team!", Toast.LENGTH_SHORT).show()
            return
        }

        // Gate scarce hackathon squad seats behind World ID v4 (IDKit) Proof of Human
        WorldIdAgentPassManager.showIdKitVerificationDialog(
            context = this,
            action = "join-squad-${project.projectId.ifEmpty { "default" }}",
            signal = currentUid
        ) { result ->
            if (result.success) {
                projectRepository.joinProject(project.projectId) { success ->
                    if (success) {
                        Toast.makeText(
                            this,
                            "World ID Verified! Joined ${project.ensSubname ?: project.title}",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        Toast.makeText(this, "Failed to join team", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this, result.message, Toast.LENGTH_LONG).show()
            }
        }
    }
}
