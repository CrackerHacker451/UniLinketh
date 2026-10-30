# Arkiv Entity Schema — Newton's Lobby (UniLink OS)

**Testnet Network**: Arkiv Tiramisu DB-Chain Testnet (`chainId: 7738577`)  
**RPC Endpoint**: `https://rpc.tiramisu.db-chain.testnet.arkiv.network`  
**Writer / Deployer Wallet**: `0x71C95911E9a5D330f4D621842EC243EE1343292e`

---

## 1. Entity: `unilink_squad` (Hackathon Squad Base Camp)
Stores active hackathon squads looking for verified student builders. Automatically expires when the hackathon team-formation or sprint deadline passes.

| Field Name | Type | Indexed / Filterable | Description |
| :--- | :--- | :--- | :--- |
| `entity_key` | `bytes32` | Primary Key | Deterministic hash of `squadId` |
| `squad_name` | `string` | Yes (`CONTAINS`) | Human-readable squad name (e.g., `"Triad Alpha — NST"`) |
| `ens_subname` | `string` | Yes (`=`) | ENSv2 subname (`triad-alpha.unilink.eth`) |
| `target_hackathon` | `string` | Yes (`=`) | e.g., `"ETHGlobal Mumbai 2026"`, `"SIH 2026"`, `"ICPC Triad"` |
| `domain` | `string` | Yes (`=`) | `"Web3 / Crypto"`, `"AI / ML"`, `"Full-Stack Web"`, `"Cyber / System"` |
| `university` | `string` | Yes (`=`) | Lead's verified institution (e.g., `"S-VYASA & Newton School of Technology"`) |
| `open_seats` | `uint8` | Yes (`>`, `<=`) | Remaining open builder slots (`0` to `6`) |
| `world_id_verified` | `bool` | Yes (`=`) | `true` if Squad Lead passed World ID v4 Proof of Human |
| `encrypted_swarm_ref` | `string` | No | Client-side AES-GCM ciphertext of the Swarm `bzz://` reference for squad blueprint |
| `hedera_hcs_topic` | `string` | Yes (`=`) | Linked Hedera Consensus Service topic ID (e.g., `0.0.5189204`) |
| `owner` | `address` | Native Arkiv Owner | Squad Lead's wallet address (only this wallet can mutate open seats or disband) |
| `expiresAt` | `uint64` | Native Arkiv TTL | Unix timestamp of the hackathon submission deadline |

---

## 2. Entity: `unilink_sprint_task` (Ephemeral Kanban & Agent Bids)
Stores sprint deliverables and autonomous agent task claims inside a Squad Base Camp. Expires 72 hours after sprint completion.

| Field Name | Type | Indexed / Filterable | Description |
| :--- | :--- | :--- | :--- |
| `task_id` | `string` | Primary Key | Unique task identifier |
| `squad_ens` | `string` | Yes (`=`) | Parent squad ENSv2 name (`triad-alpha.unilink.eth`) |
| `title` | `string` | No | Deliverable description |
| `status` | `string` | Yes (`=`) | `"TO_DO"`, `"IN_PROGRESS"`, `"SHIPPED"` |
| `priority_tier` | `uint8` | Yes (`=`) | `1` (Critical), `2` (Standard), `3` (Low) |
| `assigned_builder` | `string` | Yes (`=`) | Builder ENSv2 name (`laltesh.nst.unilink.eth`) or Agent (`guider.triad-alpha.unilink.eth`) |
| `x402_bounty_usdc` | `uint64` | Yes (`>`) | Attached Hedera x402 bounty in USDC micro-units (6 decimals) |
| `agent_pass_approved` | `bool` | Yes (`=`) | Whether World ID Agent Pass step-up auth validated this assignment |
| `owner` | `address` | Native Arkiv Owner | Task creator or Squad Lead wallet |
| `expiresAt` | `uint64` | Native Arkiv TTL | Sprint end timestamp + 259,200 seconds (72h grace window) |

---

## 3. Entity: `unilink_barter_listing` (Barter Block & Hiring Board Offer)
Stores peer-to-peer hostel hardware rentals (Arduino, GPUs, Raspberry Pi) and "Hire Me" tutoring slots.

| Field Name | Type | Indexed / Filterable | Description |
| :--- | :--- | :--- | :--- |
| `listing_id` | `string` | Primary Key | Listing identifier |
| `category` | `string` | Yes (`=`) | `"HARDWARE_BARTER"` or `"SKILL_TUTORING"` |
| `item_title` | `string` | Yes (`CONTAINS`) | e.g., `"NVIDIA Jetson Nano 4GB"` or `"Solidity + x402 1-on-1 Debug"` |
| `campus_zone` | `string` | Yes (`=`) | Geofenced hostel/lab zone (e.g., `"NST Bengaluru Block P3"`) |
| `rate_usdc_x402` | `uint64` | Yes (`<=`) | Price per session/hour settled via Hedera x402 |
| `encrypted_photo_swarm` | `string` | No | AES-GCM encrypted Swarm hash of hardware photo / college receipt |
| `owner` | `address` | Native Arkiv Owner | Student issuer's wallet address |
| `expiresAt` | `uint64` | Native Arkiv TTL | Listing availability deadline |

---

## Sample Runnable Query
```sql
SELECT entity_key, squad_name, ens_subname, university, open_seats, hedera_hcs_topic, expiresAt
FROM unilink_squad
WHERE domain = 'Web3 / Crypto'
  AND open_seats > 0
  AND world_id_verified = true
  AND expiresAt > 1793404800
ORDER BY expiresAt ASC;
```
