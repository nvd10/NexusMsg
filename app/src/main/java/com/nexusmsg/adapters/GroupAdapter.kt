package com.nexusmsg.adapters

import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.cyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nexusmsg.databinding.ItemGroupBinding
import com.nexusmsg.models.GroupEntity
import com.nexusmsg.models.Group

class GroupAdapter(
    private val onGroupClick: (GroupEntity) -> Unit
) : ListAdapter<GroupEntity, GroupAdapter.GroupViewHolder>(GroupDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val binding = ItemGroupBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return GroupViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        holder.bind(getItem(position), onGroupClick)
    }

    class GroupViewHolder(
        private val binding: ItemGroupBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(group: GroupEntity, onClick: (GroupEntity) -> Unit) {
            binding.tvGroupName.text = group.name
            binding.tvGroupTime.text = formatTime(group.lastMessageTime)

            // Last message preview
            if (group.lastMessage.isNotEmpty()) {
                binding.tvGroupLastMessage.text = group.lastMessage
                binding.tvGroupLastMessage.visibility = android.view.View.VISIBLE
            } else {
                binding.tvGroupLastMessage.visibility = android.view.View.GONE
            }

            // Member count
            binding.tvMemberCount.text = if (group.memberCount == 1) {
                "1 member"
            } else {
                "${group.memberCount} members"
            }

            // Unread badge
            if (group.unreadCount > 0) {
                binding.unreadBadge.visibility = android.view.View.VISIBLE
                binding.unreadBadge.text = if (group.unreadCount > 99) "99+" else group.unreadCount.toString()
            } else {
                binding.unreadBadge.visibility = android.view.View.GONE
            }

            binding.root.setOnClickListener { onClick(group) }
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

    private class GroupDiffCallback : DiffUtil.ItemCallback<GroupEntity>() {
        override fun areItemsTheSame(oldItem: GroupEntity, newItem: GroupEntity): Boolean =
            oldItem.groupId == newItem.groupId

        override fun areContentsTheSame(oldItem: GroupEntity, newItem: GroupEntity): Boolean =
            oldItem == newItem
    }
}
