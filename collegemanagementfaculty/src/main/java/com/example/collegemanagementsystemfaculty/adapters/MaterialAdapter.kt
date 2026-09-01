package com.example.collegemanagementsystemfaculty.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.collegemanagementsystemfaculty.R
import com.example.collegemanagementsystemfaculty.models.MaterialModel
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.*

class MaterialAdapter(
    private var materials: MutableList<MaterialModel>,
    private val isEditable: Boolean,                          // ✅ true = HOD, false = Student/View only
    private val onCardClick: (MaterialModel) -> Unit,         // ✅ View mode
    private val onEditClick: (MaterialModel) -> Unit,         // ✅ Edit mode
    private val onDeleteClick: (MaterialModel, Int) -> Unit   // ✅ Delete with position
) : RecyclerView.Adapter<MaterialAdapter.ViewHolder>() {

    // ─────────────────────────────────────────
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle       : TextView      = view.findViewById(R.id.tvTitle)
        val tvSubjectInfo : TextView      = view.findViewById(R.id.tvSubjectInfo)
        val tvDescription : TextView      = view.findViewById(R.id.tvDescription)
        val tvDate        : TextView      = view.findViewById(R.id.tvDate)
        val tvYearBadge   : TextView      = view.findViewById(R.id.tvYearBadge)
        val imgFileType   : ImageView     = view.findViewById(R.id.imgFileType)
        val cardMaterial  : View          = view.findViewById(R.id.cardMaterial)
        val layoutActions : LinearLayout  = view.findViewById(R.id.layoutActions)
        val btnEdit       : MaterialButton = view.findViewById(R.id.btnEdit)
        val btnDelete     : MaterialButton = view.findViewById(R.id.btnDelete)
    }

    // ─────────────────────────────────────────
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_material_card, parent, false)
        return ViewHolder(view)
    }

    // ─────────────────────────────────────────
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val material = materials[position]

        // ✅ Basic Info
        holder.tvTitle.text       = material.title
        holder.tvSubjectInfo.text = "${material.subject} • ${material.semester}"
        holder.tvDescription.text = material.description.ifEmpty { "No description" }
        holder.tvYearBadge.text   = material.year

        // ✅ Format uploaded date
        if (material.uploadedAt > 0L) {
            val sdf = SimpleDateFormat("dd MMM", Locale.getDefault())
            holder.tvDate.text = sdf.format(Date(material.uploadedAt))
        } else {
            holder.tvDate.text = ""
        }

        // ✅ File type icon
        val iconRes = when (material.fileType.lowercase()) {
            "pdf"         -> R.drawable.ic_book
            "doc", "docx" -> R.drawable.ic_assignment
            else          -> R.drawable.ic_materials
        }
        holder.imgFileType.setImageResource(iconRes)

        // ✅ Show Edit/Delete only for HOD (isEditable = true)
        if (isEditable) {
            holder.layoutActions.visibility = View.VISIBLE

            holder.btnEdit.setOnClickListener {
                onEditClick(material)
            }

            holder.btnDelete.setOnClickListener {
                onDeleteClick(material, holder.adapterPosition)
            }

        } else {
            // Student / View only — hide action buttons
            holder.layoutActions.visibility = View.GONE
        }

        // ✅ Whole card click → View mode
        holder.cardMaterial.setOnClickListener {
            onCardClick(material)
        }
    }

    // ─────────────────────────────────────────
    override fun getItemCount() = materials.size

    // ✅ Update full list (search / reload)
    fun updateList(newList: List<MaterialModel>) {
        materials.clear()
        materials.addAll(newList)
        notifyDataSetChanged()
    }

    // ✅ Remove single item after delete
    fun removeItem(position: Int) {
        if (position in 0 until materials.size) {
            materials.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, materials.size)
        }
    }
}
