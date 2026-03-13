package com.njm.worker.ui.dashboard

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.njm.worker.data.model.WashRecord
import com.njm.worker.databinding.ItemWashRecordBinding

class WashRecordAdapter : ListAdapter<WashRecord, WashRecordAdapter.ViewHolder>(DIFF) {

    class ViewHolder(val binding: ItemWashRecordBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemWashRecordBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        with(holder.binding) {
            tvPlate.text = item.plate_number
            tvOrg.text = item.org_name
            tvType.text = item.car_type_label()
            tvPrice.text = String.format("%.2f ر.س", item.cost)
            tvTime.text = item.wash_time_fmt
        }
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<WashRecord>() {
            override fun areItemsTheSame(a: WashRecord, b: WashRecord) = a.id == b.id
            override fun areContentsTheSame(a: WashRecord, b: WashRecord) = a == b
        }
    }
}

fun WashRecord.car_type_label() = if (car_type == "large") "كبير" else "صغير"