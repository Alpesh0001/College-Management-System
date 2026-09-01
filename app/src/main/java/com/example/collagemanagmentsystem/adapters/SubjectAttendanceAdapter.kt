package com.example.collagemanagmentsystem.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.collagemanagmentsystem.R
import com.example.collagemanagmentsystem.models.StudentSubjectAttendance
import com.google.android.material.progressindicator.LinearProgressIndicator

class SubjectAttendanceAdapter(
    private var subjects: MutableList<StudentSubjectAttendance>
) : RecyclerView.Adapter<SubjectAttendanceAdapter.SubjectViewHolder>() {

    class SubjectViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvSubjectName: TextView = view.findViewById(R.id.tvSubjectName)
        val tvPercentage: TextView = view.findViewById(R.id.tvPercentage)
        val tvLectureCount: TextView = view.findViewById(R.id.tvLectureCount)
        val subjectProgress: LinearProgressIndicator =
            view.findViewById(R.id.subjectProgress)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubjectViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_subject_attendance, parent, false)
        return SubjectViewHolder(view)
    }

    override fun onBindViewHolder(holder: SubjectViewHolder, position: Int) {
        val item = subjects[position]
        val context = holder.itemView.context

        holder.tvSubjectName.text = item.subjectName
        holder.tvLectureCount.text = "${item.present} / ${item.total} lectures"
        holder.subjectProgress.max = 100
        holder.subjectProgress.progress = item.percentage

        holder.tvPercentage.text = "${item.percentage}%"

        val colorRes = when {
            item.percentage >= 75 -> R.color.green
            item.percentage >= 60 -> R.color.orange
            else -> R.color.red
        }
        val color = ContextCompat.getColor(context, colorRes)
        holder.tvPercentage.setTextColor(color)
        holder.subjectProgress.setIndicatorColor(color)
    }

    override fun getItemCount(): Int = subjects.size

    fun updateData(newList: List<StudentSubjectAttendance>) {
        subjects.clear()
        subjects.addAll(newList)
        notifyDataSetChanged()
    }
}
