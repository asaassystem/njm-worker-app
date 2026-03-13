package com.njm.worker.ui.search
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import com.njm.worker.R
import com.njm.worker.data.model.CarDetail
import com.njm.worker.data.repository.WorkerRepository
import com.njm.worker.ui.dashboard.PrintManager
import com.njm.worker.data.model.WashRecord
import kotlinx.coroutines.launch

class SearchActivity : AppCompatActivity() {
    private val repo = WorkerRepository()
    private var currentCar: CarDetail? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Search Car"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setupViews()
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }

    private fun setupViews() {
        val etPlateNumber = findViewById<EditText>(R.id.etPlateNumber)
        val btnSearch = findViewById<Button>(R.id.btnSearch)
        val btnRecordWash = findViewById<Button>(R.id.btnRecordWash)
        val cardCar = findViewById<View>(R.id.cardCar)

        btnSearch.setOnClickListener {
            val plate = etPlateNumber.text.toString().trim()
            if (plate.isNotEmpty()) doSearch(plate)
            else Toast.makeText(this, "Enter plate number", Toast.LENGTH_SHORT).show()
        }

        etPlateNumber.setOnEditorActionListener { _, _, _ ->
            val plate = etPlateNumber.text.toString().trim()
            if (plate.isNotEmpty()) doSearch(plate)
            true
        }

        btnRecordWash.setOnClickListener {
            currentCar?.let { car ->
                val printReceipt = findViewById<SwitchCompat>(R.id.switchPrintReceipt).isChecked
                doRecordWash(car.id, printReceipt)
            }
        }
    }

    private fun doSearch(plate: String) {
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        val tvNoResults = findViewById<TextView>(R.id.tvNoResults)
        val cardCar = findViewById<View>(R.id.cardCar)

        progressBar.visibility = View.VISIBLE
        tvNoResults.visibility = View.GONE
        cardCar.visibility = View.GONE
        currentCar = null

        lifecycleScope.launch {
            val result = repo.searchCar(plate)
            progressBar.visibility = View.GONE
            result.onSuccess { data ->
                val car = data.car
                if (car == null) {
                    tvNoResults.text = data.message ?: "No car found"
                    tvNoResults.visibility = View.VISIBLE
                } else {
                    currentCar = car
                    showCarDetails(car)
                    cardCar.visibility = View.VISIBLE
                }
            }
            result.onFailure {
                tvNoResults.text = it.message ?: "Error"
                tvNoResults.visibility = View.VISIBLE
            }
        }
    }

    private fun showCarDetails(car: CarDetail) {
        findViewById<TextView>(R.id.tvCarPlate).text = car.plateNumber
        findViewById<TextView>(R.id.tvCarType).text = car.carTypeLabel ?: car.carType ?: ""
        findViewById<TextView>(R.id.tvCarOrg).text = car.orgName ?: ""
        findViewById<TextView>(R.id.tvCarPrice).text = String.format("%.0f SAR", car.washPrice ?: 0.0)
    }

    private fun doRecordWash(carId: Int, printReceipt: Boolean) {
        lifecycleScope.launch {
            val result = repo.recordWash(carId)
            result.onSuccess { data ->
                if (data.success) {
                    Toast.makeText(this@SearchActivity, "Wash recorded! ${data.cost} SAR", Toast.LENGTH_SHORT).show()
                    if (printReceipt) {
                        val fakeWash = WashRecord(
                            id = data.washId ?: 0,
                            plateNumber = data.plate ?: currentCar?.plateNumber,
                            carType = currentCar?.carType,
                            orgName = currentCar?.orgName,
                            cost = data.cost,
                            isPaid = 1,
                            washDate = null,
                            washTime = null,
                            ownerName = null,
                            ownerPhone = null
                        )
                        PrintManager.printWashReceipt(this@SearchActivity, fakeWash, this@SearchActivity)
                    }
                    finish()
                } else {
                    Toast.makeText(this@SearchActivity, data.message ?: "Failed", Toast.LENGTH_SHORT).show()
                }
            }
            result.onFailure {
                Toast.makeText(this@SearchActivity, it.message ?: "Error", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
