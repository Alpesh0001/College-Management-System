package com.example.collegemanagementsystemadmin.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.collegemanagementsystemadmin.R
import com.example.collegemanagementsystemadmin.models.Faculty

class FacultyAdapter(
    private var faculties: List<Faculty>,
    private val onItemClick: (Faculty) -> Unit,
    private val onEditClick: (Faculty) -> Unit,
    private val onDeleteClick: (Faculty) -> Unit
) : RecyclerView.Adapter<FacultyAdapter.FacultyViewHolder>() {

    inner class FacultyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cardRoot: View = view.findViewById(R.id.cardRoot)
        val tvInitial: TextView = view.findViewById(R.id.tvInitial)
        val tvName: TextView = view.findViewById(R.id.tvName)
        val tvEmployeeId: TextView = view.findViewById(R.id.tvEmployeeId)
        val tvCourse: TextView = view.findViewById(R.id.tvCourse)
        val tvRole: TextView = view.findViewById(R.id.tvRole)
        val tvDesignation: TextView = view.findViewById(R.id.tvDesignation)
        val btnEdit: FrameLayout = view.findViewById(R.id.btnEdit)
        val btnDelete: FrameLayout = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FacultyViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_faculty, parent, false)
        return FacultyViewHolder(view)
    }

    override fun onBindViewHolder(holder: FacultyViewHolder, position: Int) {
        val faculty = faculties[position]

        // Initial (first letter of name)
        val initial = faculty.fullName.firstOrNull()?.uppercase() ?: "?"
        holder.tvInitial.text = initial

        // Name
        holder.tvName.text = faculty.fullName

        // Employee ID
        holder.tvEmployeeId.text = faculty.employeeId

        // Course Badge
        holder.tvCourse.text = faculty.courseCode.ifEmpty { "N/A" }

        // Role Badge (HOD)
        if (faculty.role == "HOD") {
            holder.tvRole.visibility = View.VISIBLE
            holder.tvRole.text = "HOD"
        } else {
            holder.tvRole.visibility = View.GONE
        }

        // Designation Badge
        holder.tvDesignation.text = faculty.designation.ifEmpty { "N/A" }

        // Click Listeners
        holder.cardRoot.setOnClickListener {
            onItemClick(faculty)
        }

        holder.btnEdit.setOnClickListener {
            onEditClick(faculty)
        }

        holder.btnDelete.setOnClickListener {
            onDeleteClick(faculty)
        }
    }

    override fun getItemCount(): Int = faculties.size

    fun updateList(newList: List<Faculty>) {
        faculties = newList
        notifyDataSetChanged()
    }
}
