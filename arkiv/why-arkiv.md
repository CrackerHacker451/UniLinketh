# Why Arkiv Instead of Postgres or Firebase (`/arkiv/why-arkiv.md`)

## 1. What Users Would Lose If We Swapped Arkiv Back to Firebase / Postgres
1. **Cryptographic Wallet Ownership of Squads & Bounties**: In Firebase/Postgres, a single misconfigured rule or compromised admin key allows anyone to overwrite or delete every student's squad (`newtonslobby-3c54c-default-rtdb` had this exact pain point). On Arkiv, each `unilink_squad`, `unilink_sprint_task`, and `unilink_barter_listing` is owned at the protocol level by the student's wallet (`owner`), so only the verified Squad Lead can modify open seats or disband the squad.
2. **Protocol-Enforced Time-To-Live (`expiresAt`) for Ephemeral Sprints**: Hackathon squad formation, speed-networking queues (3-minute windows), and weekend hardware rentals are inherently temporary. Arkiv's native `expiresAt` TTL purges expired squads and claims at the consensus layer without centralized cron jobs or stale zombie listings cluttering the radar.
3. **Permissionless Multi-Agent & Cross-App Querying**: In our AI Guider workflow, external hackathon scout agents and partner dApps can query `unilink_squad` directly on Arkiv RPC using SQL-like filters without needing database credentials from us.

## 2. What We Kept Off Arkiv (And Why)
- **WebRTC Audio/Video Media Streams (`Live Stage` & `Speed Video Match`)**: Kept on P2P Port-443 STUN/TURN relays because real-time 1080p video packets require sub-50ms UDP transport, not blockchain persistence.
- **Raw Student ID Card Photos & High-Res Project Screenshots**: Stored as client-side AES-GCM encrypted blobs on **Swarm (`ethswarm.org`)**, with only the encrypted 64-byte reference stored inside the Arkiv entity. This keeps Arkiv entities lightweight and protects student PII (phone numbers and physical college ID cards).
- **World ID Nullifier Replay Table**: Kept in our backend verifier database alongside on-chain proof verification so RP signing keys (`RP_SIGNING_KEY`) are never exposed to the client.
