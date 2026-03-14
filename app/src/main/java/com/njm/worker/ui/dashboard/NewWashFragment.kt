package com.njm.worker.ui.dashboard

import android.app.AlertDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.njm.worker.R
import com.njm.worker.data.model.CarDetail
import com.njm.worker.data.repository.WorkerRepository
import com.njm.worker.printer.PrintManager
import com.njm.worker.utils.SessionManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * NewWashFragment - Design v2
 * Developer: meshari.tech
 */
class NewWashFragment : Fragment() {

    private val repo = WorkerRepository()
    private var selectedCar: CarDetail? = null
    private var searchJob: Job? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_new_wash, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupSearch(view)
        setupForm(view)
    }

    private fun setupSearch(view: View) {
        val etPlate = view.findViewById<EditText>(R.id.etPlateSearch)
        val rvCars = view.findViewById<ListView>(R.id.lvSearchResults)
        val tvStatus = view.findViewById<TextView>(R.id.tvSearchStatus)
        val cardCarInfo = view.findViewById<View>(R.id.cardCarInfo)
        etPlate.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString()?.trim() ?: ""
                searchJob?.cancel()
                if (query.length >= 2) {
                    searchJob = lifecycleScope.launch {
                        delay(500)
                        performSearch(query, rvCars, tvStatus, cardCarInfo, view)
                    }
                } else {
                    rvCars.visibility = View.GONE
                    cardCarInfo.visibility = View.GONE
                    selectedCar = null
                    updateRecordButton(view)
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun performSearch(query: String, lv: ListView, tvStatus: TextView, cardCarInfo: View, root: View) {
        tvStatus.text = getLangStr("searching")
        tvStatus.visibility = View.VISIBLE
        lifecycleScope.launch {
            val result = repo.searchCar(query)
            result.onSuccess { resp ->
                if (resp.success) {
                    val cars = resp.cars ?: listOfNotNull(resp.car)
                    if (cars.isEmpty()) {
                        tvStatus.text = getLangStr("no_cars_found")
                        lv.visibility = View.GONE
                        cardCarInfo.visibility = View.GONE
                    } else if (cars.size == 1) {
                        tvStatus.visibility = View.GONE
                        lv.visibility = View.GONE
                        showCarInfo(cars[0], cardCarInfo, root)
                    } else {
                        tvStatus.visibility = View.GONE
                        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1,
                            cars.map { "${dollar}{it.plateNumber} - ${dollar}{it.carTypeLabel ?: it.carType ?: ""}" })
                        lv.adapter = adapter
                        lv.visibility = View.VISIBLE
                        lv.setOnItemClickListener { _, _, pos, _ ->
                            lv.visibility = View.GONE
                            showCarInfo(cars[pos], cardCarInfo, root)
                        }
                    }
                } else {
                    tvStatus.text = resp.message ?: getLangStr("error")
                }
            }.onFailure {
                tvStatus.text = getLangStr("connection_error")
            }
        }
    }

    private fun showCarInfo(car: CarDetail, cardCarInfo: View, root: View) {
        selectedCar = car
        cardCarInfo.visibility = View.VISIBLE
        root.findViewById<TextView>(R.id.tvCarPlate)?.text = car.plateNumber
        root.findViewById<TextView>(R.id.tvCarType)?.text = car.carTypeLabel ?: car.carType ?: "-"
        root.findViewById<TextView>(R.id.tvCarOwner)?.text = car.ownerName ?: "-"
        root.findViewById<TextView>(R.id.tvCarPhone)?.text = car.ownerPhone ?: "-"
        root.findViewById<TextView>(R.id.tvCarBrand)?.text = "${dollar}{car.carBrand ?: ""} ${dollar}{car.carModel ?: ""} ${dollar}{car.modelYear ?: ""}".trim()
        root.findViewById<TextView>(R.id.tvCarOrg)?.text = car.orgName ?: "-"
        val price = car.washPrice ?: 0.0
        root.findViewById<TextView>(R.id.tvWashPrice)?.text = "${dollar}price ${dollar}{getLangStr("sar")}"
        updateRecordButton(root)
    }

    private fun setupForm(view: View) {
        val etNotes = view.findViewById<EditText>(R.id.etWashNotes)
        val btnRecord = view.findViewById<Button>(R.id.btnRecordWash)
        val btnPrint = view.findViewById<Button>(R.id.btnPrintReceipt)
        val switchPaid = view.findViewById<Switch>(R.id.switchPaid)
        updateRecordButton(view)
        btnRecord?.setOnClickListener {
            val car = selectedCar ?: return@setOnClickListener
            val isPaid = if (switchPaid?.isChecked == true) 1 else 0
            val notes = etNotes?.text?.toString() ?: ""
            val lang = SessionManager.getLang(requireContext())
            AlertDialog.Builder(requireContext())
                .setTitle(getLangStr("confirm_wash"))
                .setMessage("${dollar}{getLangStr("plate")}: ${dollar}{car.plateNumber}\n${dollar}{getLangStr("price")}: ${dollar}{car.washPrice ?: 0.0} ${dollar}{getLangStr("sar")}")
                .setPositiveButton(getLangStr("confirm")) { _, _ -> recordWash(car, isPaid, notes, lang, view) }
                .setNegativeButton(getLangStr("cancel"), null)
                .show()
        }
        btnPrint?.setOnClickListener {
            val car = selectedCar ?: return@setOnClickListener
            val act = activity ?: return@setOnClickListener
            PrintManager.printReceiptForCar(act, car)
        }
    }

    private fun recordWash(car: CarDetail, isPaid: Int, notes: String, lang: String, view: View) {
        val progressBar = view.findViewById<ProgressBar>(R.id.progressWash)
        progressBar?.visibility = View.VISIBLE
        view.findViewById<Button>(R.id.btnRecordWash)?.isEnabled = false
        lifecycleScope.launch {
            val result = repo.recordWash(car.id, isPaid, notes, lang)
            progressBar?.visibility = View.GONE
            view.findViewById<Button>(R.id.btnRecordWash)?.isEnabled = true
            result.onSuccess { resp ->
                if (resp.success) {
                    AlertDialog.Builder(requireContext())
                        .setTitle(getLangStr("success"))
                        .setMessage("${dollar}{getLangStr("wash_recorded")}\n${dollar}{getLangStr("invoice_number")}: ${dollar}{resp.invoiceNumber ?: "-"}")
                        .setPositiveButton(getLangStr("print")) { _, _ ->
                            activity?.let { act -> PrintManager.printWashReceipt(act, car, resp, isPaid) }
                        }
                        .setNegativeButton(getLangStr("close"), null)
                        .show()
                    resetForm(view)
                } else {
                    Toast.makeText(requireContext(), resp.message ?: getLangStr("error"), Toast.LENGTH_SHORT).show()
                }
            }.onFailure {
                Toast.makeText(requireContext(), getLangStr("connection_error"), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun resetForm(view: View) {
        view.findViewById<EditText>(R.id.etPlateSearch)?.setText("")
        view.findViewById<View>(R.id.cardCarInfo)?.visibility = View.GONE
        view.findViewById<EditText>(R.id.etWashNotes)?.setText("")
        view.findViewById<Switch>(R.id.switchPaid)?.isChecked = true
        selectedCar = null
        updateRecordButton(view)
    }

    private fun updateRecordButton(view: View) {
        val btn = view.findViewById<Button>(R.id.btnRecordWash)
        btn?.isEnabled = selectedCar != null
        btn?.alpha = if (selectedCar != null) 1.0f else 0.5f
    }

    private fun getLangStr(key: String): String {
        val lang = SessionManager.getLang(requireContext())
        return when (key) {
            "searching" -> when (lang) { "en" -> "Searching..."; else -> "جاري البحث..." }
            "no_cars_found" -> when (lang) { "en" -> "No cars found"; else -> "لا توجد سيارات" }
            "confirm_wash" -> when (lang) { "en" -> "Confirm Wash"; else -> "تاكيد الغسيل" }
            "plate" -> when (lang) { "en" -> "Plate"; else -> "اللوحة" }
            "price" -> when (lang) { "en" -> "Price"; else -> "السعر" }
            "paid" -> when (lang) { "en" -> "Paid"; else -> "مدفوع" }
            "unpaid" -> when (lang) { "en" -> "Unpaid"; else -> "غير مدفوع" }
            "confirm" -> when (lang) { "en" -> "Confirm"; else -> "تاكيد" }
            "cancel" -> when (lang) { "en" -> "Cancel"; else -> "الغاء" }
            "wash_recorded" -> when (lang) { "en" -> "Wash Recorded!"; else -> "تم تسجيل الغسيل!" }
            "invoice_number" -> when (lang) { "en" -> "Invoice #"; else -> "رقم الفاتورة" }
            "success" -> when (lang) { "en" -> "Success"; else -> "نجاح" }
            "print" -> when (lang) { "en" -> "Print"; else -> "طباعة" }
            "close" -> when (lang) { "en" -> "Close"; else -> "اغلاق" }
            "sar" -> when (lang) { "en" -> "SAR"; else -> "ر.س" }
            "error" -> when (lang) { "en" -> "Error"; else -> "خطا" }
            "connection_error" -> when (lang) { "en" -> "Connection error"; else -> "خطا في الاتصال" }
            else -> key
        }
    }
}
