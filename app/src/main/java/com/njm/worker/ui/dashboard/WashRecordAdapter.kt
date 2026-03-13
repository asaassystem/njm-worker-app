package com.njm.worker.ui.dashboard

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.njm.worker.R
import com.njm.worker.data.model.WashRecord

class WashRecordAdapter(private val items: List<WashRecord>) :
    RecyclerView.Adapter<WashRecordAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvPlate: TextView = view.findViewById(R.id.tvPlateNumber)
        val tvType: TextView = view.findViewById(R.id.tvCarType)
        val tvTime: TextView = view.findViewById(R.id.tvWashTime)
        val tvCost: TextView = view.findViewById(R.id.tvCost)
        val tvPaid: TextView = view.findViewById(R.id.tvPaidStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_wash_record, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvPlate.text = item.plateNumber ?: "-"
        holder.tvType.text = item.carType ?: "-"
        holder.tvTime.text = item.washDate ?: ""
        holder.tvCost.text = (item.cost ?: 0.0).toString()
        holder.tvPaid.text = if (item.isPaid == true) "Paid" else "Unpaid"
    }

    override fun getItemCount() = items.size
}