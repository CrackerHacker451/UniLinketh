# UniLink (Newton's Lobby) — ETHGlobal Mumbai 2026 Edition

> **Decentralized Campus OS & Verified Hackathon Squad Protocol**  
> **Live Web Portal**: [https://newtonslobby.com](https://newtonslobby.com/) · **Android Package**: `com.example.unilink`  
> **Founder**: Laltesh Chaudhary (S-VYASA University & Newton School of Technology, Bengaluru)

---

## 🏆 ETHGlobal Mumbai Partner Integrations in This Codebase

### 1. World (`$7,500` General / `$5,000` Continuity) — World ID Agent Pass + IDKit v4
- **Android Implementation**: [`app/src/main/java/com/example/unilink/web3/WorldIdAgentPassManager.kt`](./app/src/main/java/com/example/unilink/web3/WorldIdAgentPassManager.kt)
- **Backend Verifier**: [`backend/server.ts`](./backend/server.ts) (`/api/rp-signature`, `/api/verify-proof`, `/api/agent-pass/execute-protected`)
- **Where It Runs in the App**:
  - **`CreateTeamActivity.kt`**: Triggers a **World ID Agent Pass** OIDC step-up checkpoint (`https://auth.world.org`, `max_age=60`) before deploying a new hackathon squad to Arkiv & ENSv2. Includes a live judge selector demonstrating both the **Validated Journey** and **Denied / Expired / Cancelled paths** where team deployment is blocked.
  - **`FindTeamActivity.kt` & `HomeFragment.kt`**: Gates scarce squad seats and Focus Rooms behind **IDKit v4** (`proof_of_human` Schema `#1` & `selfie` Check Beta Schema `#11` with Sybil score `10/10`), converting `0x` hex nullifiers to canonical `NUMERIC(78, 0)` decimals to block Sybil replay attacks.
  - **`Web3HubActivity.kt`**: Dedicated in-app testing console for World ID v4 and Agent Pass flows.
- 📄 **Mandatory Integration Debrief**: [`WORLD_ID_DEBRIEF.md`](./WORLD_ID_DEBRIEF.md)

### 2. Arkiv + Swarm (`$4,000` Build on Arkiv + `$1,000` Swarm Bonus)
- **Android Implementation**: [`app/src/main/java/com/example/unilink/web3/ArkivSwarmClient.kt`](./app/src/main/java/com/example/unilink/web3/ArkivSwarmClient.kt) & [`ProjectRepository.kt`](./app/src/main/java/com/example/unilink/repository/ProjectRepository.kt)
- **How We Use Arkiv (`chainId: 7738577`) & Swarm (`ethswarm.org`)**:
  - Every squad created in `CreateTeamActivity.kt` is written to **Arkiv Tiramisu DB-Chain Testnet** (`https://rpc.tiramisu.db-chain.testnet.arkiv.network`) as a wallet-owned `unilink_squad` entity whose `expiresAt` TTL matches the hackathon submission window.
  - Before writing to Arkiv, the squad's Swarm `bzz://` blueprint reference is encrypted client-side with `AES/GCM/NoPadding` (`ArkivSwarmClient.encryptSwarmReference`) so public DB readers cannot extract the Swarm decryption key.
  - Category filtering in `FindTeamActivity.kt` runs live SQL-like annotation queries (`entity_type = "unilink_squad" AND open_seats >= 1 AND world_id_verified = "true"`).
- **Writer Wallet Address**: `0x71C95911E9a5D330f4D621842EC243EE1343292e`
- **Sample Swarm Reference**: `bzz://a8f4e291c07d4b32918273645566778899aabbccddeeff001122334455667788`
- 📄 **Mandatory Arkiv Rubric Files**:
  - [`/arkiv/schema.md`](./arkiv/schema.md)
  - [`/arkiv/friction.md`](./arkiv/friction.md)
  - [`/arkiv/reach.md`](./arkiv/reach.md)
  - [`/arkiv/why-arkiv.md`](./arkiv/why-arkiv.md)
  - [`/arkiv/how-you-use-arkiv.md`](./arkiv/how-you-use-arkiv.md)

### 3. ENSv2 (`$7,000` Best Use of ENSv2 / `$3,000` Continuity)
- **Smart Contract**: [`contracts/UniLinkENSv2Subregistry.sol`](./contracts/UniLinkENSv2Subregistry.sol)
- **Android Implementation**: [`app/src/main/java/com/example/unilink/web3/EnsV2Client.kt`](./app/src/main/java/com/example/unilink/web3/EnsV2Client.kt), [`ProfileActivity.kt`](./app/src/main/java/com/example/unilink/activities/ProfileActivity.kt), & [`ProjectAdapter.kt`](./app/src/main/java/com/example/unilink/adapters/ProjectAdapter.kt)
- **Key Features**:
  - Issues hierarchical subnames under `unilink.eth` for students (`laltesh.nst.unilink.eth`), squads (`triad-alpha.unilink.eth`), and AI Copilot agents (`guider.triad-alpha.unilink.eth` with **ENSIP-25 / ENSIP-26** records).
  - Enforces **Enhanced Access Control (EAC)** bitmaps so students (`ROLE_STUDENT_OWNER`) can edit their skill records while only the University KYC Registrar (`ROLE_KYC_VERIFIER`) can write `verified.college` and `verified.worldid`.

### 4. Hedera (`$6,500` x402 on Hedera + `$2,000` "No Solidity Allowed" Hedera SDKs)
- **Android Implementation**: [`app/src/main/java/com/example/unilink/web3/HederaX402Client.kt`](./app/src/main/java/com/example/unilink/web3/HederaX402Client.kt), [`DoubtAdapter.kt`](./app/src/main/java/com/example/unilink/adapters/DoubtAdapter.kt), & [`ProfileActivity.kt`](./app/src/main/java/com/example/unilink/activities/ProfileActivity.kt)
- **Backend Merchant Middleware**: [`backend/server.ts`](./backend/server.ts) (`/api/x402/ai-guider`)
- **Key Features**:
  - Settles Doubt Board bounties, `@ai` Sprint Architect calls, and UniLink Pro upgrades over **HTTP 402 Payment Required** on Hedera Testnet in **HTS USDC (`0.0.429274`)** with **0 HBAR gas** required from the student (facilitator pays network fees).
  - Enforces a `0.25 USDC` per-call autonomous agent spending cap and logs every settled payment to **Hedera Consensus Service (HCS) Topic `0.0.5189204`**.
  - **Settled Hedera Testnet Transaction ID**: `0.0.4829102@1793376421.418291045` ([View on HashScan](https://hashscan.io/testnet/transaction/0.0.4829102@1793376421.418291045))

---

## 🔄 Continuity Track: Before vs. During ETHGlobal Mumbai

| Component | Before ETHGlobal Mumbai (`CrackerHacker451/UniLink`) | Added at ETHGlobal Mumbai 2026 |
| :--- | :--- | :--- |
| **`models/User.kt` & `Project.kt`** | Basic Firestore fields (`uid`, `username`, `uniCoins`, `title`, `members`) | Added `worldIdVerified`, `worldIdNullifier`, `ensSubname`, `arkivEntityKey`, `arkivExpiresAt`, `encryptedSwarmRef`, `hederaHcsTopicId` |
| **`CreateTeamActivity.kt` & `ProjectRepository.kt`** | Wrote team only to Firestore with no expiration or human verification | Added **World ID Agent Pass** step-up gate, **Arkiv (`#7738577`)** expiring entity sync, **Swarm** `AES-GCM-256` encryption, and **ENSv2** subname minting |
| **`FindTeamActivity.kt` & `HomeFragment.kt`** | Local filter; placeholder toast on Doubt Board & Focus Rooms | Added **IDKit v4** seat verification, **Arkiv SQL query** execution, and `Web3HubActivity` launcher |
| **`DoubtAdapter.kt` & `ProfileActivity.kt`** | Empty click listener on `btnAnswer`; simulated 2s delay on Pro upgrade | Wired **Hedera x402** HTTP 402 payment settlement in HTS USDC (`0.0.429274`) + **HCS Topic `0.0.5189204`** audit logging |
