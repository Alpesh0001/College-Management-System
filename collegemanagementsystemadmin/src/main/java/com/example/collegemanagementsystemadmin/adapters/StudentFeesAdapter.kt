package com.example.collegemanagementsystemadmin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class StudentFeesAdapter(
    private val students: MutableList<StudentFeeItem>,
    private val onStudentClick: (StudentFeeItem) -> Unit
) : RecyclerView.Adapter<StudentFeesAdapter.VH>() {

    // ── Data Model ───────────────────────────────
    data class StudentFeeItem(
        val studentId:   String,
        val name:        String,
        val rollNo:      String,
        val totalAmount: Long,
        val paidAmount:  Long,
        val status:      String  // "paid" | "partial" | "pending"
    )

    // ── ViewHolder ───────────────────────────────
    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvAvatar:      TextView    = view.findViewById(R.id.tvAvatar)
        val tvName:        TextView    = view.findViewById(R.id.tvStudentName)
        val tvRoll:        TextView    = view.findViewById(R.id.tvRollNo)
        val tvAmount:      TextView    = view.findViewById(R.id.tvAmount)
        val tvBadge:       TextView    = view.findViewById(R.id.tvStatusBadge)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
        LayoutInflater.from(parent.context)
            .inflate(R.layout.item_student_fees, parent, false)
    )

    override fun getItemCount() = students.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = students[position]
        val ctx  = holder.itemView.context

        // ── Avatar ───────────────────────────────
        holder.tvAvatar.text = item.name
            .trim()
            .split(" ")
            .take(2)
            .joinToString("") { it.first().uppercase() }

        // ── Name + Roll ──────────────────────────
        holder.tvName.text = item.name
        holder.tvRoll.text = "Roll: ${item.rollNo}"

        // ── Amount ───────────────────────────────
        holder.tvAmount.text =
            "₹${String.format("%,d", item.paidAmount)} / ₹${String.format("%,d", item.totalAmount)}"

        // ── Status Badge ─────────────────────────
        val (badgeText, badgeDrawable) = when (item.status.lowercase()) {
            "paid"    -> Pair("PAID",    R.drawable.bg_badge_green)
            "partial" -> Pair("PARTIAL", R.drawable.bg_badge_orange)
            else      -> Pair("PENDING", R.drawable.bg_badge_red)
        }
        holder.tvBadge.text = badgeText
        holder.tvBadge.setBackgroundResource(badgeDrawable)

        // ── Click ────────────────────────────────
        holder.itemView.setOnClickListener { onStudentClick(item) }
    }

    fun updateList(newList: List<StudentFeeItem>) {
        students.clear()
        students.addAll(newList)
        notifyDataSetChanged()
    }
}
