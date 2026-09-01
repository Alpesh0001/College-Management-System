package com.example.collegemanagementsystemadmin.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.collegemanagementsystemadmin.R
import com.example.collegemanagementsystemadmin.models.Student

class StudentAdapter(
    private var students: List<Student>,
    private val onItemClick: (Student) -> Unit,
    private val onEditClick: (Student) -> Unit,
    private val onDeleteClick: (Student) -> Unit
) : RecyclerView.Adapter<StudentAdapter.StudentViewHolder>() {

    inner class StudentViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvInitial: TextView = view.findViewById(R.id.tvInitial)
        val tvName: TextView = view.findViewById(R.id.tvName)
        val tvRollNo: TextView = view.findViewById(R.id.tvRollNo)
        val tvCourse: TextView = view.findViewById(R.id.tvCourse)
        val tvYearSem: TextView = view.findViewById(R.id.tvYearSem)
        val btnEdit: FrameLayout = view.findViewById(R.id.btnEdit)
        val btnDelete: FrameLayout = view.findViewById(R.id.btnDelete)
        val cardRoot: View = view.findViewById(R.id.cardRoot)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StudentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_student, parent, false)
        return StudentViewHolder(view)
    }

    override fun onBindViewHolder(holder: StudentViewHolder, position: Int) {
        val student = students[position]

        holder.tvInitial.text = student.getInitial()
        holder.tvName.text = student.fullName
        holder.tvRollNo.text = "Roll: ${student.rollNo}"
        holder.tvCourse.text = student.courseCode
        holder.tvYearSem.text = student.getYearSemesterLabel()

        holder.cardRoot.setOnClickListener {
            onItemClick(student)
        }

        holder.btnEdit.setOnClickListener {
            onEditClick(student)
        }

        holder.btnDelete.setOnClickListener {
            onDeleteClick(student)
        }
    }

    override fun getItemCount(): Int = students.size

    fun updateList(newList: List<Student>) {
        students = newList
        notifyDataSetChanged()
    }
}
