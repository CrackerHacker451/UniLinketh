package com.example.unilink.models

data class Doubt(
    val doubtId: String = "",
    val userId: String = "",
    val userName: String = "",
    val userAvatarUrl: String? = null,
    val question: String = "",
    val rewardCoins: Int = 0,
    val tags: List<String> = emptyList(),
    val answerCount: Int = 0,
    val accentColor: String = "#22D3EE",
    val isOnline: Boolean = false,
    // ETHGlobal Mumbai Web3 Upgrade Fields (Hedera x402 + Arkiv TTL)
    val x402PriceUsdcMicro: Long = 50000L, // 0.05 USDC settled on Hedera Testnet via x402
    val hederaTxId: String? = null,
    val arkivEntityKey: String? = null
)
