package com.nexusmsg.ui.register

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.nexusmsg.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RegisterActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
    }
}
