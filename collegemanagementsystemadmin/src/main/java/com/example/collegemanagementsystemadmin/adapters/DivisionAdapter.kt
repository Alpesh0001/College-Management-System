package com.example.collegemanagementsystemadmin.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.collegemanagementsystemadmin.R
import com.example.collegemanagementsystemadmin.models.Division
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

class DivisionAdapter(
    private var divisions: List<Division>,
    private val onItemClick: (Division) -> Unit,
    private val onEditClick: (Division) -> Unit,
    private val onDeleteClick: (Division) -> Unit
) : RecyclerView.Adapter<DivisionAdapter.DivisionViewHolder>() {

    inner class DivisionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cardRoot: View = view.findViewById(R.id.cardRoot)
        val tvDivisionLetter: TextView = view.findViewById(R.id.tvDivisionLetter)
        val tvDivisionName: TextView = view.findViewById(R.id.tvDivisionName)
        val tvYearSem: TextView = view.findViewById(R.id.tvYearSem)
        val tvStrength: TextView = view.findViewById(R.id.tvStrength)
        val tvClassTeacher: TextView = view.findViewById(R.id.tvClassTeacher)
        val chipGroupRollRanges: ChipGroup = view.findViewById(R.id.chipGroupRollRanges)
        val btnEdit: FrameLayout = view.findViewById(R.id.btnEdit)
        val btnDelete: FrameLayout = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DivisionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_division, parent, false)
        return DivisionViewHolder(view)
    }

    override fun onBindViewHolder(holder: DivisionViewHolder, position: Int) {
        val division = divisions[position]

        // Division Letter
        holder.tvDivisionLetter.text = division.divisionName

        // Division Name with Course
        holder.tvDivisionName.text = "Division ${division.divisionName} - ${division.courseCode}"

        // Year & Semester
        holder.tvYearSem.text = "Year ${division.year} | Semester ${division.semester}"

        // Strength
        holder.tvStrength.text = "👥 ${division.currentStrength}/${division.capacity}"

        // Class Teacher
        if (!division.classTeacherName.isNullOrEmpty()) {
            holder.tvClassTeacher.visibility = View.VISIBLE
            holder.tvClassTeacher.text = "👨‍🏫 ${division.classTeacherName}"
        } else {
            holder.tvClassTeacher.visibility = View.GONE
        }

        // Roll Number Ranges as Chips
        holder.chipGroupRollRanges.removeAllViews()
        division.rollNumberRanges.forEach { range ->
            val chip = Chip(holder.itemView.context)
            chip.text = range.toDisplayString()
            chip.isClickable = false
            chip.isCheckable = false
            chip.setChipBackgroundColorResource(R.color.colorPrimary)
            chip.setTextColor(holder.itemView.context.getColor(android.R.color.white))
            chip.textSize = 11f
            holder.chipGroupRollRanges.addView(chip)
        }

        // Click Listeners
        holder.cardRoot.setOnClickListener {
            onItemClick(division)
        }

        holder.btnEdit.setOnClickListener {
            onEditClick(division)
        }

        holder.btnDelete.setOnClickListener {
            onDeleteClick(division)
        }
    }

    override fun getItemCount(): Int = divisions.size

    fun updateList(newList: List<Division>) {
        divisions = newList
        notifyDataSetChanged()
    }
}
