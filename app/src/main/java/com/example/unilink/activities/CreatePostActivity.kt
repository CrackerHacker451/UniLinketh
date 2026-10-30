package com.example.unilink.activities

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.unilink.databinding.ActivityCreatePostBinding
import com.example.unilink.repository.PostRepository
import com.example.unilink.repository.StoryRepository
import com.example.unilink.utils.ThemeUtils

class CreatePostActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreatePostBinding
    private val postRepository = PostRepository()
    private val storyRepository = StoryRepository()
    private val userRepository = com.example.unilink.repository.UserRepository()
    private var selectedImageUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeUtils.applyTheme(this)
        super.onCreate(savedInstanceState)
        binding = ActivityCreatePostBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val type = intent.getStringExtra("type") ?: "post"
        
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.title = if (type == "story") "Post Status" else "Create Post"
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.btnPost.text = if (type == "story") "Post Status" else "Post"
        binding.etPostContent.hint = if (type == "story") "Type your status..." else "What's on your mind?"

        binding.btnAddPhoto.setOnClickListener {
            userRepository.getCurrentUser { user ->
                if (user?.isPremium == true) {
                    // Mock selecting an image
                    selectedImageUrl = "https://picsum.photos/800/600" // Placeholder
                    binding.cardPremiumPhoto.visibility = android.view.View.VISIBLE
                    Toast.makeText(this, "Photo added!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Adding photos is a Premium feature! ✨", Toast.LENGTH_LONG).show()
                }
            }
        }

        binding.btnRemoveImage.setOnClickListener {
            selectedImageUrl = null
            binding.cardPremiumPhoto.visibility = android.view.View.GONE
        }

        binding.btnPost.setOnClickListener {
            val content = binding.etPostContent.text.toString().trim()
            if (content.isNotEmpty() || selectedImageUrl != null) {
                binding.btnPost.isEnabled = false
                binding.btnPost.text = "Publishing..."
                
                if (type == "story") {
                    // For stories we use content as imageUrl for simplicity in this demo
                    storyRepository.uploadStory(content) { success ->
                        binding.btnPost.isEnabled = true
                        binding.btnPost.text = "Post Status"
                        if (success) {
                            Toast.makeText(this, "Status Posted!", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                    }
                } else {
                    postRepository.createPost(content, selectedImageUrl) { success ->
                        binding.btnPost.isEnabled = true
                        binding.btnPost.text = "Post"
                        if (success) {
                            Toast.makeText(this, "Posted!", Toast.LENGTH_SHORT).show()
                            finish()
                        } else {
                            Toast.makeText(this, "Failed to post", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }
}
