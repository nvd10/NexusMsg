# NexusMsg — End-to-End Encrypted Android Messenger

A whatsApp-style Android messenger with full end-to-end encryption where the server acts only as a relay.

## New Features Added

### HTTPS + WebSocket (wss://)
- All communication uses **HTTPS** for REST API and **WSS** (WebSocket Secure) for real-time messaging
- Certificate pinning configured in `network_security_config.xml`

#### STUN-based P2P Voice/Video Calls (with TURN support)
- WebRTC peer-to-peer calling using **Google STUN servers** (free)
- **TURN relay** optionally configured via server env vars for NAT traversal
- ICE server configuration fetched dynamically from `GET /api/v1/ice_servers`
- Server acts only as a **signaling relay** — media flows directly between peers
- Call flow: offer/answer SDP exchange via WebSocket → ICE candidates via STUN → direct P2P media

#### 7-Character User ID Lookup
- Each user gets a unique **7-character Nexus ID** on registration (e.g., `A1b2C3d`)
- Find and add contacts by entering their Nexus ID
- No need to share phone numbers

### Group Chat
- Create groups with custom names
- Join groups by Group ID
- Group messages encrypted with derived group key
- View group members and roles

## Architecture

```
✔────────────────────────────────────────────────────────────────────────────────────────────────────────────┤─────────────────────────────────────────────────────────────────────────────────────────────────────────────━✕
─                        |         ─                          |