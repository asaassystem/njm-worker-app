package com.njm.worker.ui.search
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.njm.worker.R
import com.njm.worker.data.model.CarDetail

class CarAdapter(
    private val cars: List<CarDetail>,
    private val onWashClick: (CarDetail) -> Unit
) : RecyclerView.Adapter<CarAdapter.ViewHolder>() {
    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvPlate: TextView = view.findViewById(R.id.tvPlate)
        val tvCarType: TextView = view.findViewById(R.id.tvCarType)
        val tvPrice: TextView = view.findViewById(R.id.tvPrice)
        val btnRecordWash: Button = view.findViewById(R.id.btnRecordWash)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_car, parent, false)
        return ViewHolder(view)
    }
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val car = cars[position]
        holder.tvPlate.text = car.plateNumber
        holder.tvCarType.text = car.carTypeLabel ?: car.carType ?: ""
        holder.tvPrice.text = String.format("%.0f SAR", car.washPrice ?: 0.0)
        holder.btnRecordWash.setOnClickListener { onWashClick(car) }
    }
    override fun getItemCount() = cars.size
}