package com.njm.worker.ui.dashboard

import android.content.Context
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
import kotlinx.coroutines.launch

class PrintSettingsFragment : Fragment() {
    private val repo = WorkerRepository()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_print_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews(view)
        loadOrgInfo(view)
    }

    private fun setupViews(view: View) {
        val rgPrintMethod = view.findViewById<RadioGroup>(R.id.rgPrintMethod)
        val layoutNetworkPrinter = view.findViewById<View>(R.id.layoutNetworkPrinter)
        val etPrinterIp = view.findViewById<TextInputEditText>(R.id.etPrinterIp)
        val etPrinterPort = view.findViewById<TextInputEditText>(R.id.etPrinterPort)
        val switchAutoPrint = view.findViewById<SwitchCompat>(R.id.switchAutoPrint)
        val btnSave = view.findViewById<Button>(R.id.btnSaveSettings)
        val btnTest = view.findViewById<Button>(R.id.btnTestPrint)

        // Load saved settings
        val prefs = requireContext().getSharedPreferences("print_settings", Context.MODE_PRIVATE)
        val method = prefs.getString("print_method", "sunmi")
        val ip = prefs.getString("printer_ip", "")
        val port = prefs.getInt("printer_port", 9100)
        val autoPrint = prefs.getBoolean("auto_print", true)

        if (method == "network") {
            view.findViewById<android.widget.RadioButton>(R.id.rbNetworkPrinter).isChecked = true
            layoutNetworkPrinter.visibility = View.VISIBLE
        }
        etPrinterIp.setText(ip)
        etPrinterPort.setText(port.toString())
        switchAutoPrint.isChecked = autoPrint

        rgPrintMethod.setOnCheckedChangeListener { _, checkedId ->
            layoutNetworkPrinter.visibility =
                if (checkedId == R.id.rbNetworkPrinter) View.VISIBLE else View.GONE
        }

        btnSave.setOnClickListener {
            val selectedMethod = if (rgPrintMethod.checkedRadioButtonId == R.id.rbNetworkPrinter) "network" else "sunmi"
            prefs.edit()
                .putString("print_method", selectedMethod)
                .putString("printer_ip", etPrinterIp.text.toString())
                .putInt("printer_port", etPrinterPort.text.toString().toIntOrNull() ?: 9100)
                .putBoolean("auto_print", switchAutoPrint.isChecked)
                .apply()
            Toast.makeText(requireContext(), "Settings saved", Toast.LENGTH_SHORT).show()
        }

        btnTest.setOnClickListener {
            PrintManager.printTest(requireContext(), requireActivity())
        }
    }

    private fun loadOrgInfo(view: View) {
        val tvOrgInfo = view.findViewById<TextView>(R.id.tvOrgInfo)
        lifecycleScope.launch {
            val result = repo.getSettings()
            result.onSuccess { data ->
                val orgName = data.org?.name ?: "Unknown"
                tvOrgInfo.text = "Org: $orgName"
            }
        }
    }
}