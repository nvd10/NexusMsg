package com.nexusmsg.ui.group

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.nexusmsg.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class GroupChatActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_group_chat)
    }
}
