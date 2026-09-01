package com.example.collegemanagementsystemfaculty.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.collegemanagementsystemfaculty.R
import com.example.collegemanagementsystemfaculty.models.StudentModel
import de.hdodenhof.circleimageview.CircleImageView

class StudentAdapter(
    private var students: List<StudentModel>,
    private val onStudentClick: (StudentModel) -> Unit
) : RecyclerView.Adapter<StudentAdapter.StudentViewHolder>() {

    inner class StudentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val imgStudent: CircleImageView = itemView.findViewById(R.id.imgStudent)
        val tvName: TextView = itemView.findViewById(R.id.tvName)
        val tvRoll: TextView = itemView.findViewById(R.id.tvRoll)
        val tvCourse: TextView = itemView.findViewById(R.id.tvCourse)

        fun bind(student: StudentModel) {

            tvName.text = student.fullName
            tvRoll.text = "Roll No: ${student.rollNo}"
            tvCourse.text = "${student.courseName} • Year ${student.year} • Sem ${student.semester}"

            // Load photo
            if (student.photoUrl.isNotEmpty()) {
                Glide.with(itemView.context)
                    .load(student.photoUrl)
                    .placeholder(R.drawable.ic_user)
                    .into(imgStudent)
            } else {
                imgStudent.setImageResource(R.drawable.ic_user)
            }

            itemView.setOnClickListener {
                onStudentClick(student)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StudentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_student_card, parent, false)
        return StudentViewHolder(view)
    }

    override fun onBindViewHolder(holder: StudentViewHolder, position: Int) {
        holder.bind(students[position])
    }

    override fun getItemCount(): Int = students.size

    fun updateList(newList: List<StudentModel>) {
        students = newList
        notifyDataSetChanged()
    }
}