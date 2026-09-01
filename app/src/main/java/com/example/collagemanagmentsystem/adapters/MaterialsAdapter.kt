package com.example.collagemanagmentsystem.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.collagemanagmentsystem.R
import com.example.collagemanagmentsystem.models.MaterialModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import java.text.SimpleDateFormat
import java.util.*

class MaterialsAdapter(
    private var materials: MutableList<MaterialModel>,
    private val onCardClick: (MaterialModel) -> Unit,      // ✅ Card = Open URL
    private val onDownloadClick: (MaterialModel) -> Unit   // ✅ Button = Download
) : RecyclerView.Adapter<MaterialsAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cardMaterial: MaterialCardView = view.findViewById(R.id.cardMaterial)
        val ivFileIcon: ImageView          = view.findViewById(R.id.ivFileIcon)
        val tvFileTypeBadge: TextView      = view.findViewById(R.id.tvFileTypeBadge)
        val tvTitle: TextView              = view.findViewById(R.id.tvTitle)
        val tvDescription: TextView        = view.findViewById(R.id.tvDescription)
        val tvUploadDate: TextView         = view.findViewById(R.id.tvUploadDate)
        val btnDownload: MaterialButton    = view.findViewById(R.id.btnDownload)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_material_card, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount() = materials.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val m   = materials[position]
        val ctx = holder.itemView.context

        holder.tvTitle.text       = m.title
        holder.tvDescription.text = m.description.ifEmpty { "Tap card to open file" }

        if (m.uploadedAt > 0L) {
            val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            holder.tvUploadDate.text = "Uploaded: ${sdf.format(Date(m.uploadedAt))}"
        } else {
            holder.tvUploadDate.text = "Uploaded: Recently"
        }

        val ext = m.fileName
            .substringAfterLast(".")
            .lowercase()
            .ifEmpty { m.fileType.lowercase() }

        when (ext) {
            "pdf" -> {
                holder.tvFileTypeBadge.text = "PDF"
                holder.tvFileTypeBadge.setBackgroundResource(R.drawable.bg_badge_red)
                holder.ivFileIcon.setColorFilter(
                    androidx.core.content.ContextCompat.getColor(ctx, R.color.red)
                )
            }
            "doc", "docx" -> {
                holder.tvFileTypeBadge.text = "DOC"
                holder.tvFileTypeBadge.setBackgroundResource(R.drawable.badge_blue_light)
                holder.ivFileIcon.setColorFilter(
                    androidx.core.content.ContextCompat.getColor(ctx, R.color.deep_blue)
                )
            }
            "jpg", "jpeg", "png" -> {
                holder.tvFileTypeBadge.text = "IMG"
                holder.tvFileTypeBadge.setBackgroundResource(R.drawable.bg_badge_green)
                holder.ivFileIcon.setColorFilter(
                    androidx.core.content.ContextCompat.getColor(ctx, R.color.green)
                )
            }
            else -> {
                holder.tvFileTypeBadge.text = "FILE"
                holder.tvFileTypeBadge.setBackgroundResource(R.drawable.bg_badge_orange)
                holder.ivFileIcon.setColorFilter(
                    androidx.core.content.ContextCompat.getColor(ctx, R.color.orange)
                )
            }
        }

        // ✅ Card Click → Open file URL directly
        holder.cardMaterial.setOnClickListener {
            onCardClick(m)
        }

        // ✅ Download Button → Download to storage
        holder.btnDownload.setOnClickListener {
            onDownloadClick(m)
        }
    }

    fun updateList(newList: List<MaterialModel>) {
        materials.clear()
        materials.addAll(newList)
        notifyDataSetChanged()
    }
}
