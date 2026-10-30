# World ID Agent Pass & IDKit v4 — Integration Debrief & Feedback

**Project**: Newton's Lobby / UniLink OS ([newtonslobby.com](https://newtonslobby.com/))  
**Bounties Targeted**:
1. 🚀 **World ID Agent Pass** ($5,000 General / $2,500 Continuity Track)
2. 🪪 **Best Use of IDKit / [Cont] Best IDKit Use Case** ($2,500)

---

## 1. Product Trust Moments & Why Each Credential Was Chosen
1. **IDKit v4 (`proof_of_human`, Schema ID `1` + `selfie` Check Beta, Schema ID `11` with `sybil_score`)**:
   - **Trust Moment**: Claiming one of the scarce 3–6 seats in a Hackathon Squad Base Camp, enforcing the platform's **3/3 Daily Squad Creation Anti-Spam Guard**, and borrowing expensive lab hardware (Jetson Nano, Arduino kits) in **Barter Block**.
   - **Why Minimum Sufficient Assurance**: Universities suffer from bot accounts and duplicate numbers spamming hackathon groups. `proof_of_human` guarantees 1-human-1-seat without exposing the student's Aadhaar or residential address to peers, while `selfie` Check Beta (`sybil_score >= 8`) provides instant mobile onboarding on Android (`UniLink_v8.5.apk`) even when a physical Orb is not on campus.
2. **World ID Agent Pass (`https://auth.world.org`)**:
   - **Trust Moment**: In Newton's Lobby, our autonomous **AI Guider & Sprint Architect (`@ai`)** can reorganize the Squad Kanban board, hire peer sub-agents over Hedera x402, or disband/lock a Squad Base Camp.
   - **Why Human-in-the-Loop Fresh Verification is Required**: A session login at the start of a 48-hour hackathon should not grant an AI copilot carte blanche to spend squad USDC bounties or disband a team at 3 AM. When `@ai` proposes a high-impact mutation, the backend pauses the workflow and issues an OIDC Step-Up Challenge (`RFC 9470` / `max_age=60`) via **World ID Agent Pass**. Only if the human Squad Lead completes fresh verification does the protected action execute.

---

## 2. Integration Debrief (Qualification Requirement)

- **Time to First Success**:
  - **IDKit v4 (`@worldcoin/idkit` + `/api/v4/verify/{rp_id}`)**: ~35 minutes from generating `app_id`, `rp_id`, and `RP_SIGNING_KEY` to getting our first mocked v4 proof validated on the backend.
  - **World ID Agent Pass (`auth.world.org`)**: ~45 minutes to wire the OIDC authorization code + PKCE + `max_age` step-up flow and link the pairwise `sub` identifier to our Squad Lead session.
- **Friction Encountered**:
  1. **World ID 3.0 vs. 4.0 Payload Shape Differences**: Many community examples still show the v3 flat `{ merkle_root, nullifier_hash, proof }` payload, whereas IDKit v4 returns a nested `responses: [{ identifier, proof: [...], nullifier, issuer_schema_id }]` array and requires server-side `signRequest` (`rp_context`) before opening the widget.
  2. **Hex Nullifier Storage**: Converting the `0x`-prefixed 256-bit `nullifier` into a canonical `NUMERIC(78, 0)` decimal representation on the backend is critical to prevent case-sensitivity replay bugs (`0x04E5...` vs `0x04e5...`).
- **Missing Capability or Documentation**:
  - A unified copy-paste **Kotlin + Jetpack Compose** snippet for triggering **World ID Agent Pass OIDC step-up challenges** inside native Android WebViews/Custom Tabs (alongside the existing `com.worldcoin:idkit` Gradle docs) would be huge for mobile-first agent apps like UniLink.
- **One Improvement with the Greatest Impact**:
  - **Pre-built Action-Scoped Step-Up Webhook / Middleware for MCP & Agent Frameworks**: Having a 1-line middleware (`requireWorldAgentPass({ action: 'release-escrow', maxAgeSeconds: 60 })`) that automatically returns an OIDC step-up challenge URL to the agent UI and resumes the tool call once verified would cut agent integration time in half.
