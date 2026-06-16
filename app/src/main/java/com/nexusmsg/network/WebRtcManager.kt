package com.nexusmsg.network

import android.content.Context
import android.util.Log
import com.nexusmsg.models.IceServer
import com.nexusmsg.models.IceServerConfig
import org.webrtc.*
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * WebRTC peer connection manager for P2P voice/video calls via STUN/TURN.
 *
 * ICE server configuration is fetched dynamically from the server API
 * (`GET /api/v1/ice_servers`), which returns both STUN and optional TURN
 * servers with credentials.
 *
 * Flow:
 * 1. fetchIceServers() loads STUN/TURN config from server
 * 2. Caller creates PeerConnection with loaded servers, generates SDP offer
 * 3. Offer sent via WebSocket (WebRtcSignal) through server relay
 * 4. Callee receives offer, creates PeerConnection, sets remote SDP, generates answer
 * 5. ICE candidates exchanged via WebSocket
 * 6. Media streams flow P2P (or via TURN if direct P2P blocked by NAT)
 */
@Singleton
class WebRtcManager @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val TAG = "WebRtcManager"
    }

    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null
    private var localAudioTrack: AudioTrack? = null
    private var localVideoTrack: VideoTrack? = null
    private var eventListener: WebRtcEventListener? = null

    // Loaded dynamically from server API
    private var loadedIceServers: List<PeerConnection.IceServer> = emptyList()
    private var iceServersLoaded = false

    // Fallback STUN servers if API call fails
    private val fallbackIceServers = listOf(
        PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
        PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer(),
        PeerConnection.IceServer.builder("stun:stun2.l.google.com:19302").createIceServer()
    )

    interface WebRtcEventListener {
        fun onLocalSdpGenerated(sdp: String, type: String)
        fun onIceCandidate(candidate: IceCandidate)
        fun onRemoteStreamAdded(stream: MediaStream?)
        fun onCallConnected()
        fun onCallEnded()
        fun onIceServersLoaded(servers: List<IceServer>)
    }

    fun setEventListener(listener: WebRtcEventListener?) {
        this.eventListener = listener
    }

    // ─── Fetch ICE servers from the server API ───

    suspend fun fetchIceServers(apiService: ApiService, token: String): List<PeerConnection.IceServer> {
        return try {
            val response: Response<IceServerConfig> = apiService.getIceServers("Bearer $token")
            if (response.isSuccessful) {
                val config = response.body()
                config?.iceServers?.let { servers ->
                    val rtcServers = servers.map { iceServer ->
                        val builder = PeerConnection.IceServer.builder(iceServer.urls)
                        if (!iceServer.username.isNullOrEmpty()) {
                            builder.setUsername(iceServer.username)
                        }
                        if (!iceServer.credential.isNullOrEmpty()) {
                            builder.setPassword(iceServer.credential)
                        }
                        builder.createIceServer()
                    }

                    loadedIceServers = rtcServers
                    iceServersLoaded = true

                    Log.d(TAG, "Loaded ${rtcServers.size} ICE servers from API " +
                        "(STUN: ${servers.count { it.urls.firstOrNull()?.startsWith("stun:") == true }}, " +
                        "TURN: ${servers.count { it.urls.firstOrNull()?.startsWith("turn") == true }})")

                    eventListener?.onIceServersLoaded(servers)
                    rtcServers
                } ?: run {
                    Log.w(TAG, "Empty ICE server config from API, using fallback")
                    fallbackIceServers
                }
            } else {
                Log.w(TAG, "ICE server API returned ${response.code()}, using fallback")
                fallbackIceServers
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch ICE servers: ${e.message}, using fallback")
            fallbackIceServers
        }
    }

    fun getCurrentIceServers(): List<PeerConnection.IceServer> {
        return if (loadedIceServers.isNotEmpty()) loadedIceServers else fallbackIceServers
    }

    // ─── PeerConnection lifecycle ───

    fun initialize() {
        if (peerConnectionFactory != null) return

        val options = PeerConnectionFactory.InitializationOptions.builder(context)
            .setFieldTrials("")
            .createInitializationOptions()
        PeerConnectionFactory.initialize(options)

        peerConnectionFactory = PeerConnectionFactory.builder()
            .setVideoEncoderFactory(
                DefaultVideoEncoderFactory(
                    eglContext = null,
                    enableIntelVp8Encoder = true,
                    enableH264HighProfile = true
                )
            )
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(null))
            .createPeerConnectionFactory()
    }

    fun createPeerConnection(): PeerConnection? {
        peerConnection?.close()

        val iceServers = getCurrentIceServers()
        Log.d(TAG, "Creating PeerConnection with ${iceServers.size} ICE servers")

        val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
            // Aggressive ICE candidate gathering for faster NAT traversal
            iceCandidatePoolSize = 1
            tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.ENABLED
        }

        peerConnection = peerConnectionFactory?.createPeerConnection(
            rtcConfig,
            object : PeerConnection.Observer {
                override fun onIceCandidate(candidate: IceCandidate?) {
                    candidate?.let { eventListener?.onIceCandidate(it) }
                }

                override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}

                override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {
                    Log.d(TAG, "ICE gathering state: $state")
                }

                override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {
                    Log.d(TAG, "ICE connection state: $state")
                    when (state) {
                        PeerConnection.IceConnectionState.CONNECTED ->
                            eventListener?.onCallConnected()
                        PeerConnection.IceConnectionState.CLOSED,
                        PeerConnection.IceConnectionState.FAILED ->
                            eventListener?.onCallEnded()
                        else -> {}
                    }
                }

                override fun onIceConnectionReceivingChange(receiving: Boolean) {}

                override fun onSignalingChange(state: PeerConnection.SignalingState?) {
                    Log.d(TAG, "Signaling state: $state")
                }

                override fun onAddStream(stream: MediaStream?) {
                    eventListener?.onRemoteStreamAdded(stream)
                }

                override fun onRemoveStream(stream: MediaStream?) {}

                override fun onDataChannel(channel: DataChannel?) {}

                override fun onRenegotiationNeeded() {}

                override fun onAddTrack(
                    receiver: RtpReceiver?,
                    streams: Array<out MediaStream>?
                ) {
                    eventListener?.onRemoteStreamAdded(streams?.firstOrNull())
                }
            }
        )

        return peerConnection
    }

    // ─── SDP negotiation ───

    fun createOffer(callId: String) {
        val pc = peerConnection ?: return

        val constraints = MediaConstraints()
        pc.createOffer(object : SdpObserver {
            override fun onCreateSuccess(sessionDescription: SessionDescription?) {
                sessionDescription?.let { sdp ->
                    pc.setLocalDescription(object : SdpObserver {
                        override fun onSetSuccess() {
                            eventListener?.onLocalSdpGenerated(sdp.description, "offer")
                        }
                        override fun onCreateSuccess(p0: SessionDescription?) {}
                        override fun onSetFailure(p0: String?) {}
                        override fun onCreateFailure(p0: String?) {}
                    }, sdp)
                }
            }
            override fun onSetSuccess() {}
            override fun onCreateFailure(error: String?) {
                Log.e(TAG, "createOffer failed: $error")
            }
            override fun onSetFailure(error: String?) {}
        }, constraints)
    }

    fun createAnswer(callId: String) {
        val pc = peerConnection ?: return

        val constraints = MediaConstraints()
        pc.createAnswer(object : SdpObserver {
            override fun onCreateSuccess(sessionDescription: SessionDescription?) {
                sessionDescription?.let { sdp ->
                    pc.setLocalDescription(object : SdpObserver {
                        override fun onSetSuccess() {
                            eventListener?.onLocalSdpGenerated(sdp.description, "answer")
                        }
                        override fun onCreateSuccess(p0: SessionDescription?) {}
                        override fun onSetFailure(p0: String?) {}
                        override fun onCreateFailure(p0: String?) {}
                    }, sdp)
                }
            }
            override fun onSetSuccess() {}
            override fun onCreateFailure(error: String?) {
                Log.e(TAG, "createAnswer failed: $error")
            }
            override fun onSetFailure(error: String?) {}
        }, constraints)
    }

    fun setRemoteSdp(sdp: String, type: String) {
        val sdpType = when (type) {
            "offer" -> SessionDescription.Type.OFFER
            "answer" -> SessionDescription.Type.ANSWER
            else -> return
        }

        peerConnection?.setRemoteDescription(object : SdpObserver {
            override fun onSetSuccess() {}
            override fun onCreateSuccess(p0: SessionDescription?) {}
            override fun onSetFailure(error: String?) {
                Log.e(TAG, "setRemoteDescription failed: $error")
            }
            override fun onCreateFailure(p0: String?) {}
        }, SessionDescription(sdpType, sdp))
    }

    // ─── ICE candidates ───

    fun addIceCandidate(sdpMid: String, sdpMLin