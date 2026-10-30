package com.example.unilink.models

import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

@IgnoreExtraProperties
data class User(
    val uid: String = "",
    val name: String = "",
    val username: String = "",
    val email: String = "",
    val userType: String = "", // "College Student" or "School Student"
    val collegeName: String? = null,
    val schoolName: String? = null,
    val branch: String? = null, // or stream
    val yearOrSemester: String? = null, // or className
    val city: String = "",
    val state: String = "",
    val bio: String = "",
    val profileImageUrl: String? = null,
    val phoneNumber: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val blockedUsers: List<String>? = emptyList(),
    val linkers: List<String>? = emptyList(), // formerly followers
    val linked: List<String>? = emptyList(), // formerly following
    val skills: List<String>? = emptyList(),
    val skillLevels: Map<String, Int>? = emptyMap(), // skillName -> level (0-100)
    val uniCoins: Int = 100,
    val badges: List<String> = emptyList(),
    val vouchCount: Int = 0,
    val status: String = "Open to Pair", // Open to Pair, Open to Hire, Busy
    val interests: List<String> = emptyList(),
    val goals: String = "",
    val totalXp: Int = 0,
    val streakDays: Int = 0,
    val lastLoginDate: Date? = null,
    val fcmToken: String? = null,
    val isLookingForRandomMatch: Boolean = false,
    val isPremium: Boolean = false,
    val isOnline: Boolean = false,
    val premiumCode: String? = null,
    val profileBannerUrl: String? = null,
    val chatWallpaperUrl: String? = null,
    val appTheme: String? = "Cyber",
    // ETHGlobal Mumbai Web3 Upgrade Fields (World ID v4 + ENSv2 + Arkiv + Swarm + Hedera x402)
    val worldIdVerified: Boolean = false,
    val worldIdNullifier: String? = null,
    val worldIdCredentialType: String? = null, // "proof_of_human" (Schema #1) or "selfie" (Schema #11)
    val sybilScore: Int? = null,
    val ensSubname: String? = null, // e.g., "laltesh.nst.unilink.eth"
    val walletAddress: String? = "0x71C95911E9a5D330f4D621842EC243EE1343292e",
    val hederaAccountId: String? = "0.0.4829102",
    val encryptedSwarmAvatarRef: String? = null,
    @ServerTimestamp val createdAt: Date? = null
)
