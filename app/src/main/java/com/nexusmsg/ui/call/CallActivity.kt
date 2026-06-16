package com.nexusmsg.ui.call

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.nexusmsg.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CallActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_call)
    }
}
