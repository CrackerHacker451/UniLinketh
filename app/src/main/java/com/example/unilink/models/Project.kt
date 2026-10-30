package com.example.unilink.models

import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

@IgnoreExtraProperties
data class Project(
    val projectId: String = "",
    val title: String = "",
    val description: String = "",
    val ownerId: String = "",
    val ownerName: String = "",
    val skillsRequired: List<String> = emptyList(),
    val members: List<String> = emptyList(),
    val maxTeamSize: Int = 4,
    val progress: Int = 0, // 0-100
    val tags: List<String> = emptyList(),
    val status: String = "Open", // Open, In Progress, Completed
    // ETHGlobal Mumbai Web3 Upgrade Fields (Arkiv DB-Chain + Swarm + ENSv2 + World ID + Hedera HCS)
    val arkivEntityKey: String? = null,
    val arkivChainId: Int = 7738577,
    val arkivExpiresAt: Long? = null, // Unix timestamp for automatic hackathon deadline expiration
    val ensSubname: String? = null, // e.g., "squad-alpha.unilink.eth"
    val agentEnsSubname: String? = null, // e.g., "guider.squad-alpha.unilink.eth" (ENSIP-25/26)
    val encryptedSwarmRef: String? = null, // AES-GCM-256 encrypted Swarm bzz:// blueprint pointer
    val worldIdVerifiedLead: Boolean = true,
    val hederaHcsTopicId: String = "0.0.5189204",
    @ServerTimestamp val createdAt: Date? = null
)
