# Reach & Post-Mumbai Adoption (`/arkiv/reach.md`)

## 1. Who Uses Newton's Lobby / UniLink Today?
- **Live Portal & Android App**: Newton's Lobby is live at [newtonslobby.com](https://newtonslobby.com/) alongside our Android APK (`UniLink_v8.5.apk`), built by founder **Laltesh Chaudhary** at **S-VYASA University & Newton School of Technology (NST), Bengaluru**.
- **Current Campus Footprint**:
  - **25+ Verified Builders** and **6 Active Hackathon Squads** onboarded across Indian engineering campuses (S-VYASA & NST Bengaluru, RVCE, PES University, IIIT Bangalore, IIT Bombay, IIT Delhi, BITS Pilani, DTU, and NITs).
  - **17 Live Premier Hackathons** tracked in the platform radar (including ETHGlobal Mumbai, Smart India Hackathon, ICPC Triad, and Bengaluru Tech Week).

## 2. How Users Will Use the Arkiv Layer After Mumbai
- **Replacing Insecure Firebase Rules**: Prior to ETHGlobal Mumbai, our squad coordination state lived in Firebase Realtime Database (`newtonslobby-3c54c-default-rtdb` and `uni-link-b7f2c-default-rtdb`), which repeatedly triggered open-access security alerts and accumulated dead squad listings from past hackathons.
- **Permanent Rollout Plan**:
  1. **Ephemeral Hackathon Squads**: Every new squad deployed for a 24h–48h hackathon is written to Arkiv with `expiresAt` bound to the hackathon's submission deadline. Once the hackathon ends, the recruitment listing automatically expires from the discovery radar.
  2. **Cross-Campus Open Directory**: Because Arkiv entities are public and queryable via SQL-like filters without an API key, third-party hackathon organizers, college clubs, and AI agents can query `unilink_squad` directly on Arkiv to find open student teams in Bengaluru, Delhi, or Mumbai.
