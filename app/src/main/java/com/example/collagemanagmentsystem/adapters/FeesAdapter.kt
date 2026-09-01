package com.example.collagemanagmentsystem.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.collagemanagmentsystem.FeesReceiptActivity
import com.example.collagemanagmentsystem.R
import com.google.android.material.card.MaterialCardView
import java.text.SimpleDateFormat
import java.util.*

class FeesAdapter(
    private val semesters: MutableList<FeeSemester>,
    private val onReceiptClick: (FeeSemester) -> Unit
) : RecyclerView.Adapter<FeesAdapter.FeesViewHolder>() {

    // ── Data Model ──────────────────────────────────
    data class FeeSemester(
        val semNumber: Int,          // 1, 2, 3...
        val totalAmount: Long,       // 25000
        val paidAmount: Long,        // 15000
        val status: String,          // "paid" | "partial" | "pending"
        val receiptUrl: String?,     // cloudinary url or null
        val paidDate: Long           // timestamp
    )

    // ── ViewHolder ───────────────────────────────────
    inner class FeesViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val card: MaterialCardView   = itemView.findViewById(R.id.cardSemester)
        val tvSemName: TextView      = itemView.findViewById(R.id.tvSemName)
        val tvPaidDate: TextView     = itemView.findViewById(R.id.tvPaidDate)
        val tvStatusBadge: TextView  = itemView.findViewById(R.id.tvStatusBadge)
        val tvAmount: TextView       = itemView.findViewById(R.id.tvAmount)
        val btnReceipt: View         = itemView.findViewById(R.id.btnReceipt)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeesViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_fee_semester, parent, false)
        return FeesViewHolder(view)
    }

    override fun getItemCount() = semesters.size

    override fun onBindViewHolder(holder: FeesViewHolder, position: Int) {
        val item = semesters[position]
        val ctx = holder.itemView.context

        // ── Semester Name ────────────────────────────
        holder.tvSemName.text = "Semester ${item.semNumber}"

        // ── Paid Date ────────────────────────────────
        if (item.paidDate > 0L) {
            val dateStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                .format(Date(item.paidDate))
            holder.tvPaidDate.text = "Paid on $dateStr"
            holder.tvPaidDate.visibility = View.VISIBLE
        } else {
            holder.tvPaidDate.text = "Not yet paid"
            holder.tvPaidDate.visibility = View.VISIBLE
        }

        // ── Status Badge ─────────────────────────────
        val (badgeText, badgeDrawable, progressColor) = when (item.status.lowercase()) {
            "paid"    -> Triple("PAID",    R.drawable.bg_badge_green,  R.color.green)
            "partial" -> Triple("PARTIAL", R.drawable.bg_badge_orange, R.color.orange)
            else      -> Triple("PENDING", R.drawable.bg_badge_red,    R.color.red)
        }
        holder.tvStatusBadge.text = badgeText
        holder.tvStatusBadge.setBackgroundResource(badgeDrawable)

        // ── Amount ───────────────────────────────────
        holder.tvAmount.text =
            "₹${formatAmount(item.paidAmount)} / ₹${formatAmount(item.totalAmount)}"


        // ── Receipt Button ───────────────────────────
        // ✅ Always show receipt button
        holder.btnReceipt.visibility = View.VISIBLE

        // ✅ Helper: open receipt screen
        fun openReceipt() {
            val intent = android.content.Intent(ctx, FeesReceiptActivity::class.java).apply {
                putExtra("semNumber",   item.semNumber)
                putExtra("totalAmount", item.totalAmount)
                putExtra("paidAmount",  item.paidAmount)
                putExtra("status",      item.status)
                putExtra("paidDate",    item.paidDate)
            }
            ctx.startActivity(intent)
        }

        // ✅ Card click → open receipt
        holder.card.setOnClickListener {
            openReceipt()
        }

        // ✅ Receipt button click → open receipt
        holder.btnReceipt.setOnClickListener {
            openReceipt()
        }
    }


    // ── Helper: format 25000 → "25,000" ─────────────
    private fun formatAmount(amount: Long): String {
        return String.format("%,d", amount)
    }

    // ── Update list ──────────────────────────────────
    fun updateList(newList: List<FeeSemester>) {
        semesters.clear()
        semesters.addAll(newList)
        notifyDataSetChanged()
    }
}
