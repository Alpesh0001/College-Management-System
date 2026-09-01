package com.example.collegemanagementsystemfaculty.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.collegemanagementsystemfaculty.R
import com.example.collegemanagementsystemfaculty.models.MyLecture

class MyLectureAdapter(
    private var lectures: MutableList<MyLecture>
) : RecyclerView.Adapter<MyLectureAdapter.MyLectureViewHolder>() {

    class MyLectureViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTimeFrom    : TextView = view.findViewById(R.id.tvTimeFrom)
        val tvTimeTo      : TextView = view.findViewById(R.id.tvTimeTo)
        val tvSubjectName : TextView = view.findViewById(R.id.tvSubjectName)
        val tvSubjectCode : TextView = view.findViewById(R.id.tvSubjectCode)
        val tvDivisionName: TextView = view.findViewById(R.id.tvDivisionName)
        val tvRoomNo      : TextView = view.findViewById(R.id.tvRoomNo)
        val tvSlotType    : TextView = view.findViewById(R.id.tvSlotType)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MyLectureViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_my_lecture, parent, false)
        return MyLectureViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: MyLectureViewHolder,
        position: Int
    ) {
        val lecture = lectures[position]

        // ✅ Time
        holder.tvTimeFrom.text = lecture.timeFrom
        holder.tvTimeTo.text   = lecture.timeTo

        // ✅ Subject
        holder.tvSubjectName.text = lecture.subjectName
        holder.tvSubjectCode.text = lecture.subjectCode

        // ✅ Division
        holder.tvDivisionName.text = "🏫 ${lecture.divisionName}"

        // ✅ Room
        if (lecture.roomNo.isNotEmpty()) {
            holder.tvRoomNo.text       = "📍 ${lecture.roomNo}"
            holder.tvRoomNo.visibility = View.VISIBLE
        } else {
            holder.tvRoomNo.visibility = View.GONE
        }

        // ✅ Slot Type Badge
        holder.tvSlotType.text = when (lecture.slotType.lowercase()) {
            "lab"      -> "Lab 🔬"
            "tutorial" -> "Tutorial 📝"
            else       -> "Lecture 📚"
        }
    }

    override fun getItemCount() = lectures.size

    // ✅ Update full list
    fun updateLectures(newList: List<MyLecture>) {
        lectures.clear()
        lectures.addAll(newList)
        notifyDataSetChanged()
    }

    // ✅ Get count
    fun getLectureCount() = lectures.size
}
