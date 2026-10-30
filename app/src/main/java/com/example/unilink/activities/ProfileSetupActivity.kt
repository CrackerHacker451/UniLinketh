package com.example.unilink.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.unilink.databinding.ActivityProfileSetupBinding
import com.example.unilink.models.User
import com.example.unilink.repository.UserRepository
import com.example.unilink.utils.ThemeUtils
import com.google.firebase.auth.FirebaseAuth

class ProfileSetupActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileSetupBinding
    private val auth = FirebaseAuth.getInstance()
    private val userRepository = UserRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeUtils.applyTheme(this)
        super.onCreate(savedInstanceState)
        binding = ActivityProfileSetupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.rgUserType.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == binding.rbCollege.id) {
                binding.llCollegeFields.visibility = View.VISIBLE
                binding.llSchoolFields.visibility = View.GONE
            } else {
                binding.llCollegeFields.visibility = View.GONE
                binding.llSchoolFields.visibility = View.VISIBLE
            }
        }

        binding.btnGetLocation.setOnClickListener {
            // Mock location detection
            binding.etCity.setText("New York")
            binding.etState.setText("NY")
            Toast.makeText(this, "City detected: New York", Toast.LENGTH_SHORT).show()
        }

        binding.btnSaveProfile.setOnClickListener {
            saveProfile()
        }
    }

    private fun saveProfile() {
        val uid = auth.currentUser?.uid ?: return
        val name = binding.etFullName.text.toString().trim()
        val username = binding.etUsername.text.toString().trim()
        val userType = if (binding.rbCollege.isChecked) "College Student" else "School Student"
        val bio = binding.etBio.text.toString().trim()
        val skillsStr = binding.etSkills.text.toString().trim()
        val goals = binding.etGoals.text.toString().trim()
        val phoneNumber = binding.etPhone.text.toString().trim()
        val city = binding.etCity.text.toString().trim()
        val state = binding.etState.text.toString().trim()

        if (name.isEmpty() || username.isEmpty() || city.isEmpty() || state.isEmpty()) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show()
            return
        }

        val skillsList = if (skillsStr.isNotEmpty()) skillsStr.split(",").map { it.trim() } else emptyList()

        userRepository.isUsernameUnique(username) { isUnique ->
            if (!isUnique) {
                binding.etUsername.error = "Username already taken"
                return@isUsernameUnique
            }

            val baseUser = User(
                uid = uid,
                name = name,
                username = username,
                email = auth.currentUser?.email ?: "",
                userType = userType,
                city = city,
                state = state,
                bio = bio,
                phoneNumber = if (phoneNumber.isNotEmpty()) phoneNumber else null,
                skills = skillsList,
                goals = goals,
                blockedUsers = emptyList(),
                linkers = emptyList(),
                linked = emptyList()
            )

            val user = if (userType == "College Student") {
                val collegeName = binding.etCollegeName.text.toString().trim()
                val branch = binding.etBranch.text.toString().trim()
                val year = binding.etYear.text.toString().trim()
                baseUser.copy(
                    collegeName = collegeName,
                    branch = branch,
                    yearOrSemester = year
                )
            } else {
                val schoolName = binding.etSchoolName.text.toString().trim()
                val className = binding.etClass.text.toString().trim()
                val stream = binding.etStream.text.toString().trim()
                baseUser.copy(
                    schoolName = schoolName,
                    branch = stream,
                    yearOrSemester = className
                )
            }

            userRepository.saveUser(user) { success ->
                if (success) {
                    Toast.makeText(this, "Profile saved successfully", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this, "Error saving profile", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
