package com.nexusmsg.adapters

import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nexusmsg.databinding.ItemContactBinding
import com.nexusmsg.models.Contact
import java.util.

class ContactAdapter(
    private val onContactClick: (Contact) -> Unit
) : ListAdapter<Contact, ContactAdapter.ContactViewHolder>(ContactDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val binding = ItemContactBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ContactViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        holder.bind(getItem(position), onContactClick)
    }

    class ContactViewHolder(
        private val binding: ItemContactBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(contact: Contact, onClick: (Contact) -> Unit) {
            binding.tvIname.text = contact.name
            binding.tvLastMessage.text = contact.lastMessage
            binding.tvTime.text = formatTime(contact.lastMessageTime)

            // Avatar letter
            binding.tvAvatar.text = contact.name.take(1).uppercase(Locale.getDefault())

            // Online status
            binding.dotOnline.visibility = if (contact.online) {
                android.view.View.VISIBLE
            } else {
                android.view.View.GONE
            }

            // Unread badge
            if (contact.unreadCount > 0) {
                binding.unreadBadge.visibility = android.view.View.VISIBLE
                binding.unreadBadge.text = if (contact.unreadCount > 99) "99+" else contact.unreadCount.toString()
            } else {
                binding.unreadBadge.visibility = android.view.View.GONE
            }

            binding.root.setOnClickListener { onClick(contact) }
        }

        private fun formatTime(timestamp: Long): String {
            if (timestamp == 0L) return ""
            val now = Calendar.getInstance()
            val msgTime = Calendar.getInstance().apply { timeInMillis = timestamp }

            return if (now.get(Calendar.DAY_OF_YEAR) == msgTime.get(Calendar.DAY_OF_YEAR) &&
                now.get(Calendar.YEAR) == msgTime.get(Calendar.YEAR)) {
                DateFormat.format("HH:mm", msgTime).toString()
            } else if (now.get(Calendar.YEAR) == msgTime.get(Calendar.YEAR)) {
                DateFormat.format("MMM dd", msgTime).toString()
            } else {
                DateFormat.format("dd/MM/yyyy", msgTime).toString()
            }
        }
    }

    private class ContactDiffCallback : DiffUtil.ItemCallback<Contact>() {
        override fun areItemsTheSame(oldItem: Contact, newItem: Contact): Boolean =
            oldItem.userId == newItem.userId

        override fun areContentsTheSame(oldItem: Contact, newItem: Contact): Boolean =
            oldItem == newItem
    }
}
