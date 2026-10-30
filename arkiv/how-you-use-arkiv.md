# How Newton's Lobby Uses Arkiv (`/arkiv/how-you-use-arkiv.md`)

## 1. Alignment of Filters, Ownership, and Expiration with Product Mechanics

| Newton's Lobby Feature | Arkiv Entity | Filter Fields Used in UI | Who Can Edit (`owner`) | Expiration Policy (`expiresAt`) |
| :--- | :--- | :--- | :--- | :--- |
| **Squad Radar & Recruiting** | `unilink_squad` | `domain`, `university`, `open_seats > 0`, `world_id_verified = true` | Squad Lead's Wallet (`0x...`) | Exact timestamp of the target hackathon's submission deadline |
| **Squad Kanban & Agent Tasks** | `unilink_sprint_task` | `squad_ens`, `status`, `priority_tier`, `x402_bounty_usdc > 0` | Squad Lead or Assigned Builder Wallet | Hackathon deadline + 72 hours (post-sprint review window) |
| **Barter Block (Hostel Hardware)** | `unilink_barter_listing` | `category`, `campus_zone`, `rate_usdc_x402 <= budget` | Hardware Owner's Student Wallet | Rental return deadline (e.g., 48 hours for a weekend hackathon) |

## 2. How Each Arkiv Feature Changes What Users See in the UI
- **SQL-Like Filtering (`client.queryEntities`)**: When a student clicks **🤖 AI / ML**, **⛓️ Web3 / Crypto**, or **⚡ Open Seats Only** on the Newton's Lobby portal, the UI executes a live filter query against Arkiv's `stringAnnotations` and `numericAnnotations` and renders the matching entities in real time.
- **Entity Ownership Badges**: Every Squad Card and Barter Listing displays its Arkiv `owner` wallet and ENSv2 subname (`triad-alpha.unilink.eth`). Edit/Disband buttons are cryptographically gated by matching the connected wallet against the Arkiv entity's `owner`.
- **Live TTL Countdown**: Every Squad Base Camp card renders a live countdown timer driven directly by the Arkiv entity's `expiresAt` timestamp, visually warning builders before a recruitment window or task bounty expires.
