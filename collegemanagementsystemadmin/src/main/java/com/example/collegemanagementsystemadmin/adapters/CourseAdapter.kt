package com.example.collegemanagementsystemadmin.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.collegemanagementsystemadmin.R
import com.example.collegemanagementsystemadmin.models.CourseUi

class CourseAdapter(
    private val items: MutableList<CourseUi>,
    private val onClick: (CourseUi) -> Unit
) : RecyclerView.Adapter<CourseAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvDeptName)
        val tvCode: TextView = view.findViewById(R.id.tvDeptCode)
        val tvDuration: TextView = view.findViewById(R.id.tvLevels)
        val tvStatus: TextView = view.findViewById(R.id.tvStatus)
        val cardRoot: View = view.findViewById(R.id.cardRoot)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_course, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        holder.tvName.text = item.name.ifEmpty { "Unnamed Course" }
        holder.tvCode.text = "Code: ${item.code}"
        holder.tvDuration.text = item.years
        holder.tvStatus.text = item.status.ifEmpty { "Active" }

        // Change status badge color based on status
        if (item.status.equals("Inactive", ignoreCase = true)) {
            holder.tvStatus.setBackgroundResource(R.drawable.bg_badge_inactive)
            holder.tvStatus.setTextColor(Color.parseColor("#DC2626"))
        } else {
            holder.tvStatus.setBackgroundResource(R.drawable.bg_badge_status)
            holder.tvStatus.setTextColor(Color.parseColor("#6B4BFF"))
        }

        holder.cardRoot.setOnClickListener { onClick(item) }
    }

    override fun getItemCount() = items.size

    fun replaceAll(newItems: List<CourseUi>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
}