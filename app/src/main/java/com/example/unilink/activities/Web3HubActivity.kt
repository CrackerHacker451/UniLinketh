package com.example.unilink.activities

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.unilink.databinding.ActivityWeb3HubBinding
import com.example.unilink.models.Project
import com.example.unilink.utils.ThemeUtils
import com.example.unilink.web3.ArkivSwarmClient
import com.example.unilink.web3.EnsV2Client
import com.example.unilink.web3.HederaX402Client
import com.example.unilink.web3.WorldIdAgentPassManager
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class Web3HubActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWeb3HubBinding
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeUtils.applyTheme(this)
        super.onCreate(savedInstanceState)
        binding = ActivityWeb3HubBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
    }

    private fun setupListeners() {
        val currentUid = auth.currentUser?.uid ?: "student_nst_01"

        // 1. World ID v4 (IDKit)
        binding.btnHubVerifyIdKit.setOnClickListener {
            WorldIdAgentPassManager.showIdKitVerificationDialog(
                context = this,
                action = "verify-unilink-builder-pass",
                signal = currentUid
            ) { result ->
                binding.tvHubConsoleOutput.text = buildString {
                    appendLine("=== WORLD ID v4 (IDKIT) RESULT ===")
                    appendLine("Success: ${result.success}")
                    appendLine("Credential: ${result.credentialType}")
                    if (result.sybilScore != null) appendLine("Sybil Score: ${result.sybilScore}/10")
                    if (result.canonicalNullifierDecimal.isNotEmpty()) {
                        appendLine("Nullifier NUMERIC(78,0): ${result.canonicalNullifierDecimal.take(28)}...")
                    }
                    appendLine("Detail: ${result.message}")
                }
                Toast.makeText(this, result.message, Toast.LENGTH_SHORT).show()
            }
        }

        // 2. World ID Agent Pass Step-Up
        binding.btnHubAgentPass.setOnClickListener {
            WorldIdAgentPassManager.requireAgentPassStepUp(
                context = this,
                protectedActionName = "RELEASE_BARTER_ESCROW_45_USDC",
                onAuthorized = { pairwiseSub, challengeId ->
                    binding.tvHubConsoleOutput.text = buildString {
                        appendLine("=== WORLD ID AGENT PASS: VALIDATED ===")
                        appendLine("Challenge ID: $challengeId (max_age=60s)")
                        appendLine("OIDC Issuer: https://auth.world.org")
                        appendLine("Pairwise Subject (sub): $pairwiseSub")
                        appendLine("Protected Action: RELEASE_BARTER_ESCROW_45_USDC -> EXECUTED ✅")
                    }
                },
                onBlocked = { outcome, reason ->
                    binding.tvHubConsoleOutput.text = buildString {
                        appendLine("=== WORLD ID AGENT PASS: BLOCKED ($outcome) ===")
                        appendLine(reason)
                        appendLine("Protected Action: DID NOT OCCUR 🛑")
                    }
                }
            )
        }

        // 3. Arkiv + Swarm Entity Sync
        binding.btnHubArkivWrite.setOnClickListener {
            lifecycleScope.launch {
                val sampleProject = Project(
                    projectId = "proj_mumbai_01",
                    title = "NST Web3 Squad Alpha",
                    description = "Ephemeral hackathon squad synced to Arkiv DB-Chain",
                    ownerId = currentUid,
                    ownerName = "Laltesh Chaudhary",
                    skillsRequired = listOf("Kotlin", "Solidity", "Arkiv", "x402"),
                    tags = listOf("Web3 / Crypto", "Hackathon")
                )
                val receipt = ArkivSwarmClient.syncProjectEntityToArkiv(sampleProject, ttlHours = 48)
                binding.tvHubConsoleOutput.text = buildString {
                    appendLine("=== ARKIV TESTNET (#7738577) + SWARM ===")
                    appendLine("Entity Key: ${receipt.entityKey}")
                    appendLine("Owner Wallet: ${receipt.ownerWallet}")
                    appendLine("ExpiresAt (Unix TTL): ${receipt.expiresAtUnix} (48h Sprint Window)")
                    appendLine("Swarm Ref (AES-GCM-256): ${receipt.encryptedSwarmRef.take(44)}...")
                    appendLine("Runnable SQL Query: ${receipt.sqlQueryPreview}")
                }
            }
        }

        // 4. ENSv2 Subname Mint & EAC Role Check
        binding.btnHubEnsMint.setOnClickListener {
            val bundle = EnsV2Client.mintSquadNamespaceBundle(
                teamTitle = "Triad Alpha",
                ownerUsername = "laltesh",
                skills = listOf("Kotlin", "AI/ML", "Web3"),
                arkivEntityKey = "0xarkiv_9f81a2c401",
                worldIdNullifier = "0x04e5f6a199887766"
            )
            val (_, eacBlockMsg) = EnsV2Client.checkEacWritePermission(
                EnsV2Client.ROLE_STUDENT_OWNER,
                "verified.college"
            )
            val (_, eacAllowMsg) = EnsV2Client.checkEacWritePermission(
                EnsV2Client.ROLE_STUDENT_OWNER,
                "unilink.skills"
            )
            binding.tvHubConsoleOutput.text = buildString {
                appendLine("=== ENSv2 PERMISSIONED REGISTRY (unilink.eth) ===")
                appendLine("Builder Pass: ${bundle.builderSubname}")
                appendLine("Squad Namespace: ${bundle.squadSubname}")
                appendLine("AI Copilot (ENSIP-26): ${bundle.aiAgentSubname}")
                appendLine("EAC Student Edit (unilink.skills): $eacAllowMsg")
                appendLine("EAC Student Forge Check (verified.college): $eacBlockMsg")
            }
        }

        // 5. Hedera x402 + HTS/HCS
        binding.btnHubHederaX402.setOnClickListener {
            val options = arrayOf(
                "⚡ @ai Sprint Architecture Blueprint (0.05 USDC — Auto-Settled)",
                "🎓 Peer Doubt Bounty Settlement (0.10 USDC — 95/5 HTS Split)",
                "⚠️ Heavy GPU Run (0.50 USDC — Exceeds 0.25 USDC Agent Cap!)"
            )
            AlertDialog.Builder(this)
                .setTitle("🤖 Hedera x402 Payment Handshake")
                .setItems(options) { _, which ->
                    lifecycleScope.launch {
                        val (service, price) = when (which) {
                            0 -> "@ai Sprint Architecture Blueprint" to 50_000L
                            1 -> "Peer Doubt Bounty Settlement" to 100_000L
                            else -> "Heavy GPU Cluster Run" to 500_000L
                        }
                        val receipt = HederaX402Client.settleX402Request(service, price)
                        if (receipt.settled) {
                            binding.tvHubConsoleOutput.text = buildString {
                                appendLine("=== HEDERA x402 PAYMENT SETTLED (HTTP 200 OK) ===")
                                appendLine("Service: ${receipt.serviceName}")
                                appendLine("Paid: ${receipt.amountUsdcFormatted} (HTS USDC 0.0.429274)")
                                appendLine("Gas Payer: x402 Facilitator (0 HBAR from student)")
                                appendLine("Hedera Tx ID: ${receipt.hederaTxId}")
                                appendLine("HCS Audit Topic: ${receipt.hcsTopicId} (Seq #${receipt.hcsSequenceNumber})")
                                appendLine("HashScan: ${receipt.hashScanUrl}")
                            }
                        } else {
                            binding.tvHubConsoleOutput.text = buildString {
                                appendLine("=== HEDERA x402 SPEND GUARD TRIGGERED ===")
                                appendLine(receipt.errorMessage)
                            }
                        }
                    }
                }
                .show()
        }
    }
}
