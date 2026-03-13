package com.njm.worker.ui.search

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.njm.worker.R
import com.njm.worker.data.model.Car
import com.njm.worker.data.repository.WorkerRepository
import com.njm.worker.printer.PrinterManager
import com.njm.worker.utils.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class CarAdapter(
    private val items: List<Car>,
    private val repo: WorkerRepository,
    private val scope: CoroutineScope
) : RecyclerView.Adapter<CarAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvPlate: TextView = view.findViewById(R.id.tvPlate)
        val tvType: TextView = view.findViewById(R.id.tvCarType)
        val tvPrice: TextView = view.findViewById(R.id.tvPrice)
        val btnRecord: Button = view.findViewById(R.id.btnRecordWash)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_car, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val car = items[position]
        holder.tvPlate.text = car.plateNumber
        holder.tvType.text = car.carType
        holder.tvPrice.text = (car.price ?: 0.0).toString() + " SAR"
        holder.btnRecord.setOnClickListener {
            scope.launch {
                val result = repo.recordWash(car.id)
                result.onSuccess {
                    Toast.makeText(holder.itemView.context, "Wash recorded!", Toast.LENGTH_SHORT).show()
                    // Print receipt
                    try {
                        PrinterManager.printWashReceipt(
                            SessionManager.getWorkerName(),
                            car.plateNumber,
                            car.carType,
                            car.price ?: 0.0,
                            SessionManager.getOrgName()
                        )
                    } catch (e: Exception) { /* printer optional */ }
                }.onFailure {
                    Toast.makeText(holder.itemView.context, it.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun getItemCount() = items.size
}