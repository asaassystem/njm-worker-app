package com.njm.worker.ui.dashboard

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
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
        val card: CardView = v.findViewById(R.id.cardWashRecord)
        val tvPlate: TextView = v.findViewById(R.id.tvPlateNumber)
        val tvType: TextView = v.findViewById(R.id.tvCarType)
        val tvOrg: TextView = v.findViewById(R.id.tvOrgName)
        val tvCost: TextView = v.findViewById(R.id.tvCost)
        val tvTime: TextView = v.findViewById(R.id.tvWashTime)
        val tvPayStatus: TextView = v.findViewById(R.id.tvPaidStatus)
        val btnPrint: Button = v.findViewById(R.id.btnPrintReceipt)
        val btnTogglePaid: Button = v.findViewById(R.id.btnTogglePaid)
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
            "small" -> "صغير"
            else -> wash.carType ?: ""
        }
        holder.tvOrg.text = wash.orgName ?: ""
        holder.tvCost.text = String.format("%.0f ر.س", wash.cost ?: 0.0)
        holder.tvTime.text = wash.washTime ?: ""

        val isPaid = (wash.isPaid ?: 1) == 1
        holder.tvPayStatus.text = if (isPaid) "مدفوع" else "غير مدفوع"
        holder.tvPayStatus.setTextColor(if (isPaid) Color.parseColor("#27AE60") else Color.parseColor("#E74C3C"))

        holder.btnTogglePaid.text = if (isPaid) "غير مدفوع" else "دفع"
        holder.btnTogglePaid.backgroundTintList = android.content.res.ColorStateList.valueOf(
            if (isPaid) Color.parseColor("#E74C3C") else Color.parseColor("#27AE60")
        )

        holder.btnPrint.setOnClickListener { onPrint?.invoke(wash) }
        holder.btnTogglePaid.setOnClickListener { onTogglePaid?.invoke(wash) }

        // Color-code card background based on payment status
        holder.card.setCardBackgroundColor(
            if (isPaid) Color.WHITE else Color.parseColor("#FFF8E1")
        )
    }
}
