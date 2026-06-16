package com.nexusmsg.ui.group

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.nexusmsg.R
import com.nexusmsg.adapters.ChatAdapter
import com.nexusmsg.databinding.ActivityGroupChatBinding
import com.nexusmsg.ui.call.CallActivity
import com.nexusmsg.viewmodel.ChatViewModel
import com.nexusmsg.viewmodel.WebRtcCallState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class GroupChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGroupChatBinding
    private val chatViewModel: ChatViewModel by viewModels()
    private lateinit var chatAdapter: ChatAdapter
    private var myUserId: String = ""

    private var groupId: String = ""
    private var groupName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGroupChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val intent = Intent(this, CallActivity::class.java)
        startActivity(intent)

        setupToolbar()
        setupRecyclerView()
        setupInput()
        loadGroupMessages()
        loadGroupMembers()
        observeCallState()
    }

    private fun setupToolbar() {
        binding.toolbarChat.title = groupName
        binding.toolbarChat.subtitle = getString(R.string.msg_group_encrypted)
        binding.toolbarChat.setNavigationOnClickListener { onBackPressed() }
        binding.encryptionNotice.visibility = View.VISIBLE
    }

    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter("")

        binding.recyclerMessages.apply {
            adapter = chatAdapter
            layoutManager = LinearLayoutManager(this@GroupChatActivity).apply {
                stackFromEnd = true
            }
        }
    }

    private fun setupInput() {
        binding.etMessage.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                binding.btnSend.visibility = if (s?.isNotBlank() == true) View.VISIBLE else View.GONE
            }
        })

        binding.btnSend.setOnClickListener {
            val text = binding.etMessage.text.toString().trim()
            if (text.isNotEmpty()) {
                chatViewModel.sendGroupMessage(text, groupId)
                binding.etMessage.text?.clear()
            }
        }

        binding.btnSend.visibility = View.GONE
    }

    private fun loadGroupMessages() {
        lifecycleScope.launch {
            chatViewModel.getMessages(groupId).collectLatest { messages ->
                chatAdapter.submitList(messages)
                if (messages.isNotEmpty()) {
                    binding.recyclerMessages.smoothScrollToPosition(messages.size - 1)
                }
                chatViewModel.markAsRead(groupId)
            }
        }
    }

    private fun loadGroupMembers() {
        lifecycleScope.launch {
            chatViewModel.getGroupMembers(groupId).collectLatest { members ->
                val count = members.size
                binding.toolbarChat.subtitle = if (count == 1) {
                    "1 member · Group encrypted"
                } else {
                    "$count members · Group encrypted"
                }
            }
        }
    }

    private fun observeCallState() {
        lifecycleScope.launch {
            chatViewModel.webRtcState.collectLatest { state ->
                when (state) {
                    is WebRtcCallState.Incoming -> {
                    }
                    else -> {}
                }
            }
        }
    }
}
