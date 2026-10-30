package com.example.unilink.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.unilink.adapters.DoubtAdapter
import com.example.unilink.adapters.DuelPeerAdapter
import com.example.unilink.adapters.FocusRoomAdapter
import com.example.unilink.adapters.PostAdapter
import com.example.unilink.adapters.StoryAdapter
import com.example.unilink.databinding.FragmentHomeBinding
import com.example.unilink.models.Doubt
import com.example.unilink.models.DuelPeer
import com.example.unilink.models.FocusRoom
import com.example.unilink.models.Story
import com.example.unilink.repository.UserRepository
import com.google.firebase.firestore.ListenerRegistration

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val postRepository = com.example.unilink.repository.PostRepository()
    private val storyRepository = com.example.unilink.repository.StoryRepository()
    private val userRepository = UserRepository()
    
    private var userListener: ListenerRegistration? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        setupClickListeners()
        fetchFeed()
        fetchStories()
        setupDoubtBoard()
        setupSkillDuels()
        setupFocusRooms()
    }

    private fun setupUI() {
        if (_binding == null) return

        // Real-time listener for current user stats
        userListener?.remove()
        userListener = userRepository.observeCurrentUser { user ->
            if (_binding == null) return@observeCurrentUser
            user?.let {
                binding.tvCollegeName.text = it.collegeName ?: "SRM University"
                
                binding.tvUniCoins.text = it.uniCoins.toString()
                
                val streakText = if (it.streakDays == 1) "🔥 1-day streak" else "🔥 ${it.streakDays}-day streak"
                binding.tvXpStreak.text = streakText
                binding.tvTotalXp.text = "${it.totalXp} XP"
                
                val progress = (it.totalXp % 1000) / 10
                binding.progressXp.progress = progress
            }
        }
    }

    private fun setupClickListeners() {
        binding.actionFindTeam.root.setOnClickListener {
            startActivity(Intent(context, com.example.unilink.activities.FindTeamActivity::class.java))
        }
        binding.actionCreateTeam.root.setOnClickListener {
            startActivity(Intent(context, com.example.unilink.activities.CreateTeamActivity::class.java))
        }
        binding.actionRandomMatch.root.setOnClickListener {
            startActivity(Intent(context, com.example.unilink.activities.RandomMatchActivity::class.java))
        }
        
        binding.btnNotifications.setOnClickListener {
            startActivity(Intent(context, com.example.unilink.activities.Web3HubActivity::class.java))
        }
        
        binding.tvViewAllDoubts.setOnClickListener {
            startActivity(Intent(context, com.example.unilink.activities.Web3HubActivity::class.java))
        }

        binding.btnPostDoubt.setOnClickListener {
            startActivity(Intent(context, com.example.unilink.activities.Web3HubActivity::class.java))
        }

        binding.tvJoinFocusRoom.setOnClickListener {
            com.example.unilink.web3.WorldIdAgentPassManager.showIdKitVerificationDialog(requireContext(), "join-focus-room", "focus_room_1") { res -> Toast.makeText(context, res.message, Toast.LENGTH_LONG).show() }
        }
    }

    private fun setupDoubtBoard() {
        val mockDoubts = listOf(
            Doubt("1", "101", "Kurt", null, "Best way to prep for OOS engineering interviews?", 8, listOf("Career", "Engineering"), 4, "#22D3EE", true),
            Doubt("2", "102", "Summers", null, "Need help understanding Michigan's CS transfer credits", 5, listOf("Academics", "CS"), 0, "#EC4899", false),
            Doubt("3", "103", "Alice", null, "How to balance part-time work with studies?", 12, listOf("Lifestyle", "Career"), 2, "#8B5CF6", true)
        )
        binding.rvDoubtBoard.layoutManager = LinearLayoutManager(context)
        binding.rvDoubtBoard.adapter = DoubtAdapter(mockDoubts)
    }

    private fun setupSkillDuels() {
        val mockPeers = listOf(
            DuelPeer("1", "Sameera", "AI/ML", "S"),
            DuelPeer("2", "Kurt", "Robotics", "K"),
            DuelPeer("3", "Jean", "Drones", "J"),
            DuelPeer("4", "Alex", "Design", "A")
        )
        binding.rvSkillDuels.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        binding.rvSkillDuels.adapter = DuelPeerAdapter(mockPeers)
    }

    private fun setupFocusRooms() {
        val mockRooms = listOf(
            FocusRoom("1", "Midterm", "#EC4899"),
            FocusRoom("2", "Robotics Lab", "#22D3EE"),
            FocusRoom("3", "Quiet Hours", "#22D3EE"),
            FocusRoom("4", "Finals Prep", "#8B5CF6")
        )
        binding.rvFocusRooms.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        binding.rvFocusRooms.adapter = FocusRoomAdapter(mockRooms)
    }

    private fun fetchStories() {
        storyRepository.getActiveStories { stories ->
            if (_binding == null) return@getActiveStories
            val storyList = mutableListOf<Story>()
            storyList.add(Story("1", "1", "My Story", ""))
            storyList.addAll(stories)
            binding.rvStories.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            binding.rvStories.adapter = StoryAdapter(storyList)
        }
    }

    private fun fetchFeed() {
        if (_binding == null) return
        binding.rvActivityFeed.layoutManager = LinearLayoutManager(context)
        postRepository.getFeed { posts ->
            if (_binding == null) return@getFeed
            if (posts.isNotEmpty()) {
                binding.rvActivityFeed.adapter = PostAdapter(posts)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        userListener?.remove()
        _binding = null
    }
}
