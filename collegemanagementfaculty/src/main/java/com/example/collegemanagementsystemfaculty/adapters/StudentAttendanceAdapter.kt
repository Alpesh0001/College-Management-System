package com.example.collegemanagementsystemfaculty.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.collegemanagementsystemfaculty.R
import com.example.collegemanagementsystemfaculty.models.StudentAttendanceItem
import com.google.android.material.card.MaterialCardView

class StudentAttendanceAdapter(
    private val students: MutableList<StudentAttendanceItem>,
    private val onToggle: () -> Unit
) : RecyclerView.Adapter<StudentAttendanceAdapter.ViewHolder>() {

    class ViewHolder(view: android.view.View) : RecyclerView.ViewHolder(view) {
        val cardStudent: MaterialCardView = view.findViewById(R.id.cardStudent)
        val tvRollNo: TextView           = view.findViewById(R.id.tvRollNo)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_student_attendance, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val student = students[position]

        // show roll number
        holder.tvRollNo.text = student.rollNumber.toString()

        // apply color based on status
        when (student.status) {
            "Present" -> {
                holder.cardStudent.setCardBackgroundColor(Color.parseColor("#16A34A")) // green
                holder.tvRollNo.setTextColor(Color.WHITE)
            }
            "Absent" -> {
                holder.cardStudent.setCardBackgroundColor(Color.parseColor("#DC2626")) // red
                holder.tvRollNo.setTextColor(Color.WHITE)
            }
            else -> {
                holder.cardStudent.setCardBackgroundColor(Color.parseColor("#E5E7EB")) // grey
                holder.tvRollNo.setTextColor(Color.parseColor("#374151"))
            }
        }

        // click → toggle status and refresh colors
        holder.cardStudent.setOnClickListener {
            student.status = when (student.status) {
                "Present" -> "Absent"
                "Absent"  -> "Present"
                else      -> "Present"
            }
            notifyItemChanged(position)
            onToggle()
        }
    }

    override fun getItemCount() = students.size
}
