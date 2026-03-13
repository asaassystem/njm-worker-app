package com.njm.worker.ui.dashboard

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.njm.worker.R
import com.njm.worker.data.model.WashRecord

class WashRecordAdapter(
    private val washes: List<WashRecord>,
    private val onPrint: ((WashRecord) -> Unit)? = null,
    private val onTogglePaid: ((WashRecord) -> Unit)? = null
) : RecyclerView.Adapter<WashRecordAdapter.VH>() {

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val card: CardView = v.findViewById(R.id.cardWash)
        val tvPlate: TextView = v.findViewById(R.id.tvWashPlate)
        val tvType: TextView = v.findViewById(R.id.tvWashType)
        val tvOrg: TextView = v.findViewById(R.id.tvWashOrg)
        val tvCost: TextView = v.findViewById(R.id.tvWashCost)
        val tvTime: TextView = v.findViewById(R.id.tvWashTime)
        val tvPayStatus: TextView = v.findViewById(R.id.tvPayStatus)
        val btnPrint: ImageButton = v.findViewById(R.id.btnPrintReceipt)
        val btnTogglePaid: TextView = v.findViewById(R.id.btnTogglePaid)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        return VH(LayoutInflater.from(parent.context).inflate(R.layout.item_wash_record, parent, false))
    }

    override fun getItemCount() = washes.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val wash = washes[position]
        holder.tvPlate.text = wash.plateNumber ?: ""
        holder.tvType.text = when ((wash.carType ?: "").lowercase()) {
            "large" -> "كبير"
            else -> "صغير"
        }
        holder.tvOrg.text = wash.orgName ?: ""
        holder.tvCost.text = String.format("%.0f ر.س", wash.cost ?: 0.0)
        holder.tvTime.text = wash.washTime ?: ""
        val isPaid = (wash.isPaid ?: 1) == 1
        holder.tvPayStatus.text = if (isPaid) "مدفوع" else "غير مدفوع"
        holder.tvPayStatus.setTextColor(if (isPaid) Color.parseColor("#27AE60") else Color.parseColor("#E74C3C"))
        holder.btnTogglePaid.text = if (isPaid) "تغيير لغير مدفوع" else "تغيير لمدفوع"
        holder.btnTogglePaid.setBackgroundColor(if (isPaid) Color.parseColor("#E74C3C") else Color.parseColor("#27AE60"))
        holder.btnPrint.setOnClickListener { onPrint?.invoke(wash) }
        holder.btnTogglePaid.setOnClickListener { onTogglePaid?.invoke(wash) }
        // Highlight unpaid
        holder.card.setCardBackgroundColor(
            if (isPaid) Color.WHITE else Color.parseColor("#FFF3CD")
        )
    }
}
