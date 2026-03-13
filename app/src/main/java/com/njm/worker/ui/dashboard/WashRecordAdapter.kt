package com.njm.worker.ui.dashboard
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.njm.worker.R
import com.njm.worker.data.model.WashRecord

class WashRecordAdapter(
    private val washes: List<WashRecord>,
    private val onPrintClick: (WashRecord) -> Unit
) : RecyclerView.Adapter<WashRecordAdapter.ViewHolder>() {
    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvPlateNumber: TextView = view.findViewById(R.id.tvPlateNumber)
        val tvCarType: TextView = view.findViewById(R.id.tvCarType)
        val tvWashTime: TextView = view.findViewById(R.id.tvWashTime)
        val tvCost: TextView = view.findViewById(R.id.tvCost)
        val tvPaidStatus: TextView = view.findViewById(R.id.tvPaidStatus)
        val btnPrintReceipt: Button = view.findViewById(R.id.btnPrintReceipt)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_wash_record, parent, false)
        return ViewHolder(view)
    }
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val w = washes[position]
        holder.tvPlateNumber.text = w.plateNumber ?: ""
        holder.tvCarType.text = w.carType ?: ""
        holder.tvWashTime.text = w.washTime ?: ""
        holder.tvCost.text = String.format("%.0f SAR", w.cost ?: 0.0)
        holder.tvPaidStatus.text = if ((w.isPaid ?: 1) == 1) "Paid" else "Unpaid"
        holder.btnPrintReceipt.setOnClickListener { onPrintClick(w) }
    }
    override fun getItemCount() = washes.size
}