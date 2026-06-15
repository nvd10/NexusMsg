package com.nexusmsg.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.nexusmsg.adapters.ContactAdapter
import com.nexusmsg.adapters.GroupAdapter
import com.nexusmsg.databinding.ActivityMainBinding
import com.nexusmsg.ui.chat.ChatActivity
import com.nexusmsg.ui.group.GroupChatActivity
import com.nexusmsg.ui.register.RegisterActivity
import com.nexusmsg.viewmodel.AuthState
import com.nexusmsg.viewmodel.AuthViewModel
import com.nexusmsg.viewmodel.ChatViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    @Inject
    lateinit var chatViewModel: ChatViewModel

    private val authViewModel: AuthViewModel by viewModels()
    private lateinit var contactAdapter: ContactAdapter
    private lateinit var groupAdapter: GroupAdapter

    private var currentTab = 0 // 0 = Chats, 1 = Groups

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerViews()
        setupTabs()
        observeAuthState()
    }

    private fun setupRecyclerViews() {
        contactAdapter = ContactAdapter { contact ->
            if (contact.isGroup) {
                val intent = Intent(this, GroupChatActivity::java.class).apply {
                    putExtra("group_id", contact.userId)
                    putExtra("group_name", contact.name)
                }
                startActivity(intent)
            } else {
                val intent = Intent(this, ChatActivity::java.class).apply {
                    putExtra("user_id", contact.userId)
                    putExtra("user_name", contact.name)
                }
                startActivity(intent)
            }
        }

        binding.recyclerChats.apply {
            adapter = contactAdapter
            layoutManager = LinearLayoutManager(this@MainActivity)
            setHasFixedSize(true)
        }

        groupAdapter = GroupAdapter { group ->
            val intent = Intent(this, GroupChatActivity::java.class).apply {
                putExtra("group_id", group.groupId)
                putExtra("group_name", group.name)
            }
            startActivity(intent)
        }

        binding.recyclerGroups.apply {
            adapter = groupAdapter
            layoutManager = LinearLayoutManager(this@MainActivity)
            setHasFixedSize(true)
        }
    }

    private fun setupTabs() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                currentTab = tab?.position ?? 0
                when (currentTab) {
                    0 -> {
                        binding.recyclerChats.visibility = View.VISIBLE
                        binding.recycerGroups.visibility = View.GONE
                        binding.fabNewChat.setImageResource(com.nexusmsg.R.drawable.ic_chat)
                    }
                    1 -> {
                        binding.recyclerChats.visibility = View.GONE
                        binding.recyclerGroups.visibility = View.VISIBLE
                        binding.fabNewChat.setImageResource(com.nexusmsg.R.drawable.ic_group)
                    }
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun observeAuthState() {
        lifecycleScope.launch {
            authViewModel.authState.collectLatest { state ->
                when (state) {
                    is AuthState.NotRegistered -> {
                        startActivity(Intent(this@MainActivity, RegisterActivity::java.class))
                        finish()
                    }
                    is AuthState.LoggedIn -> {
                        setupChatList(state.userId, state.token)
                    }
                    is AuthState.PendingApproval -> {
                        startActivity(Intent(this@MainActivity, RegisterActivity::java.class).apply {
                            putExtra("pending", true)
                        })
                        finish()
                    }
                    is AuthState.Error -> {
                        Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun setupChatList(userId: String, token: String) {
        chatViewModel.initialize(userId, token)
        chatViewModel.loadContacts()
        chatViewModel.loadGroups()

        lifecycleScope.launch {
            chatViewModel.contacts.collectLatest { contacts ->
                contactAdapter.submitList(contacts)
            }
        }

        lifecycleScope.launch {
            chatViewModel.groups.collectLatest { groups ->
                groupAdapter.submitList(groups)
            }
        }

        binding.fabNewChat.setOnClickListener {
            when (currentTab) {
                0 -> NewChatDialog().show(supportFragmentManager, "NewChat")
                1 -> NewGroupDialog().show(supportFragmentManager, "NewGroup")
            }
        }
    }
}
