package com.njm.worker.ui.search

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.njm.worker.R
import com.njm.worker.data.model.Car
import com.njm.worker.data.repository.WorkerRepository
import kotlinx.coroutines.launch

class SearchActivity : AppCompatActivity() {
    private val repo = WorkerRepository()
    private lateinit var etPlate: EditText
    private lateinit var rvCars: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmpty: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        etPlate = findViewById(R.id.etPlate)
        rvCars = findViewById(R.id.rvCars)
        progressBar = findViewById(R.id.progressBar)
        tvEmpty = findViewById(R.id.tvEmpty)

        rvCars.layoutManager = LinearLayoutManager(this)

        findViewById<Button>(R.id.btnSearch).setOnClickListener {
            val plate = etPlate.text.toString().trim()
            if (plate.isNotEmpty()) doSearch(plate)
        }

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }
    }

    private fun doSearch(plate: String) {
        progressBar.visibility = View.VISIBLE
        tvEmpty.visibility = View.GONE
        rvCars.visibility = View.GONE

        lifecycleScope.launch {
            val result = repo.searchCar(plate)
            progressBar.visibility = View.GONE
            result.onSuccess { data ->
                val cars = data.cars ?: emptyList()
                if (cars.isEmpty()) {
                    tvEmpty.visibility = View.VISIBLE
                } else {
                    rvCars.visibility = View.VISIBLE
                    rvCars.adapter = CarAdapter(cars) { car -> doRecordWash(car) }
                }
            }
            result.onFailure {
                tvEmpty.visibility = View.VISIBLE
                Toast.makeText(this@SearchActivity, it.message ?: "Error", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun doRecordWash(car: Car) {
        lifecycleScope.launch {
            val result = repo.recordWash(car.id)
            result.onSuccess {
                Toast.makeText(this@SearchActivity, getString(R.string.wash_recorded), Toast.LENGTH_SHORT).show()
                finish()
            }
            result.onFailure {
                Toast.makeText(this@SearchActivity, it.message ?: "Error", Toast.LENGTH_SHORT).show()
            }
        }
    }
}