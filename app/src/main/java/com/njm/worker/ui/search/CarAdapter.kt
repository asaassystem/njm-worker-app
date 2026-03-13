package com.njm.worker.ui.search

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.njm.worker.data.model.Car
import com.njm.worker.databinding.ItemCarBinding

class CarAdapter(private val onRecord: (Car) -> Unit) :
    ListAdapter<Car, CarAdapter.ViewHolder>(DIFF) {

    class ViewHolder(val binding: ItemCarBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCarBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val car = getItem(position)
        with(holder.binding) {
            tvPlate.text = car.plate_number
            tvOrgType.text = car.org_name + " • " + car.car_type_label()
            tvPrice.text = String.format("%.2f ريال", car.price)
            tvColor.text = car.color.takeIf { it.isNotEmpty() }?.let { "اللون: $it" } ?: ""
            btnRecord.setOnClickListener { onRecord(car) }
        }
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<Car>() {
            override fun areItemsTheSame(a: Car, b: Car) = a.id == b.id
            override fun areContentsTheSame(a: Car, b: Car) = a == b
        }
    }
}