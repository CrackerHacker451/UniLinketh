# Arkiv Developer Feedback & Friction Log (`/arkiv/friction.md`)

**Project**: Newton's Lobby / UniLink OS (`https://newtonslobby.com`)  
**Track**: Build on Arkiv (Existing Projects — Continuity) & Build on Arkiv + Swarm Bonus

---

## 1. What Got in Our Way / What Broke

### Issue A: Numeric vs. String Attribute Coercion in SQL-like Filter Queries
- **What we expected**: When creating an entity with `@arkiv-network/sdk` containing `open_seats: 2` (as a JS number) and `world_id_verified: true`, we expected `client.queryEntities("open_seats > 0 AND world_id_verified = true")` to match both numeric and boolean annotations seamlessly.
- **What happened**: When annotations were serialized without explicit distinction between string attributes and numeric attributes in our initial batch import payload, range comparisons (`open_seats > 0`) returned `0` rows while exact string matches (`domain = "Web3 / Crypto"`) succeeded.
- **How to reproduce**:
  1. Connect to `https://rpc.tiramisu.db-chain.testnet.arkiv.network` (`chainId: 7738577`).
  2. Call `createEntity` passing `open_seats: "2"` inside string annotations instead of `numericAnnotations: [{ key: "open_seats", value: 2 }]`.
  3. Execute query `open_seats > 1` -> returns empty array `[]` without a schema mismatch warning.
- **Suggested Fix**: Emit a clear SDK warning or RPC diagnostic hint when a `>` / `<` operator is run against a key that exists only in `stringAnnotations`.

### Issue B: Handling Encrypted Swarm References Safely on Public Entities
- **What we expected**: A built-in helper or pattern in `@arkiv-network/sdk` for storing private off-chain blob pointers (like Swarm encrypted references).
- **What happened**: Because an encrypted Swarm reference (`64-byte hex` = `32-byte Swarm hash` + `32-byte decryption key`) embeds the decryption key directly in the reference, writing it raw into a public Arkiv entity exposes the file to anyone querying the DB-chain.
- **How we solved it**: We built a client-side WebCrypto `AES-GCM-256` wrapper (`src/integrations/arkivSwarmClient.ts`) that encrypts the Swarm reference using a squad-shared key derived from the Squad Lead's signature before writing `encrypted_swarm_ref` to Arkiv.

---

## 2. High-Impact Improvements We Would Love to See
1. **Native Reactive WebSocket Subscriptions by Filter**: In Newton's Lobby, squads watch live Kanban task changes (`status = "IN_PROGRESS"`). Being able to subscribe via `wss://rpc.tiramisu.db-chain.testnet.arkiv.network` to a specific SQL filter (`squad_ens = 'triad-alpha.unilink.eth'`) would let us completely replace Firebase Realtime Database listeners.
2. **Android / Kotlin Lightweight JSON-RPC Wrapper**: Since UniLink also ships a native Kotlin Android APK (`UniLink_v8.5.apk`), an official Kotlin/JVM wrapper or OpenAPI spec for the JSON-RPC query methods would save mobile teams from hand-rolling OkHttp JSON-RPC envelopes.
