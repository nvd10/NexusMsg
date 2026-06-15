# NexusMsg — End-to-End Encrypted Android Messenger

A WhatsApp-style Android messenger with full end-to-end encryption where the server acts only as a relay.

## Features

### HTTPS + WebSocket (wss://)
- All communication uses **HTTPS** for REST API and **WSS** (WebSocket Secure) for real-time messaging

### STUN based P2P Voice/Video Calls
- WebRTC peer-to-peer calling using **Google STUN servers** (free)
- **TURN relay** optionally configured via server env vars
- ICE server configuration fetched dynamically from `GET /api/v1/ice_servers`
- Server acts only as a **signaling relay** — media flows directly between peers

### 7-Character User ID Lookup
- Each user gets a unique **7-character Nexus ID** (e.g., `A1b2C3d`)
- Find and add contacts by entering their Nexus ID

### Group Chat
- Create groups with custom names
- Join groups by Group ID
- Group messages encrypted with derived group key

## Tech Stack
- **Android**: Kotlin, MVMM, Hilt DI, Room, Retrofit, OkHttp, WebRTC
- **Server**: Node.js, Express, WebSocket, SQLite

## Building the APK
`h`sh
./gradlew assembleDebug
```