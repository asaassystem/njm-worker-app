package com.njm.worker.ui.search

import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.njm.worker.data.model.Car
import com.njm.worker.data.repository.WorkerRepository
import com.njm.worker.databinding.ActivitySearchBinding
import com.njm.worker.printer.PrinterManager
import com.njm.worker.utils.SessionManager
import kotlinx.coroutines.launch

class SearchActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySearchBinding
    private lateinit var printerManager: PrinterManager
    private val repo = WorkerRepository()
    private val carAdapter = CarAdapter { car -> confirmWash(car) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        printerManager = PrinterManager(this)
        printerManager.initialize { _, _ -> }

        setupUI()
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener { finish() }

        binding.rvCars.layoutManager = LinearLayoutManager(this)
        binding.rvCars.adapter = carAdapter

        binding.btnSearch.setOnClickListener { performSearch() }

        binding.etPlate.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch(); true
            } else false
        }
    }

    private fun performSearch() {
        val plate = binding.etPlate.text.toString().trim()
        if (plate.isEmpty()) {
            binding.etPlate.error = "أدخل رقم اللوحة"
            return
        }

        setLoading(true)
        lifecycleScope.launch {
            repo.searchCar(plate)
                .onSuccess { cars ->
                    setLoading(false)
                    carAdapter.submitList(cars)
                    binding.tvNoResults.visibility = if (cars.isEmpty()) View.VISIBLE else View.GONE
                    binding.rvCars.visibility = if (cars.isEmpty()) View.GONE else View.VISIBLE
                }
                .onFailure { error ->
                    setLoading(false)
                    Toast.makeText(this@SearchActivity, "خطأ: " + error.message, Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun confirmWash(car: Car) {
        AlertDialog.Builder(this)
            .setTitle("تأكيد تسجيل الغسيل")
            .setMessage(
                "السيارة: " + car.plate_number + "
" +
                "النوع: " + car.car_type_label() + "
" +
                "المنشأة: " + car.org_name + "
" +
                "السعر: " + String.format("%.2f ريال", car.price)
            )
            .setPositiveButton("تسجيل + طباعة") { _, _ -> recordAndPrint(car) }
            .setNeutralButton("تسجيل فقط") { _, _ -> recordOnly(car) }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    private fun recordAndPrint(car: Car) {
        setLoading(true)
        lifecycleScope.launch {
            repo.recordWash(car.id)
                .onSuccess { response ->
                    setLoading(false)
                    if (response.success) {
                        Toast.makeText(this@SearchActivity, "تم التسجيل بنجاح", Toast.LENGTH_SHORT).show()
                        printerManager.printWashReceipt(
                            car.plate_number,
                            car.car_type_label(),
                            car.org_name,
                            response.cost,
                            SessionManager.workerName,
                            response.wash_id
                        ) { success, msg ->
                            runOnUiThread {
                                val printMsg = if (success) "تمت الطباعة" else "فشل الطباعة: $msg"
                                Toast.makeText(this@SearchActivity, printMsg, Toast.LENGTH_SHORT).show()
                            }
                        }
                        finish()
                    } else {
                        Toast.makeText(this@SearchActivity, response.message, Toast.LENGTH_SHORT).show()
                    }
                }
                .onFailure { error ->
                    setLoading(false)
                    Toast.makeText(this@SearchActivity, "خطأ: " + error.message, Toast.LENGTH_LONG).show()
                }
        }
    }

    private fun recordOnly(car: Car) {
        setLoading(true)
        lifecycleScope.launch {
            repo.recordWash(car.id)
                .onSuccess { response ->
                    setLoading(false)
                    val msg = if (response.success) "تم التسجيل: " + String.format("%.2f ريال", response.cost)
                              else response.message
                    Toast.makeText(this@SearchActivity, msg, Toast.LENGTH_SHORT).show()
                    if (response.success) finish()
                }
                .onFailure { error ->
                    setLoading(false)
                    Toast.makeText(this@SearchActivity, "خطأ: " + error.message, Toast.LENGTH_LONG).show()
                }
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnSearch.isEnabled = !loading
    }

    override fun onDestroy() {
        super.onDestroy()
        printerManager.destroy()
    }
}

fun Car.car_type_label() = if (car_type == "large") "كبير" else "صغير"