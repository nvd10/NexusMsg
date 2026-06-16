package com.nexusmsg.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.nexusmsg.models.Group
import com.nexusmsg.R

class GroupAdapter : RecyclerView.Adapter<GroupAdapter.ViewHolder>() {
    
    private var groups: List<Group> = emptyList()
    
    fun submitList(list: List<Group>) {
        this.groups = list
        notifyDataSetChanged()
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_group, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val group = groups[position]
        holder.nameTextView.text = group.name
    }
    
    override fun getItemCount(): Int = groups.size
    
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameTextView: TextView = view.findViewById(R.id.groupName)
    }
}
