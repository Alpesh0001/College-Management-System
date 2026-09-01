package com.example.collagemanagmentsystem.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.collagemanagmentsystem.R
import com.example.collagemanagmentsystem.models.AssignmentModel
import com.google.android.material.card.MaterialCardView
import java.text.SimpleDateFormat
import java.util.*

class StudentAssignmentAdapter(
    private var assignments: MutableList<AssignmentModel>,
    private val onCardClick: (AssignmentModel) -> Unit
) : RecyclerView.Adapter<StudentAssignmentAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cardAssignment: MaterialCardView = view.findViewById(R.id.cardAssignment)
        val tvTitle: TextView                = view.findViewById(R.id.tvTitle)
        val tvSubjectInfo: TextView          = view.findViewById(R.id.tvSubjectInfo)
        val tvDescription: TextView          = view.findViewById(R.id.tvDescription)
        val tvDueDate: TextView              = view.findViewById(R.id.tvDueDate)
        val tvYearBadge: TextView            = view.findViewById(R.id.tvYearBadge)
        val tvPdfChip: TextView              = view.findViewById(R.id.tvPdfChip)
        val imgAssignmentType: ImageView     = view.findViewById(R.id.imgAssignmentType)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_student_assignment_card, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount() = assignments.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val a   = assignments[position]
        val ctx = holder.itemView.context
        val now = System.currentTimeMillis()

        // ── Title ────────────────────────────────
        holder.tvTitle.text = a.title

        // ── Subject + Sem ────────────────────────
        holder.tvSubjectInfo.text = "${a.subject} • Sem ${a.semester}"

        // ── Description ──────────────────────────
        holder.tvDescription.text =
            if (a.description.isEmpty()) "No description" else a.description

        // ── Year Badge ───────────────────────────
        holder.tvYearBadge.text = "Year ${a.year}"

        // ── Due Date Badge ───────────────────────
        if (a.dueDate > 0L) {
            val sdf    = SimpleDateFormat("dd MMM", Locale.getDefault())
            val isOver = now > a.dueDate

            holder.tvDueDate.text = if (isOver)
                "⏰ Overdue: ${sdf.format(Date(a.dueDate))}"
            else
                "Due: ${sdf.format(Date(a.dueDate))}"

            holder.tvDueDate.setBackgroundResource(
                if (isOver) R.drawable.bg_badge_red
                else        R.drawable.bg_badge_green
            )
        } else {
            holder.tvDueDate.text = "No due date"
            holder.tvDueDate.setBackgroundResource(R.drawable.bg_badge_orange)
        }

        // ── PDF Chip ─────────────────────────────
        holder.tvPdfChip.visibility =
            if (a.fileUrl.isNotEmpty()) View.VISIBLE else View.GONE

        // ── Card Click ───────────────────────────
        holder.cardAssignment.setOnClickListener {
            onCardClick(a)
        }
    }

    // ── Update list ──────────────────────────────
    fun updateList(newList: List<AssignmentModel>) {
        assignments.clear()
        assignments.addAll(newList)
        notifyDataSetChanged()
    }
}
