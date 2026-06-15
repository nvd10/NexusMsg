package com.nexusmsg.ui.register

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.nexusmsg.R
import com.nexusmsg.databinding.ActivityRegisterBinding
import com.nexusmsg.ui.MainActivity
import com.nexusmsg.viewmodel.AuthState
import com.nexusmsg.viewmodel.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

@AndroidEntryPoint
class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val isPending = intent.getBooleanExtra("pending", false)
        if (isPending) {
            binding.tvStatus.text = getString(R.string.msg_registration_pending)
            binding.tvStatus.visibility = View.VISIBLE
        }

        setupButtons()
        observeState()
    }

    private fun setupButtons() {
        binding.btnRegister.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            val phone = binding.etPhone.text.toString().trim()
            val username = binding.etUsername.text.toString().trim()

            when {
                name.isEmpty() -> {
                    binding.tilName.error = "Name is required"
                }
                phone.isEmpty() -> {
                    binding.tilPhone.error = "Phone number is required"
                }
                username.isEmpty() -> {
                    binding.tilUsername.error = "Username is required"
                }
                else -> {
                    binding.tilName.error = null
                    binding.tilPhone.error = null
                    binding.tilUsername.error = null
                    authViewModel.requestRegistration(name, phone, username)
                }
            }
        }

        binding.btnLogin.setOnClickListener {
            val phone = binding.etLoginPhone.text.toString().trim()
            val username = binding.etLoginUsername.text.toString().trim()

            when {
                phone.isEmpty() -> {
                    binding.tilLoginPhone.error = "Phone number is required"
                }
                username.isEmpty() -> {
                    binding.tilLoginUsername.error = "Username is required"
                }
                else -> {
                    binding.tilLoginPhone.error = null
                    binding.tilLoginUsername.error = null
                    authViewModel.login(phone, username)
                }
            }
        }
    }

    private fun observeState() {
        lifecycleScope.launch {
            authViewModel.authState.collectLatest { state ->
                binding.progressBar.visibility = View.GONE
                when (state) {
                    is AuthState.LoggedIn -> {
                        startActivity(Intent(this@RegisterActivity, MainActivity::java.class))
                        finish()
                    }
                    is AuthState.PendingApproval -> {
                        binding.tvStatus.text = state.toString().let {
                            "Registration request submitted. Please wait for admin approval."
                        }
                        binding.tvStatus.visibility = View.VISIBLE
                    }
                    is AuthState.Error -> {
                        Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                    }
                    else -> {}
                }
            }
        }

        lifecycleScope.launch {
            authViewModel.isLoading.collectLatest { isLoading ->
                binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            }
        }
    }
}
