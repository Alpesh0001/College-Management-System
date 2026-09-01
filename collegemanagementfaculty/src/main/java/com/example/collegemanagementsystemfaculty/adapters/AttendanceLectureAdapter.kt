package com.example.collegemanagementsystemfaculty.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.collegemanagementsystemfaculty.R
import com.example.collegemanagementsystemfaculty.models.MyLecture

class AttendanceLectureAdapter(
    private var lectures    : MutableList<MyLecture>,
    private var markedSlots : MutableMap<String, Boolean>,
    private val onCardClick : (MyLecture) -> Unit
) : RecyclerView.Adapter<AttendanceLectureAdapter.ViewHolder>() {

    private var currentDateStr = ""

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cardLecture    = view.findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardLecture)
        val viewStatusBar  = view.findViewById<View>(R.id.viewStatusBar)
        val tvSubjectName  = view.findViewById<TextView>(R.id.tvSubjectName)
        val tvDivisionInfo = view.findViewById<TextView>(R.id.tvDivisionInfo)
        val tvTimeSlot     = view.findViewById<TextView>(R.id.tvTimeSlot)
        val tvMarkedBadge  = view.findViewById<TextView>(R.id.tvMarkedBadge)
        val tvPendingBadge = view.findViewById<TextView>(R.id.tvPendingBadge)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_attendance_lecture, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val lecture  = lectures[position]
        val key      = "${lecture.slotId}_${currentDateStr}"
        val isMarked = markedSlots[key] == true

        // ✅ Log each card binding
        android.util.Log.d("ADAPTER", "bind pos=$position key=$key isMarked=$isMarked")
        android.util.Log.d("ADAPTER", "markedSlots=$markedSlots")

        holder.tvSubjectName.text = lecture.subjectName

        holder.tvDivisionInfo.text = "${lecture.divisionName} • ${
            when (lecture.slotType.lowercase()) {
                "lab"      -> "Lab 🔬"
                "tutorial" -> "Tutorial 📝"
                else       -> "Lecture 📚"
            }
        }"

        holder.tvTimeSlot.text = "${lecture.timeFrom} – ${lecture.timeTo}"

        holder.viewStatusBar.setBackgroundColor(
            if (isMarked)
                android.graphics.Color.parseColor("#16A34A")
            else
                android.graphics.Color.parseColor("#DC2626")
        )

        if (isMarked) {
            holder.tvMarkedBadge.visibility  = View.VISIBLE
            holder.tvPendingBadge.visibility = View.GONE
        } else {
            holder.tvMarkedBadge.visibility  = View.GONE
            holder.tvPendingBadge.visibility = View.VISIBLE
        }

        holder.cardLecture.setOnClickListener {
            onCardClick(lecture)
        }
    }

    override fun getItemCount() = lectures.size

    // ✅ FIX: Create NEW map instead of reusing same reference
    fun updateLectures(
        newLectures : List<MyLecture>,
        newMarked   : MutableMap<String, Boolean>,
        dateStr     : String
    ) {
        android.util.Log.d("ADAPTER", "=== updateLectures ===")
        android.util.Log.d("ADAPTER", "newLectures count = ${newLectures.size}")
        android.util.Log.d("ADAPTER", "newMarked = $newMarked")
        android.util.Log.d("ADAPTER", "dateStr = $dateStr")

        lectures.clear()
        lectures.addAll(newLectures)

        // ✅ FIX: Create a brand new map (don't reuse reference!)
        markedSlots = newMarked.toMutableMap()
        currentDateStr = dateStr

        android.util.Log.d("ADAPTER", "markedSlots after update = $markedSlots")

        notifyDataSetChanged()
    }

    fun refreshMarkedStatus(
        newMarked : MutableMap<String, Boolean>,
        dateStr   : String
    ) {
        markedSlots = newMarked.toMutableMap()
        currentDateStr = dateStr
        notifyDataSetChanged()
    }
}
