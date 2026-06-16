package com.nexusmsg.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.nexusmsg.models.Message
import com.nexusmsg.R

class ChatAdapter : RecyclerView.Adapter<ChatAdapter.ViewHolder>() {
    
    private var messages: List<Message> = emptyList()
    
    fun submitList(list: List<Message>) {
        this.messages = list
        notifyDataSetChanged()
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_message, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val message = messages[position]
        holder.messageTextView.text = message.content
    }
    
    override fun getItemCount(): Int = messages.size
    
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val messageTextView: TextView = view.findViewById(R.id.messageText)
    }
}
