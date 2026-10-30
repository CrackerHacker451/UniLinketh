package com.example.unilink.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.unilink.R
import com.example.unilink.databinding.ActivityProfileBinding
import com.example.unilink.databinding.DialogPremiumCustomizeBinding
import com.example.unilink.models.User
import com.example.unilink.utils.ThemeUtils
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration

class ProfileActivity : AppCompatActivity() {

    private var _binding: ActivityProfileBinding? = null
    private val binding get() = _binding!!
    
    private val auth = FirebaseAuth.getInstance()
    private val userRepository = com.example.unilink.repository.UserRepository()
    private val chatRepository = com.example.unilink.repository.ChatRepository()

    private var profileListener: ListenerRegistration? = null
    private var currentUserListener: ListenerRegistration? = null
    private var currentUid: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeUtils.applyTheme(this)
        super.onCreate(savedInstanceState)
        try {
            _binding = ActivityProfileBinding.inflate(layoutInflater)
            setContentView(binding.root)

            setSupportActionBar(binding.toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            binding.toolbar.setNavigationOnClickListener { finish() }

            val targetUserId = intent.getStringExtra("userId")
            currentUid = auth.currentUser?.uid

            if (targetUserId != null && targetUserId != currentUid) {
                // Viewing someone else's profile
                binding.btnEditProfile.visibility = View.GONE
                binding.btnRedeemPremium.visibility = View.GONE
                binding.btnCustomizePremium.visibility = View.GONE
                binding.llProfileActions.visibility = View.VISIBLE
                binding.btnLogout.visibility = View.GONE
                binding.btnSettings.visibility = View.GONE
                binding.btnMore.visibility = View.VISIBLE
                
                observeOtherProfile(targetUserId)
                setupLinkButton(targetUserId)
                setupMoreMenu(targetUserId)
                
                binding.btnMessage.setOnClickListener {
                    startChatWithUser(targetUserId)
                }
            } else {
                // Viewing my own profile
                binding.btnEditProfile.visibility = View.VISIBLE
                binding.btnEditProfile.text = "Edit Profile"
                binding.llProfileActions.visibility = View.GONE
                binding.btnLogout.visibility = View.VISIBLE
                binding.btnSettings.visibility = View.VISIBLE
                binding.btnMore.visibility = View.GONE
                
                observeCurrentProfile()
                
                binding.btnEditProfile.setOnClickListener {
                    startActivity(Intent(this, ProfileSetupActivity::class.java))
                }

                binding.btnRedeemPremium.setOnClickListener {
                    showProfessionalPremiumDialog()
                }

                binding.btnCustomizePremium.setOnClickListener {
                    showPremiumCustomizationDialog()
                }
            }

            binding.btnSettings.setOnClickListener {
                startActivity(Intent(this, SettingsActivity::class.java))
            }

            binding.btnLogout.setOnClickListener {
                auth.signOut()
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
        } catch (e: Exception) {
            android.util.Log.e("ProfileActivity", "Error in onCreate: ${e.message}")
            finish()
        }
    }

    private fun showProfessionalPremiumDialog() {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.dialog_premium_purchase, null)
        dialog.setContentView(view)

        view.findViewById<View>(R.id.btnUpgradeNow).setOnClickListener {
            // Professional payment simulation
            Toast.makeText(this, "Settling 0.20 USDC via Hedera x402 Facilitator (0 HBAR gas)...", Toast.LENGTH_SHORT).show()
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                userRepository.activatePremium { success ->
                    if (success) {
                        Toast.makeText(this, "UniLink Pro Unlocked via Hedera x402 + ENSv2 Minted! ✨", Toast.LENGTH_LONG).show()
                        dialog.dismiss()
                    } else {
                        Toast.makeText(this, "Payment failed", Toast.LENGTH_SHORT).show()
                    }
                }
            }, 2000)
        }

        view.findViewById<View>(R.id.btnHaveCode).setOnClickListener {
            dialog.dismiss()
            showPremiumCodeDialog()
        }

        dialog.show()
    }

    private fun observeCurrentProfile() {
        currentUserListener?.remove()
        currentUserListener = userRepository.observeCurrentUser { user ->
            if (_binding == null) return@observeCurrentUser
            user?.let { populateUI(it) }
        }
    }

    private fun observeOtherProfile(userId: String) {
        profileListener?.remove()
        profileListener = userRepository.observeUser(userId) { user ->
            if (_binding == null) return@observeUser
            user?.let { populateUI(it) } ?: run {
                Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun showPremiumCodeDialog() {
        val editText = EditText(this)
        editText.hint = "Enter Premium Code"
        
        AlertDialog.Builder(this)
            .setTitle("Redeem Premium")
            .setMessage("Enter your code to unlock profile banners, chat wallpapers, and more!")
            .setView(editText)
            .setPositiveButton("Redeem") { _, _ ->
                val code = editText.text.toString()
                if (code.isNotEmpty()) {
                    userRepository.redeemPremiumCode(code) { success, message ->
                        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showPremiumCustomizationDialog() {
        val dialog = BottomSheetDialog(this)
        val dialogBinding = DialogPremiumCustomizeBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)

        var selectedBanner = R.drawable.bg_mesh_gradient
        var selectedWallpaper = R.drawable.bg_mesh_gradient
        var newProfilePicUrl: String? = null
        var selectedTheme = com.example.unilink.utils.ThemeUtils.getSelectedTheme(this)

        when (selectedTheme) {
            "Cyber" -> dialogBinding.rbThemeCyber.isChecked = true
            "Indigo Liquid" -> dialogBinding.rbThemeIndigo.isChecked = true
            "Rose Crystal" -> dialogBinding.rbThemeRose.isChecked = true
            "WhatsApp Light" -> dialogBinding.rbThemeLight.isChecked = true
        }

        dialogBinding.btnChangeProfilePic.setOnClickListener {
            newProfilePicUrl = "https://i.pravatar.cc/300?u=${auth.currentUser?.uid}"
            Toast.makeText(this, "New profile picture selected!", Toast.LENGTH_SHORT).show()
        }

        dialogBinding.bannerOption1.setOnClickListener {
            selectedBanner = R.drawable.bg_mesh_gradient
            dialogBinding.bannerOption1.strokeWidth = 6
            dialogBinding.bannerOption2.strokeWidth = 0
            dialogBinding.bannerOption3.strokeWidth = 0
        }
        dialogBinding.bannerOption2.setOnClickListener {
            selectedBanner = R.drawable.bg_brand_gradient
            dialogBinding.bannerOption1.strokeWidth = 0
            dialogBinding.bannerOption2.strokeWidth = 6
            dialogBinding.bannerOption3.strokeWidth = 0
        }
        dialogBinding.bannerOption3.setOnClickListener {
            selectedBanner = R.drawable.grad_cyber
            dialogBinding.bannerOption1.strokeWidth = 0
            dialogBinding.bannerOption2.strokeWidth = 0
            dialogBinding.bannerOption3.strokeWidth = 6
        }

        dialogBinding.wallpaperOption1.setOnClickListener {
            selectedWallpaper = R.drawable.bg_mesh_gradient
            dialogBinding.wallpaperOption1.strokeWidth = 6
            dialogBinding.wallpaperOption2.strokeWidth = 0
            dialogBinding.wallpaperOption3.strokeWidth = 0
        }
        dialogBinding.wallpaperOption2.setOnClickListener {
            selectedWallpaper = R.drawable.bg_animated_mesh
            dialogBinding.wallpaperOption1.strokeWidth = 0
            dialogBinding.wallpaperOption2.strokeWidth = 6
            dialogBinding.wallpaperOption3.strokeWidth = 0
        }
        dialogBinding.wallpaperOption3.setOnClickListener {
            selectedWallpaper = -1
            dialogBinding.wallpaperOption1.strokeWidth = 0
            dialogBinding.wallpaperOption2.strokeWidth = 0
            dialogBinding.wallpaperOption3.strokeWidth = 6
        }

        dialogBinding.btnSaveCustomization.setOnClickListener {
            val themeToSave = when (dialogBinding.rgThemes.checkedRadioButtonId) {
                R.id.rbThemeIndigo -> "Indigo Liquid"
                R.id.rbThemeRose -> "Rose Crystal"
                R.id.rbThemeLight -> "WhatsApp Light"
                else -> "Cyber"
            }
            
            userRepository.updateUserPremiumSettings(selectedBanner, selectedWallpaper, newProfilePicUrl, themeToSave) { success ->
                if (success) {
                    com.example.unilink.utils.ThemeUtils.saveTheme(this, themeToSave)
                    Toast.makeText(this, "Settings updated!", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    // Recreate to apply theme immediately
                    recreate()
                } else {
                    Toast.makeText(this, "Failed to update settings", Toast.LENGTH_SHORT).show()
                }
            }
        }

        dialog.show()
    }

    private fun setupMoreMenu(targetUserId: String) {
        binding.btnMore.setOnClickListener { view ->
            if (_binding == null) return@setOnClickListener
            userRepository.getCurrentUser { currentUser ->
                if (_binding == null) return@getCurrentUser
                val isBlocked = currentUser?.blockedUsers?.contains(targetUserId) == true
                
                val popup = androidx.appcompat.widget.PopupMenu(this, view)
                popup.menu.add(if (isBlocked) "Unblock User" else "Block User")
                
                popup.setOnMenuItemClickListener { item ->
                    if (item.title == "Block User") {
                        userRepository.blockUser(targetUserId) { success ->
                            if (success && _binding != null) Toast.makeText(this, "User blocked", Toast.LENGTH_SHORT).show()
                        }
                    } else if (item.title == "Unblock User") {
                        userRepository.unblockUser(targetUserId) { success ->
                            if (success && _binding != null) Toast.makeText(this, "User unblocked", Toast.LENGTH_SHORT).show()
                        }
                    }
                    true
                }
                popup.show()
            }
        }
    }

    private fun setupLinkButton(targetUserId: String) {
        currentUserListener?.remove()
        currentUserListener = userRepository.observeCurrentUser { currentUser ->
            if (_binding == null) return@observeCurrentUser
            val linkedList = currentUser?.linked ?: emptyList()
            val isLinked = linkedList.contains(targetUserId)
            
            binding.btnLink.text = if (isLinked) "Unlink" else "Link"
            
            binding.btnLink.setOnClickListener {
                if (isLinked) {
                    userRepository.unlinkUser(targetUserId) { success ->
                        if (success) Toast.makeText(this, "Unlinked", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    userRepository.linkUser(targetUserId) { success ->
                        if (success) Toast.makeText(this, "Linked", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun startChatWithUser(userId: String) {
        userRepository.getUserById(userId) { user ->
            if (_binding == null) return@getUserById
            if (user != null) {
                val intent = Intent(this, ChatActivity::class.java)
                val currentUid = auth.currentUser?.uid ?: ""
                val chatId = chatRepository.getChatId(currentUid, user.uid)
                intent.putExtra("chatId", chatId)
                intent.putExtra("receiverId", user.uid)
                intent.putExtra("chatName", user.name)
                intent.putExtra("isGroup", false)
                startActivity(intent)
            } else {
                Toast.makeText(this, "Error: User data unavailable", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun populateUI(user: User) {
        if (_binding == null) return
        binding.tvName.text = user.name
        val ensName = user.ensSubname ?: com.example.unilink.web3.EnsV2Client.formatBuilderSubname(user.username, user.collegeName)
        binding.tvUsername.text = if (user.username.isNotEmpty()) "@${user.username} · $ensName" else ensName
        binding.tvUserType.text = user.userType
        binding.tvBio.text = user.bio
        
        binding.vOnlineStatus.visibility = if (user.isOnline) View.VISIBLE else View.GONE

        binding.tvLinkersCount.text = (user.linkers?.size ?: 0).toString()
        binding.tvLinkedCount.text = (user.linked?.size ?: 0).toString()

        if (user.isPremium) {
            binding.ivPremiumBadge.visibility = View.VISIBLE
            binding.ivProfileBanner.visibility = View.VISIBLE
            binding.btnRedeemPremium.visibility = View.GONE
            
            if (user.uid == auth.currentUser?.uid) {
                binding.btnCustomizePremium.visibility = View.VISIBLE
            }

            val bannerResId = user.profileBannerUrl?.toIntOrNull() ?: R.drawable.bg_mesh_gradient
            binding.ivProfileBanner.setImageResource(bannerResId)

            if (user.premiumCode == "PAPABOL1917") {
                binding.tvVipStatus.visibility = View.VISIBLE
            } else {
                binding.tvVipStatus.visibility = View.GONE
            }
        } else {
            binding.ivPremiumBadge.visibility = View.GONE
            binding.ivProfileBanner.visibility = View.GONE
            binding.btnCustomizePremium.visibility = View.GONE
            if (user.uid == auth.currentUser?.uid) {
                binding.btnRedeemPremium.visibility = View.VISIBLE
            }
        }

        showLinkedByInfo(user)

        if (user.userType == "College Student") {
            binding.tvDetail1.text = "College: ${user.collegeName ?: "N/A"}"
            binding.tvDetail2.text = "Branch: ${user.branch ?: "N/A"}"
            binding.tvDetail3.text = "Year: ${user.yearOrSemester ?: "N/A"}"
        } else {
            binding.tvDetail1.text = "School: ${user.schoolName ?: "N/A"}"
            binding.tvDetail2.text = "Stream: ${user.branch ?: "N/A"}"
            binding.tvDetail3.text = "Class: ${user.yearOrSemester ?: "N/A"}"
        }
        binding.tvLocation.text = "Location: ${user.city}, ${user.state}"
    }

    private fun showLinkedByInfo(targetUser: User) {
        val myUid = auth.currentUser?.uid ?: return
        if (targetUser.uid == myUid) {
            binding.tvLinkedBy.visibility = View.GONE
            return
        }

        userRepository.getCurrentUser { currentUser ->
            if (_binding == null || currentUser == null) return@getCurrentUser
            
            val targetLinkers = targetUser.linkers ?: emptyList()
            val myLinked = currentUser.linked ?: emptyList()
            val mutualLinkers = myLinked.intersect(targetLinkers.toSet())
            
            if (mutualLinkers.isNotEmpty()) {
                userRepository.getUserById(mutualLinkers.first()) { mutualUser ->
                    if (_binding == null || mutualUser == null) return@getUserById
                    val count = mutualLinkers.size - 1
                    binding.tvLinkedBy.text = if (count > 0) {
                        "Linked by ${mutualUser.name} and $count others"
                    } else {
                        "Linked by ${mutualUser.name}"
                    }
                    binding.tvLinkedBy.visibility = View.VISIBLE
                }
            } else {
                binding.tvLinkedBy.visibility = View.GONE
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        profileListener?.remove()
        currentUserListener?.remove()
        _binding = null
    }
}
