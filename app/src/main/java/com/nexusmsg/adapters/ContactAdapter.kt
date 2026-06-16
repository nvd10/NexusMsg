package com.nexusmsg.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.nexusmsg.models.Contact
import com.nexusmsg.R

class ContactAdapter : RecyclerView.Adapter<ContactAdapter.ViewHolder>() {
    
    private var contacts: List<Contact> = emptyList()
    
    fun submitList(list: List<Contact>) {
        this.contacts = list
        notifyDataSetChanged()
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_contact, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val contact = contacts[position]
        holder.nameTextView.text = contact.name
    }
    
    override fun getItemCount(): Int = contacts.size
    
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameTextView: TextView = view.findViewById(R.id.contactName)
    }
}
