package com.example.unilink.activities

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.unilink.databinding.ActivityCreateTeamBinding
import com.example.unilink.repository.ProjectRepository
import com.example.unilink.utils.ThemeUtils
import com.example.unilink.web3.WorldIdAgentPassManager

class CreateTeamActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreateTeamBinding
    private val projectRepository = ProjectRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeUtils.applyTheme(this)
        super.onCreate(savedInstanceState)
        binding = ActivityCreateTeamBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.btnCreateTeam.setOnClickListener {
            createTeamWithWeb3Verification()
        }
    }

    private fun createTeamWithWeb3Verification() {
        val title = binding.etTeamTitle.text.toString().trim()
        val description = binding.etTeamDescription.text.toString().trim()
        val skillsString = binding.etRequiredSkills.text.toString().trim()
        val tagsString = binding.etTags.text.toString().trim()

        if (title.isEmpty()) {
            binding.etTeamTitle.error = "Title required"
            return
        }
        if (description.isEmpty()) {
            binding.etTeamDescription.error = "Description required"
            return
        }

        val skills = skillsString.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val tags = tagsString.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            .ifEmpty { listOf("Hackathon", "Web3 / Crypto") }

        // Require World ID Agent Pass step-up verification before deploying a Squad to Arkiv + ENSv2
        WorldIdAgentPassManager.requireAgentPassStepUp(
            context = this,
            protectedActionName = "DEPLOY_SQUAD_${title.uppercase()}",
            onAuthorized = { pairwiseSub, _ ->
                binding.btnCreateTeam.isEnabled = false
                projectRepository.createProject(
                    title = title,
                    description = description,
                    skills = skills,
                    tags = tags,
                    worldIdNullifier = pairwiseSub
                ) { success ->
                    if (success) {
                        Toast.makeText(
                            this,
                            "Squad synced to Arkiv (#7738577) + Swarm & ENSv2 minted!",
                            Toast.LENGTH_LONG
                        ).show()
                        finish()
                    } else {
                        binding.btnCreateTeam.isEnabled = true
                        Toast.makeText(this, "Failed to create team", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onBlocked = { _, reason ->
                Toast.makeText(this, reason, Toast.LENGTH_LONG).show()
            }
        )
    }
}
