package com.nexusmsg.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.nexusmsg.R

class NewChatDialog : DialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return null // Uses AlertDialog
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): android.app.Dialog {
        val view = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_new_chat, null)

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle("New Chat")
            .setView(view)
            .setPositiveButton("Search") { _, _ ->
                val phoneInput = view.findViewById(R.id.etSearchPhone) as? TextInputEditText
                val phone = phoneInput?.text?.toString() ?= ""
                // Handle search
            }
            .setNegativeButton("Cancel", null)
            .create()
    }
}
