package com.nexusmsg.ui.chat

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.nexusmsg.adapters.ChatAdapter
import com.nexusmsg.databinding.ActivityChatBinding
import com.nexusmsg.viewmodel.ChatViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatBinding
    private val chatViewModel: ChatViewModel by viewModels()
    private lateinit var chatAdapter: ChatAdapter

    private var recipientId: String = ""
    private var recipientName: String = ""
    private var currentUserId: String = ""
    private var chatId: String = ""
    private var typingJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        recipientId = intent.getStringExtra("user_id") ?? ""
        recipientName = intent.getStringExtra("user_name") ?? "Chat"

        setupToolbar()
        setupRecyclerView()
        setupMessageInput()
        loadMessages()
    }

    private fun setupToolbar() {
        binding.toolbarChat.title = recipientName
        binding.toolbarChat.subtitle = getString(com.nexusmsg.R.string.msg_end_to_end_encrypted)
        binding.toolbarChat.setNavigationOnClickListener { onBackPressed() }
    }

    private fun setupRecyclerView() {
        currentUserId = ""
        chatAdapter = ChatAdapter(currentUserId)

        binding.recyclerMessages.apply {
            adapter = chatAdapter
            layoutManager = LinearLayoutManager(this@ChatActivity).apply {
                stackFromEnd = true
            }
        }
    }

    private fun setupMessageInput() {
        binding.etMessage.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val hasText = s?.isNotBlank() == true
                binding.btnSend.visibility = if (hasText) View.VISIBLE else View.GONE
            }
        })

        binding.btnSend.setOnClickListener {
            val text = binding.etMessage.text.toString().trim()
            if (text.isNotEmpty()) {
                chatViewModel.sendMessage(text, recipientId)
                binding.etMessage.text?.clear()
            }
        }

        binding.btnSend.visibility = View.GONE
    }

    private fun loadMessages() {
        lifecycleScope.launch {
            chatId = ChatViewModel.getChatId(currentUserId, recipientId)

            chatViewModel.getMessages(chatId).collectLatest { messages ->
                chatAdapter.submitList(messages)
                if (messages.isNotEmpty()) {
                    binding.recyclerMessages.smoothScrollToPosition(messages.size - 1)
                }
                chatViewModel.markAsRead(chatId)
            }
        }
    }
}
