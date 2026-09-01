package com.example.collegemanagementsystemadmin.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.collegemanagementsystemadmin.models.SubjectUi
import com.example.collegemanagementsystemadmin.R

class SubjectAdapter(
    private val items: MutableList<SubjectUi>,
    private val onView: (SubjectUi) -> Unit,  // ✅ ADD onView callback
    private val onEdit: (SubjectUi) -> Unit,
    private val onDelete: (SubjectUi) -> Unit
) : RecyclerView.Adapter<SubjectAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvSubjectName)
        val tvId: TextView = view.findViewById(R.id.tvSubjectId)
        val tvCourse: TextView = view.findViewById(R.id.tvCourse)
        val tvYear: TextView = view.findViewById(R.id.tvYear)
        val tvSemester: TextView = view.findViewById(R.id.tvSemester)
        val btnEdit: FrameLayout = view.findViewById(R.id.btnEdit)
        val btnDelete: FrameLayout = view.findViewById(R.id.btnDelete)
        val cardRoot: View = view.findViewById(R.id.cardRoot)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_subject, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        holder.tvName.text = item.name.ifEmpty { "Unnamed Subject" }
        holder.tvId.text = "ID: ${item.subjectId}"

        // Show COURSE ID instead of name
        holder.tvCourse.text = if (item.courseId.isNotEmpty()) {
            item.courseId.uppercase()
        } else {
            "NO-ID"
        }

        // Year badge
        holder.tvYear.text = if (item.year.isNotEmpty()) "Y${item.year}" else "Y?"

        // Semester badge
        holder.tvSemester.text = if (item.semester.isNotEmpty()) "S${item.semester}" else "S?"

        // ✅ Edit button - Opens EDIT mode
        holder.btnEdit.setOnClickListener {
            onEdit(item)
        }

        holder.btnDelete.setOnClickListener {
            onDelete(item)
        }

        // ✅ Card click - Opens VIEW mode
        holder.cardRoot.setOnClickListener {
            onView(item)
        }
    }

    override fun getItemCount() = items.size

    fun replaceAll(newItems: List<SubjectUi>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun removeItem(item: SubjectUi) {
        val index = items.indexOf(item)
        if (index != -1) {
            items.removeAt(index)
            notifyItemRemoved(index)
        }
    }
}
