package com.njm.worker.ui.dashboard

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import com.njm.worker.R
import com.njm.worker.data.repository.WorkerRepository
import com.njm.worker.printer.PrinterManager
import kotlinx.coroutines.launch

/**
 * PrintSettingsFragment - Sunmi V2s printer detection, ZATCA invoice settings
 * Developer: meshari.tech
 */
class PrintSettingsFragment : Fragment() {

    private val repo = WorkerRepository()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_print_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews(view)
        loadOrgInfo(view)
        initPrinter(view)
    }

    private fun initPrinter(view: View) {
        val tvStatus = view.findViewById<TextView>(R.id.tvPrinterStatus)
        val tvModel = view.findViewById<TextView>(R.id.tvSunmiModel)

        val deviceModel = Build.MODEL
        tvModel?.text = "الجهاز: $deviceModel"

        PrinterManager.init(requireContext()) {
            activity?.runOnUiThread {
                val connected = PrinterManager.isConnected()
                tvStatus?.text = if (connected) "✅ متصلة | Connected" else "❌ غير متصلة | Disconnected"
                tvStatus?.setTextColor(
                    if (connected) android.graphics.Color.parseColor("#4CAF50")
                    else android.graphics.Color.parseColor("#F44336")
                )
            }
        }
    }

    private fun setupViews(view: View) {
        val rgPrintMethod = view.findViewById<RadioGroup>(R.id.rgPrintMethod)
        val layoutNetworkPrinter = view.findViewById<View>(R.id.layoutNetworkPrinter)
        val etPrinterIp = view.findViewById<TextInputEditText>(R.id.etPrinterIp)
        val etPrinterPort = view.findViewById<TextInputEditText>(R.id.etPrinterPort)
        val switchAutoPrint = view.findViewById<SwitchCompat>(R.id.switchAutoPrint)
        val switchPrintInvoice = view.findViewById<SwitchCompat>(R.id.switchPrintInvoice)
        val btnDetect = view.findViewById<Button>(R.id.btnDetectPrinter)
        val tvStatus = view.findViewById<TextView>(R.id.tvPrinterStatus)

        val etOrgName = view.findViewById<TextInputEditText>(R.id.etInvoiceOrgName)
        val etVatNumber = view.findViewById<TextInputEditText>(R.id.etVatNumber)
        val etCrNumber = view.findViewById<TextInputEditText>(R.id.etCrNumber)
        val etAddress = view.findViewById<TextInputEditText>(R.id.etAddress)
        val rgPaperSize = view.findViewById<RadioGroup>(R.id.rgPaperSize)

        val btnSave = view.findViewById<Button>(R.id.btnSaveSettings)
        val btnTest = view.findViewById<Button>(R.id.btnTestPrint)

        val prefs = requireContext().getSharedPreferences("print_settings", Context.MODE_PRIVATE)
        val method = prefs.getString("print_method", "sunmi")
        val ip = prefs.getString("printer_ip", "")
        val port = prefs.getInt("printer_port", 9100)
        val autoPrint = prefs.getBoolean("auto_print", true)
        val printInvoice = prefs.getBoolean("print_invoice", false)
        val paperSize = prefs.getString("paper_size", "58mm")
        val orgName = prefs.getString("org_name", "مغسلة نجم")
        val vatNumber = prefs.getString("vat_number", "")
        val crNumber = prefs.getString("cr_number", "")
        val address = prefs.getString("address", "حفر الباطن، المنطقة الشرقية")

        if (method == "network") {
            view.findViewById<android.widget.RadioButton>(R.id.rbNetworkPrinter)?.isChecked = true
            layoutNetworkPrinter?.visibility = View.VISIBLE
        }

        etPrinterIp?.setText(ip)
        etPrinterPort?.setText(port.toString())
        switchAutoPrint?.isChecked = autoPrint
        switchPrintInvoice?.isChecked = printInvoice
        etOrgName?.setText(orgName)
        etVatNumber?.setText(vatNumber)
        etCrNumber?.setText(crNumber)
        etAddress?.setText(address)

        if (paperSize == "80mm") {
            view.findViewById<android.widget.RadioButton>(R.id.rb80mm)?.isChecked = true
        }

        rgPrintMethod?.setOnCheckedChangeListener { _, checkedId ->
            layoutNetworkPrinter?.visibility =
                if (checkedId == R.id.rbNetworkPrinter) View.VISIBLE else View.GONE
        }

        btnDetect?.setOnClickListener {
            tvStatus?.text = "⏳ جاري الفحص..."
            tvStatus?.setTextColor(android.graphics.Color.parseColor("#FF9800"))
            PrinterManager.init(requireContext()) {
                activity?.runOnUiThread {
                    val connected = PrinterManager.isConnected()
                    tvStatus?.text = if (connected) "✅ متصلة | Connected" else "❌ غير متصلة | Disconnected"
                    tvStatus?.setTextColor(
                        if (connected) android.graphics.Color.parseColor("#4CAF50")
                        else android.graphics.Color.parseColor("#F44336")
                    )
                    Toast.makeText(
                        requireContext(),
                        if (connected) "الطابعة متصلة ✅" else "الطابعة غير متصلة ❌",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        btnSave?.setOnClickListener {
            val selectedMethod = if (rgPrintMethod?.checkedRadioButtonId == R.id.rbNetworkPrinter) "network" else "sunmi"
            val selectedPaper = if (rgPaperSize?.checkedRadioButtonId == R.id.rb80mm) "80mm" else "58mm"

            prefs.edit()
                .putString("print_method", selectedMethod)
                .putString("printer_ip", etPrinterIp?.text.toString())
                .putInt("printer_port", etPrinterPort?.text.toString().toIntOrNull() ?: 9100)
                .putBoolean("auto_print", switchAutoPrint?.isChecked ?: true)
                .putBoolean("print_invoice", switchPrintInvoice?.isChecked ?: false)
                .putString("paper_size", selectedPaper)
                .putString("org_name", etOrgName?.text.toString().ifBlank { "مغسلة نجم" })
                .putString("vat_number", etVatNumber?.text.toString())
                .putString("cr_number", etCrNumber?.text.toString())
                .putString("address", etAddress?.text.toString())
                .apply()

            Toast.makeText(requireContext(), "✅ تم حفظ الإعدادات", Toast.LENGTH_SHORT).show()
        }

        btnTest?.setOnClickListener {
            if (PrinterManager.isConnected()) {
                PrintManager.printTest(requireContext(), requireActivity())
                Toast.makeText(requireContext(), "🖨 جاري الطباعة التجريبية...", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "❌ الطابعة غير متصلة", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadOrgInfo(view: View) {
        val tvOrgInfo = view.findViewById<TextView>(R.id.tvOrgInfo)
        lifecycleScope.launch {
            try {
                val result = repo.getSettings()
                result.onSuccess { data ->
                    val orgName = data.org?.name ?: "NJM - مغسلة نجم"
                    tvOrgInfo?.text = "المنشأة: $orgName"
                }
            } catch (_: Exception) {
                tvOrgInfo?.text = "المنشأة: NJM - مغسلة نجم"
            }
        }
    }
}
