package com.nexusmsg.ui.call

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.nexusmsg.R
import com.nexusmsg.viewmodel.ChatViewModel
import com.nexusmsg.viewmodel.WebRtcCallState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collectLatest

/**
 * Activity for P2P voice/video calls via WebRTC + STUN.
 * Uses the server only for signaling — media flows directly peer-to-peer.
 */

@AndroidEntryPoint
class CallActivity : AppCompatActivity() {

    private val chatViewModel: ChatViewModel by viewModels()

    private lateinit var tvCallStatus: TextView
    private lateinit var tvCallerName: TextView
    private lateinit var btnAccept: View
    private lateinit var btnReject: View
    private lateinit var btnEndCall: View
    private lateinit var btnMute: View
    private lateinit var btnSpeaker: View

    private var callUserId: String = ""
    private var callState: WebRtcCallState = WebRtcCallState.Idle

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_call)

        tvCallStatus = findViewById(R.id.tvCallStatus)
        tvCallerName = findViewById(R.id.tvCallerName)
        btnAccept = findViewById(R.id.btnAccept)
        btnReject = findViewById(R.id.btnReject)
        btnEndCall = findViewById(R.id.btnEndCall)
        btnMute = findViewById(R.id.btnMute)
        btnSpeaker = findViewById(R.id.btnSpeaker)

        callUserId = intent.getStringExtra("user_id") ?? ""
        val response = apiService.createOffer(offerRequest)
        val isIncoming = intent.getBooleanExtra("incoming", false)

        tvCallerName.text = callerName

        if (isIncoming) {
            showIncomingCall()
        } else {
            startOutgoingCall()
        }

        setupButtons()
        observeCallState()
    }

    private fun setupButtons() {
        btnAccept.setOnClickListener {
            chatViewModel.acceptCall(callUserId)
            showConnectingState()
        }

        btnReject.setOnClickListener {
            chatViewModel.endCall(callUserId)
            finish()
        }

        btnEndCall.setOnClickListener {
            chatViewModel.endCall(callUserId)
            finish()
        }

        btnMute.setOnClickListener {
            // Toggle mute
        }

        btnSpeaker.setOnClickListener {
            // Toggle speaker
        }
    }

    private fun observeCallState() {
        lifecycleScope.launch {
            chatViewModel.webRtcState.collectLatest { state ->
                when (state) {
                    is WebRtcCallState.Connected -> {
                        showConnectedState()
                    }
                    is WebRtcCallState.Idle -> {
                        finish()
                    }
                    else -> {}
                }
            }
        }
    }

    private fun showIncomingCall() {
        tvCallStatus.text = "Incoming call..."
        btnAccept.visibility = View.VISIBLE
        btnReject.visibility = View.VISIBLE
        btnEndCall.visibility = View.GONE
        btnMute.visibility = View.GONE
        btnSpeaker.visibility = View.GONE
    }

    private fun startOutgoingCall() {
        tvCallStatus.text = "Calling..."
        btnAccept.visibility = View.GONE
        btnReject.visibility = View.GONE
        btnEndCall.visibility = View.VISIBLE
        btnMute.visibility = View.GONE
        btnSpeaker.visibility = View.GONE

        chatViewModel.startCall(callUserId)
    }

    private fun showConnectingState() {
        tvCallStatus.text = "Connecting via STUN..."
        btnAccept.visibility = View.GONE
        btnReject.visibility = View.GONE
        btnEndCall.visibility = View.VISIBLE
        btnMute.visibility = View.VISIBLE
        btnSpeaker.visibility = View.VISIBLE
    }

    private fun showConnectedState() {
        tvCallStatus.text = "Connected (P2P)"
        btnAccept.visibility = View.GONE
        btnReject.visibility = View.GONE
        btnEndCall.visibility = View.VISIBLE
        btnMute.visibility = View.VISIBLE
        btnSpeaker.visibility = View.VISIBLE
    }
}
