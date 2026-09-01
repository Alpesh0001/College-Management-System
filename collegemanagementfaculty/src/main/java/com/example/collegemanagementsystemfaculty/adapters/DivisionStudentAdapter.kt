package com.example.collegemanagementsystemfaculty.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.collegemanagementsystemfaculty.R
import com.example.collegemanagementsystemfaculty.models.Division

class DivisionStudentAdapter(
    private val list: List<Division>,
    private val onClick: (Division) -> Unit
) : RecyclerView.Adapter<DivisionStudentAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName  : TextView = view.findViewById(R.id.tvDivisionName)
        val tvInfo  : TextView = view.findViewById(R.id.tvCourseInfo)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_division_student, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]

        holder.tvName.text = item.divisionName
        holder.tvInfo.text =
            "${item.courseName} • Year ${item.year} • Sem ${item.semester}"

        holder.itemView.setOnClickListener { onClick(item) }
    }

    override fun getItemCount(): Int = list.size
}