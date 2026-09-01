package com.example.collegemanagementsystemfaculty.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.example.collegemanagementsystemfaculty.R
import com.example.collegemanagementsystemfaculty.models.AssignmentModel
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.*

class AssignmentAdapter(
    private var assignments: MutableList<AssignmentModel>,
    private val isEditable: Boolean = true,
    private val onCardClick: (AssignmentModel) -> Unit,
    private val onEditClick: (AssignmentModel) -> Unit,
    private val onDeleteClick: (AssignmentModel, Int) -> Unit
) : RecyclerView.Adapter<AssignmentAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        val tvSubjectInfo: TextView = view.findViewById(R.id.tvSubjectInfo)
        val tvDescription: TextView = view.findViewById(R.id.tvDescription)
        val tvDueDate: TextView = view.findViewById(R.id.tvDueDate)
        val tvYearBadge: TextView = view.findViewById(R.id.tvYearBadge)
        val imgAssignmentType: ImageView = view.findViewById(R.id.imgAssignmentType)
        val cardAssignment: View = view.findViewById(R.id.cardAssignment)
        val layoutActions: LinearLayout = view.findViewById(R.id.layoutActions)
        val btnEdit: MaterialButton = view.findViewById(R.id.btnEdit)
        val btnDelete: MaterialButton = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_assignment_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val a = assignments[position]

        holder.tvTitle.text = a.title
        holder.tvSubjectInfo.text = "${a.subject} • ${a.semester}"
        holder.tvDescription.text =
            if (a.description.isEmpty()) "No description" else a.description

        holder.tvYearBadge.text = a.year

        // ✅ Due Date calculation
        if (a.dueDate > 0) {
            val sdf = SimpleDateFormat("dd MMM", Locale.getDefault())
            holder.tvDueDate.text = "Due: ${sdf.format(Date(a.dueDate))}"

            val expired = System.currentTimeMillis() > a.dueDate

            holder.tvDueDate.setBackgroundResource(
                if (expired) R.drawable.badge_red_light
                else R.drawable.badge_green_light
            )
        } else {
            holder.tvDueDate.text = "No due date"
        }

        holder.imgAssignmentType.setImageResource(R.drawable.ic_assignment)

        // Edit/Delete buttons
        if (isEditable) {
            holder.layoutActions.visibility = View.VISIBLE
            holder.btnEdit.setOnClickListener { onEditClick(a) }
            holder.btnDelete.setOnClickListener { onDeleteClick(a, holder.adapterPosition) }
        } else {
            holder.layoutActions.visibility = View.GONE
        }

        holder.cardAssignment.setOnClickListener { onCardClick(a) }
    }

    override fun getItemCount() = assignments.size

    fun updateList(newList: List<AssignmentModel>) {
        assignments.clear()
        assignments.addAll(newList)
        notifyDataSetChanged()
    }

    fun removeItem(position: Int) {
        if (position in 0 until assignments.size) {
            assignments.removeAt(position)
            notifyItemRemoved(position)
        }
    }
}