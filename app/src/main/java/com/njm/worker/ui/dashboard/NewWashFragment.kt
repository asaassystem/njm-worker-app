package com.njm.worker.ui.dashboard

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.njm.worker.R
import com.njm.worker.data.ApiClient
import com.njm.worker.data.model.WashRecord
import com.njm.worker.printer.PrinterManager
import com.njm.worker.utils.SessionManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * NewWashFragment - Record a new car wash
 * Navy/Gold design
 * Developer: meshari.tech
 */
class NewWashFragment : Fragment() {

    private lateinit var etCarSearch: AutoCompleteTextView
    private lateinit var etPlate: EditText
    private lateinit var etServiceType: EditText
    private lateinit var tvCarInfo: TextView
    private lateinit var btnRecord: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var tvResult: TextView

    private var selectedCarId: Int? = null
    private val searchHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var searchRunnable: Runnable? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_new_wash, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)
        setupCarSearch()
    }

    private fun initViews(view: View) {
        etCarSearch = view.findViewById(R.id.et_car_search)
        etPlate = view.findViewById(R.id.et_plate)
        etServiceType = view.findViewById(R.id.et_service_type)
        tvCarInfo = view.findViewById(R.id.tv_car_info)
        btnRecord = view.findViewById(R.id.btn_record_wash)
        progressBar = view.findViewById(R.id.progress_bar)
        tvResult = view.findViewById(R.id.tv_result)

        btnRecord.setOnClickListener { recordWash() }
    }

    private fun setupCarSearch() {
        etCarSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                searchRunnable?.let { searchHandler.removeCallbacks(it) }
                val query = s?.toString()?.trim() ?: ""
                if (query.length >= 2) {
                    searchRunnable = Runnable { searchCar(query) }
                    searchHandler.postDelayed(searchRunnable!!, 500)
                } else {
                    selectedCarId = null
                    tvCarInfo.visibility = View.GONE
                }
            }
        })

        etCarSearch.setOnItemClickListener { parent, _, position, _ ->
            val selected = parent.getItemAtPosition(position) as? String ?: return@setOnItemClickListener
            // Extract car ID from adapter tag if available
        }
    }

    private fun searchCar(query: String) {
        ApiClient.apiService.searchCar(query).enqueue(object : Callback<List<com.njm.worker.data.model.Car>> {
            override fun onResponse(call: Call<List<com.njm.worker.data.model.Car>>, response: Response<List<com.njm.worker.data.model.Car>>) {
                if (!isAdded) return
                val cars = response.body() ?: return
                val names = cars.map { "${it.owner_name} - ${it.plate_number}" }
                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, names)
                etCarSearch.setAdapter(adapter)
                adapter.notifyDataSetChanged()
                if (cars.size == 1) {
                    selectedCarId = cars[0].id
                    tvCarInfo.text = "${cars[0].owner_name}\n${cars[0].plate_number} - ${cars[0].car_model}"
                    tvCarInfo.visibility = View.VISIBLE
                }
            }

            override fun onFailure(call: Call<List<com.njm.worker.data.model.Car>>, t: Throwable) {
                if (isAdded) {
                    Toast.makeText(requireContext(), R.string.connection_error, Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    private fun recordWash() {
        val plate = etPlate.text.toString().trim()
        val serviceType = etServiceType.text.toString().trim()

        if (plate.isEmpty()) {
            etPlate.error = getString(R.string.required_field)
            return
        }

        val session = SessionManager(requireContext())
        val workerId = session.getWorkerId()

        showLoading(true)

        val washData = mapOf(
            "plate_number" to plate,
            "service_type" to serviceType.ifEmpty { "غسيل عادي" },
            "worker_id" to workerId.toString()
        )

        ApiClient.apiService.recordWash(washData).enqueue(object : Callback<WashRecord> {
            override fun onResponse(call: Call<WashRecord>, response: Response<WashRecord>) {
                if (!isAdded) return
                showLoading(false)
                if (response.isSuccessful) {
                    val record = response.body()
                    showSuccess(getString(R.string.wash_recorded))
                    clearForm()
                    record?.let { printReceipt(it) }
                } else {
                    showError(getString(R.string.record_failed))
                }
            }

            override fun onFailure(call: Call<WashRecord>, t: Throwable) {
                if (!isAdded) return
                showLoading(false)
                showError(getString(R.string.connection_error))
            }
        })
    }

    private fun printReceipt(record: WashRecord) {
        context?.let { ctx ->
            PrinterManager.printWashReceipt(ctx, record)
        }
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        btnRecord.isEnabled = !show
    }

    private fun showSuccess(msg: String) {
        tvResult.text = msg
        tvResult.setTextColor(resources.getColor(R.color.success_green, null))
        tvResult.visibility = View.VISIBLE
    }

    private fun showError(msg: String) {
        tvResult.text = msg
        tvResult.setTextColor(resources.getColor(R.color.error_red, null))
        tvResult.visibility = View.VISIBLE
    }

    private fun clearForm() {
        etCarSearch.text.clear()
        etPlate.text.clear()
        etServiceType.text.clear()
        tvCarInfo.visibility = View.GONE
        selectedCarId = null
    }
}
