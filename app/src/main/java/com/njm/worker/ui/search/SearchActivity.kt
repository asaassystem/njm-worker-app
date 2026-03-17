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
import com.njm.worker.data.model.WashRecord
import com.njm.worker.data.repository.WorkerRepository
import com.njm.worker.printer.PrintManager
import kotlinx.coroutines.launch

/**
 * SearchActivity - Car search and wash recording
 * v5.0: FIXED wrong PrintManager import (was ui.dashboard, now printer)
 * Arabic UI, cleaner operational flow
 * Developer: meshari.tech
 */
class SearchActivity : AppCompatActivity() {

    private val repo = WorkerRepository()
    private var currentCar: CarDetail? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "بحث عن سيارة"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setupViews()
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }

    private fun setupViews() {
        val etPlateNumber = findViewById<EditText>(R.id.etPlateNumber)
        val btnSearch = findViewById<Button>(R.id.btnSearch)
        val btnRecordWash = findViewById<Button>(R.id.btnRecordWash)

        btnSearch.setOnClickListener {
            val plate = etPlateNumber.text.toString().trim()
            if (plate.isNotEmpty()) doSearch(plate)
            else Toast.makeText(this, "أدخل رقم اللوحة", Toast.LENGTH_SHORT).show()
        }

        etPlateNumber.setOnEditorActionListener { _, _, _ ->
            val plate = etPlateNumber.text.toString().trim()
            if (plate.isNotEmpty()) doSearch(plate)
            true
        }

        btnRecordWash?.setOnClickListener {
            currentCar?.let { car ->
                val printReceipt = try { findViewById<SwitchCompat>(R.id.switchPrintReceipt)?.isChecked ?: false } catch (e: Exception) { false }
                doRecordWash(car.id, printReceipt)
            }
        }
    }

    private fun doSearch(plate: String) {
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        val tvNoResults = findViewById<TextView>(R.id.tvNoResults)
        val cardCar = findViewById<View>(R.id.cardCar)
        progressBar?.visibility = View.VISIBLE
        tvNoResults?.visibility = View.GONE
        cardCar?.visibility = View.GONE
        currentCar = null
        lifecycleScope.launch {
            val result = repo.searchCar(plate)
            progressBar?.visibility = View.GONE
            result.onSuccess { data ->
                val car = data.car
                if (car == null) { tvNoResults?.text = data.message ?: "لم يتم العثور"; tvNoResults?.visibility = View.VISIBLE }
                else { currentCar = car; showCarDetails(car); cardCar?.visibility = View.VISIBLE }
            }
            result.onFailure { tvNoResults?.text = it.message ?: "خطأ"; tvNoResults?.visibility = View.VISIBLE }
        }
    }

    private fun showCarDetails(car: CarDetail) {
        val carTypeLabel = when (car.carType?.lowercase()) {
            "large" -> "كبير"
            "small" -> "صغير"
            else -> car.carTypeLabel ?: car.carType ?: ""
        }
        findViewById<TextView>(R.id.tvCarPlate)?.text = car.plateNumber
        findViewById<TextView>(R.id.tvCarType)?.text = carTypeLabel
        findViewById<TextView>(R.id.tvCarOrg)?.text = car.orgName ?: ""
        val price = car.washPrice ?: 0.0
        findViewById<TextView>(R.id.tvCarPrice)?.text = String.format("%.0f ر.س", price)
    }

    private fun doRecordWash(carId: Int, printReceipt: Boolean) {
        lifecycleScope.launch {
            repo.recordWash(carId)
                .onSuccess { data ->
                    if (data.success) {
                        Toast.makeText(this@SearchActivity, "تم تسجيل الغسيل ✓", Toast.LENGTH_SHORT).show()
                        if (printReceipt) {
                            val wash = WashRecord(id = data.washId ?: 0,
                                plateNumber = data.plate ?: currentCar?.plateNumber,
                                carType = currentCar?.carType, orgName = currentCar?.orgName,
                                cost = data.cost, isPaid = 1,
                                washDate = null, washTime = null, ownerName = null, ownerPhone = null)
                            PrintManager.printWashReceipt(this@SearchActivity, wash, this@SearchActivity)
                        }
                        finish()
                    } else {
                        Toast.makeText(this@SearchActivity, data.message ?: "فشل", Toast.LENGTH_SHORT).show()
                    }
                }
                .onFailure { Toast.makeText(this@SearchActivity, it.message ?: "خطأ", Toast.LENGTH_SHORT).show() }
        }
    }
}
