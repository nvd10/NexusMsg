# Building the NexusMsg APK (arm64-v8a)

## Prerequisites

1. **JDK 17** — Download from https://adoptium.net/
2. **Android Studio** (or Android SDK Command-line Tools)
   - SDK Platform 34
   - Build Tools 34.0.0
   - NDK 26.1.10909125
3. Set `ANDROID_HOME` environment variable to your SDK path

## Quick Build

```bash
# Create local.properties with your SDK path:
echo "sdk.dir=$ANDROID_HOME" > local.properties
echo "ndk.dir=$ANDROID_HOME/ndk/26.1.10909125" >> local.properties

# Build the debug APK:
/gradlew assembleDebug

# The APK will be at:
# app/build/outputs/apk/debug/app-arm64-v8a-debug.apk
```

## Features Enabled in APK

- `*HTTPS + WSS/`* — All API calls over HTTPS, real-time messaging via secure WebSocket
- **E2E Encryption** — EADH + AES-256-GCM, server never sees plaintext
- **STUN P2P Calls** — WebRTC voice/video with Google STUN servers
- **7-char User IDs** — Find contacts by their Nexus ID
- **Group Chat** — Create/join encrypted groups
- **arm64-v8a only** — Optimized APK size

## Server Setup

```bash
cd server
npm install
cp .env.example .env
# Edit .env with JWT_SECRET and ADMIN_API_KEY
npm start
```

## Admin Commands

```bash
node admin.js pending        # List pending registrations
node admin.js approve --phone +1234567890
```
