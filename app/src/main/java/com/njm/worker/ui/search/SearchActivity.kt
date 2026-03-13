package com.njm.worker.ui.search

import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.njm.worker.R
import com.njm.worker.data.model.Car
import com.njm.worker.data.repository.WorkerRepository
import kotlinx.coroutines.launch

class SearchActivity : AppCompatActivity() {
    private val repo = WorkerRepository()
    private lateinit var adapter: CarAdapter
    private val cars = mutableListOf<Car>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)
        setupViews()
    }

    private fun setupViews() {
        val etPlate = findViewById<TextInputEditText>(R.id.etPlateNumber)
        val rvCars = findViewById<RecyclerView>(R.id.rvCars)
        val tvNoResults = findViewById<TextView>(R.id.tvNoResults)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)

        adapter = CarAdapter(cars, repo, lifecycleScope)
        rvCars.layoutManager = LinearLayoutManager(this)
        rvCars.adapter = adapter

        fun doSearch() {
            val plate = etPlate.text.toString().trim()
            if (plate.isEmpty()) { Toast.makeText(this, "Enter plate number", Toast.LENGTH_SHORT).show(); return }
            progressBar.visibility = View.VISIBLE
            lifecycleScope.launch {
                val result = repo.searchCar(plate)
                progressBar.visibility = View.GONE
                result.onSuccess { resp ->
                    cars.clear()
                    cars.addAll(resp.cars ?: emptyList())
                    adapter.notifyDataSetChanged()
                    tvNoResults.visibility = if (cars.isEmpty()) View.VISIBLE else View.GONE
                }.onFailure {
                    Toast.makeText(this@SearchActivity, it.message, Toast.LENGTH_SHORT).show()
                }
            }
        }

        findViewById<Button>(R.id.btnSearch).setOnClickListener { doSearch() }
        etPlate.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) { doSearch(); true } else false
        }
    }
}