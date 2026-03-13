package com.njm.worker.ui.dashboard

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.njm.worker.R
import com.njm.worker.data.model.WashRecord

class WashRecordAdapter(
    private val washes: List<WashRecord>
) : RecyclerView.Adapter<WashRecordAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvPlate: TextView = view.findViewById(R.id.tvPlate)
        val tvTime: TextView = view.findViewById(R.id.tvTime)
        val tvAmount: TextView = view.findViewById(R.id.tvAmount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_wash_record, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val w = washes[position]
        holder.tvPlate.text = w.plateNumber ?: ""
        holder.tvTime.text = w.washTime ?: ""
        holder.tvAmount.text = if (w.amount != null) String.format("%.0f", w.amount) else ""
    }

    override fun getItemCount() = washes.size
}