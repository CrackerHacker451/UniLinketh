package com.example.unilink.repository

import com.example.unilink.models.Project
import com.example.unilink.web3.ArkivSwarmClient
import com.example.unilink.web3.EnsV2Client
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ProjectRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    fun createProject(
        title: String,
        description: String,
        skills: List<String>,
        tags: List<String> = listOf("Hackathon", "Web3 / Crypto"),
        worldIdNullifier: String = "0x04e5f6a199887766",
        onComplete: (Boolean) -> Unit
    ) {
        val uid = auth.currentUser?.uid ?: return
        val projectId = db.collection("projects").document().id

        db.collection("users").document(uid).get().addOnSuccessListener { doc ->
            val name = doc.getString("name") ?: "User"
            val username = doc.getString("username") ?: name

            CoroutineScope(Dispatchers.Main).launch {
                val baseProject = Project(
                    projectId = projectId,
                    title = title,
                    description = description,
                    ownerId = uid,
                    ownerName = name,
                    skillsRequired = skills,
                    members = listOf(uid),
                    tags = tags,
                    status = "Open"
                )

                // 1. Sync to Arkiv Testnet (#7738577) with 48h TTL + AES-GCM encrypted Swarm ref
                val arkivReceipt = ArkivSwarmClient.syncProjectEntityToArkiv(baseProject, ttlHours = 48)

                // 2. Mint ENSv2 squad + AI agent subnames under unilink.eth
                val ensBundle = EnsV2Client.mintSquadNamespaceBundle(
                    teamTitle = title,
                    ownerUsername = username,
                    skills = skills,
                    arkivEntityKey = arkivReceipt.entityKey,
                    worldIdNullifier = worldIdNullifier
                )

                val enrichedProject = baseProject.copy(
                    arkivEntityKey = arkivReceipt.entityKey,
                    arkivExpiresAt = arkivReceipt.expiresAtUnix,
                    ensSubname = ensBundle.squadSubname,
                    agentEnsSubname = ensBundle.aiAgentSubname,
                    encryptedSwarmRef = arkivReceipt.encryptedSwarmRef,
                    worldIdVerifiedLead = true
                )

                db.collection("projects").document(projectId).set(enrichedProject)
                    .addOnCompleteListener { task ->
                        onComplete(task.isSuccessful)
                    }
            }
        }
    }

    fun getAllProjects(onResult: (List<Project>) -> Unit) {
        db.collection("projects")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    onResult(emptyList())
                    return@addSnapshotListener
                }
                val projects = snapshot?.mapNotNull { it.toObject(Project::class.java) } ?: emptyList()
                onResult(projects)
            }
    }

    fun joinProject(projectId: String, onComplete: (Boolean) -> Unit) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("projects").document(projectId)
            .update("members", com.google.firebase.firestore.FieldValue.arrayUnion(uid))
            .addOnCompleteListener { onComplete(it.isSuccessful) }
    }
}
