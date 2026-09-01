package com.example.collagemanagmentsystem.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.collagemanagmentsystem.R
import com.google.android.material.card.MaterialCardView

class SubjectAdapter(
    private var subjects: MutableList<Pair<String, Int>>, // subject name + material count
    private val onClick: (String) -> Unit
) : RecyclerView.Adapter<SubjectAdapter.ViewHolder>() {

    // ── Color pool for subject icons ──────────
    private val bgColors = listOf(
        "#E3F2FD", "#F3E5F5", "#E8F5E9",
        "#FFF3E0", "#FCE4EC", "#E0F7FA"
    )
    private val textColors = listOf(
        "#1565C0", "#6A1B9A", "#2E7D32",
        "#E65100", "#880E4F", "#00695C"
    )

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cardSubject: MaterialCardView = view.findViewById(R.id.cardSubject)
        val tvSubjectInitial: TextView    = view.findViewById(R.id.tvSubjectInitial)
        val iconBg: View                  = view.findViewById(R.id.iconBg)
        val tvSubjectName: TextView       = view.findViewById(R.id.tvSubjectName)
        val tvSemBadge: TextView          = view.findViewById(R.id.tvSemBadge)
        val tvMaterialCount: TextView     = view.findViewById(R.id.tvMaterialCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_subject_card, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount() = subjects.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val (subjectName, count) = subjects[position]

        // ── Subject Name ─────────────────────
        holder.tvSubjectName.text = subjectName

        // ── Initial Letter ───────────────────
        val initial = subjectName.firstOrNull()?.uppercase() ?: "S"
        holder.tvSubjectInitial.text = initial

        // ── Rotating color per subject ───────
        val colorIndex = position % bgColors.size
        holder.iconBg.backgroundTintList =
            android.content.res.ColorStateList.valueOf(
                android.graphics.Color.parseColor(bgColors[colorIndex])
            )
        holder.tvSubjectInitial.setTextColor(
            android.graphics.Color.parseColor(textColors[colorIndex])
        )

        // ── Material count badge ──────────────
        holder.tvMaterialCount.text =
            if (count > 0) "$count file${if (count == 1) "" else "s"}"
            else "No files"

        // ── Click ─────────────────────────────
        holder.cardSubject.setOnClickListener {
            onClick(subjectName)
        }
    }

    fun updateList(newList: List<Pair<String, Int>>) {
        subjects.clear()
        subjects.addAll(newList)
        notifyDataSetChanged()
    }
}
