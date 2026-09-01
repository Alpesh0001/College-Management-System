package com.example.collegemanagementsystemfaculty.adapters

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.collegemanagementsystemfaculty.R
import com.example.collegemanagementsystemfaculty.models.Division

class DivisionAdapter(
    private var divisions: MutableList<Division>,
    private val onViewClick: (Division) -> Unit,
    private val onCreateClick: (Division) -> Unit,
    private val onDeleteClick: (Division) -> Unit
) : RecyclerView.Adapter<DivisionAdapter.DivisionViewHolder>() {

    private val allDivisions = mutableListOf<Division>()

    init {
        allDivisions.addAll(divisions)
    }

    class DivisionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDivisionName: TextView = view.findViewById(R.id.tvDivisionName)
        val tvCourseInfo: TextView = view.findViewById(R.id.tvCourseInfo)
        val tvClassTeacher: TextView = view.findViewById(R.id.tvClassTeacher)
        val tvTimetableStatus: TextView = view.findViewById(R.id.tvTimetableStatus)
        val btnView: Button = view.findViewById(R.id.btnViewTimetable)
        val btnCreate: Button = view.findViewById(R.id.btnCreateTimetable)
        val btnDelete: Button = view.findViewById(R.id.btnDeleteTimetable)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DivisionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_division_timetable, parent, false)
        return DivisionViewHolder(view)
    }

    override fun onBindViewHolder(holder: DivisionViewHolder, position: Int) {
        val division = divisions[position]

        holder.tvDivisionName.text = division.divisionName
        holder.tvCourseInfo.text = "${division.courseName} — Year ${division.year} — Sem ${division.semester}"
        holder.tvClassTeacher.text = "Class Teacher: ${division.classTeacherName ?: "Not Assigned"}"

        // ✅ Status badge
        if (division.hasTimetable) {
            holder.tvTimetableStatus.text = "✅ Created"
            holder.tvTimetableStatus.setBackgroundResource(R.drawable.badge_green)
            holder.btnCreate.text = "✏️ Edit"
            holder.btnView.visibility = View.VISIBLE
        } else {
            holder.tvTimetableStatus.text = "❌ Not Created"
            holder.tvTimetableStatus.setBackgroundResource(R.drawable.badge_red)
            holder.btnCreate.text = "➕ Create"
            holder.btnView.visibility = View.GONE
        }

        // ✅ Click Listeners
        holder.btnView.setOnClickListener { onViewClick(division) }
        holder.btnCreate.setOnClickListener { onCreateClick(division) }
        holder.btnDelete.setOnClickListener { onDeleteClick(division) }
    }

    override fun getItemCount() = divisions.size

    // ✅ Search Filter
    // ✅ Search Filter
    fun filter(query: String) {
        divisions.clear()
        if (query.isEmpty()) {
            divisions.addAll(allDivisions)
        } else {
            divisions.addAll(
                allDivisions.filter {
                    it.divisionName.contains(query, true) ||  // SE-A
                            it.courseName.contains(query, true)   ||  // Computer
                            it.courseCode.contains(query, true)   ||  // CE
                            it.year.contains(query, true)         ||  // 2, 3
                            it.semester.contains(query, true)         // 3, 4
                }
            )
        }
        notifyDataSetChanged()
    }


    fun updateList(newList: List<Division>) {
        divisions.clear()
        allDivisions.clear()
        divisions.addAll(newList)
        allDivisions.addAll(newList)
        notifyDataSetChanged()
    }
}
