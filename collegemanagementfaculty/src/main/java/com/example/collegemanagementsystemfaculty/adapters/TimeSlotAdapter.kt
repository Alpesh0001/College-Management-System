package com.example.collegemanagementsystemfaculty.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.collegemanagementsystemfaculty.R
import com.example.collegemanagementsystemfaculty.models.TimeSlot

class TimeSlotAdapter(
    private var slots: MutableList<TimeSlot>,
    private val isEditable: Boolean,
    private val onEditClick: (TimeSlot, Int) -> Unit,   // ✅ Edit
    private val onDeleteClick: (TimeSlot, Int) -> Unit  // ✅ Delete
) : RecyclerView.Adapter<TimeSlotAdapter.TimeSlotViewHolder>() {

    class TimeSlotViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTimeFrom: TextView     = view.findViewById(R.id.tvTimeFrom)
        val tvTimeTo: TextView       = view.findViewById(R.id.tvTimeTo)
        val tvSubjectName: TextView  = view.findViewById(R.id.tvSubjectName)
        val tvFacultyName: TextView  = view.findViewById(R.id.tvFacultyName)
        val tvRoomNo: TextView       = view.findViewById(R.id.tvRoomNo)
        val btnEditSlot: ImageButton = view.findViewById(R.id.btnEditSlot)
        val btnDeleteSlot: ImageButton = view.findViewById(R.id.btnDeleteSlot)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TimeSlotViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_time_slot, parent, false)
        return TimeSlotViewHolder(view)
    }

    override fun onBindViewHolder(holder: TimeSlotViewHolder, position: Int) {
        val slot = slots[position]

        // ✅ Set time
        holder.tvTimeFrom.text = slot.timeFrom
        holder.tvTimeTo.text   = slot.timeTo

        // ✅ Set subject + faculty
        holder.tvSubjectName.text  = slot.subjectName
        holder.tvFacultyName.text  = "👨‍🏫 ${slot.facultyName}"
        holder.tvRoomNo.text       = if (slot.roomNo.isNotEmpty()) "📍 ${slot.roomNo}" else ""
        holder.tvRoomNo.visibility = if (slot.roomNo.isNotEmpty()) View.VISIBLE else View.GONE

        // ✅ Show edit + delete only for HOD
        holder.btnEditSlot.visibility   = if (isEditable) View.VISIBLE else View.GONE
        holder.btnDeleteSlot.visibility = if (isEditable) View.VISIBLE else View.GONE

        // ✅ Edit click
        holder.btnEditSlot.setOnClickListener {
            onEditClick(slot, position)
        }

        // ✅ Delete click
        holder.btnDeleteSlot.setOnClickListener {
            onDeleteClick(slot, position)
        }
    }

    override fun getItemCount() = slots.size

    // ✅ Replace full list
    fun updateSlots(newSlots: List<TimeSlot>) {
        slots.clear()
        slots.addAll(newSlots)
        notifyDataSetChanged()
    }

    // ✅ Add new slot + auto sort
    fun addSlot(slot: TimeSlot) {
        slots.add(slot)
        slots.sortBy { it.timeFrom }
        notifyDataSetChanged()
    }

    // ✅ Update edited slot
    fun updateSlot(updatedSlot: TimeSlot, position: Int) {
        if (position >= 0 && position < slots.size) {
            slots[position] = updatedSlot
            notifyItemChanged(position)
        }
    }

    // ✅ Remove slot
    fun removeSlot(position: Int) {
        if (position >= 0 && position < slots.size) {
            slots.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, slots.size)
        }
    }

    // ✅ Get all slots
    fun getAllSlots(): List<TimeSlot> = slots.toList()
}
