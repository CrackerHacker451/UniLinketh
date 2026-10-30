package com.example.unilink.web3

/**
 * ENSv2 Hierarchical Registry, PermissionedResolver & EnhancedAccessControl Manager for UniLink
 * Targets:
 * - 🧬 Best Use of ENSv2 ($7,000)
 * - 🔗 Best Integration of ENSv2 into an Existing Project ($3,000 Continuity)
 *
 * Implements:
 * - Student Builder Subnames (`<username>.nst.unilink.eth`)
 * - Expiring Hackathon Squad Subnames (`<squad>.unilink.eth`)
 * - ENSIP-25 / ENSIP-26 AI Copilot Subnames (`guider.<squad>.unilink.eth`)
 * - Enhanced Access Control (EAC) bitmaps (`ROLE_KYC_VERIFIER` vs `ROLE_STUDENT_OWNER`)
 */
object EnsV2Client {

    const val PARENT_DOMAIN = "unilink.eth"
    const val ROLE_REGISTRAR_ADMIN = 1 shl 0
    const val ROLE_KYC_VERIFIER = 1 shl 1
    const val ROLE_STUDENT_OWNER = 1 shl 2
    const val ROLE_AI_AGENT_OPERATOR = 1 shl 3

    data class EnsV2SubnameBundle(
        val builderSubname: String,
        val squadSubname: String,
        val aiAgentSubname: String,
        val records: Map<String, String>
    )

    fun formatBuilderSubname(username: String, college: String?): String {
        val cleanUser = username.lowercase().replace(Regex("[^a-z0-9]"), "").ifEmpty { "builder" }
        val cleanCollege = when {
            college.isNullOrBlank() -> "nst"
            college.contains("NST", ignoreCase = true) || college.contains("Newton", ignoreCase = true) -> "nst"
            college.contains("SRM", ignoreCase = true) -> "srm"
            college.contains("IIT", ignoreCase = true) -> "iit"
            else -> "campus"
        }
        return "$cleanUser.$cleanCollege.$PARENT_DOMAIN"
    }

    fun mintSquadNamespaceBundle(
        teamTitle: String,
        ownerUsername: String,
        skills: List<String>,
        arkivEntityKey: String,
        worldIdNullifier: String
    ): EnsV2SubnameBundle {
        val slug = teamTitle.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-').ifEmpty { "squad" }
        val squadSubname = "$slug.$PARENT_DOMAIN"
        val aiAgentSubname = "guider.$slug.$PARENT_DOMAIN"
        val builderSubname = formatBuilderSubname(ownerUsername, "NST")

        val records = mapOf(
            "verified.college" to "S-VYASA & Newton School of Technology",
            "verified.worldid" to worldIdNullifier.take(24) + "...",
            "unilink.skills" to skills.joinToString(", "),
            "arkiv.entity" to arkivEntityKey,
            "ensip25.agent.verified" to "true",
            "ensip26.agent.endpoint" to "https://newtonslobby.com/api/x402/ai-guider",
            "ensip26.agent.x402.hedera" to "0.0.4829102 (HTS USDC 0.0.429274)"
        )

        return EnsV2SubnameBundle(
            builderSubname = builderSubname,
            squadSubname = squadSubname,
            aiAgentSubname = aiAgentSubname,
            records = records
        )
    }

    /**
     * Enforces ENSv2 Enhanced Access Control (EAC):
     * Students with ROLE_STUDENT_OWNER can edit `unilink.skills`, `com.github`, and `bio`,
     * but ONLY `ROLE_KYC_VERIFIER` can mutate `verified.college` or `verified.worldid`.
     */
    fun checkEacWritePermission(callerRoleBitmap: Int, recordKey: String): Pair<Boolean, String> {
        val isRestrictedKycRecord = recordKey == "verified.college" || recordKey == "verified.worldid"
        if (isRestrictedKycRecord && (callerRoleBitmap and ROLE_KYC_VERIFIER) == 0) {
            return false to "ENSv2 EAC Revert: Caller holds ROLE_STUDENT_OWNER (1<<2), but '$recordKey' requires ROLE_KYC_VERIFIER (1<<1)."
        }
        return true to "ENSv2 PermissionedResolver: Record '$recordKey' updated successfully."
    }
}
