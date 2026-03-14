package com.njm.worker.ui.dashboard

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.njm.worker.R
import com.njm.worker.data.model.WashRecord

/**
 * WashRecordAdapter - RecyclerView adapter for wash records
 * Navy/Gold card design
 * Developer: meshari.tech
 */
class WashRecordAdapter(
    private val records: List<WashRecord>,
    private val onItemClick: (WashRecord) -> Unit
) : RecyclerView.Adapter<WashRecordAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvPlate: TextView = itemView.findViewById(R.id.tv_plate)
        val tvService: TextView = itemView.findViewById(R.id.tv_service)
        val tvPrice: TextView = itemView.findViewById(R.id.tv_price)
        val tvTime: TextView = itemView.findViewById(R.id.tv_time)
        val tvCarModel: TextView = itemView.findViewById(R.id.tv_car_model)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_ID.toInt()) {
                    onItemClick(records[position])
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_wash_record, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val record = records[position]
        holder.tvPlate.text = record.plate_number ?: "-"
        holder.tvService.text = record.service_type ?: "-"
        holder.tvPrice.text = if (record.price != null) {
            "${record.price} ر.س"
        } else {
            "-"
        }
        holder.tvTime.text = record.created_at?.let { formatTime(it) } ?: "-"
        holder.tvCarModel.text = record.car_model ?: ""
    }

    override fun getItemCount(): Int = records.size

    private fun formatTime(dateTime: String): String {
        return try {
            val parts = dateTime.split("T", " ")
            if (parts.size >= 2) {
                val timeParts = parts[1].split(":")
                if (timeParts.size >= 2) {
                    "${timeParts[0]}:${timeParts[1]}"
                } else parts[1]
            } else dateTime
        } catch (e: Exception) {
            dateTime
        }
    }
}
