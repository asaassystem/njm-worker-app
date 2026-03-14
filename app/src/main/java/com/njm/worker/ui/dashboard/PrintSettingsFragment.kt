package com.njm.worker.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.njm.worker.R
import com.njm.worker.printer.PrinterManager

/**
 * PrintSettingsFragment - Printer settings and test print
 * Navy/Gold design with Sunmi printer status
 * Developer: meshari.tech
 */
class PrintSettingsFragment : Fragment() {

    private lateinit var tvPrinterStatus: TextView
    private lateinit var tvPrinterModel: TextView
    private lateinit var btnTestPrint: Button
    private lateinit var btnCheckStatus: Button

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_print_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)
        checkPrinterStatus()
    }

    private fun initViews(view: View) {
        tvPrinterStatus = view.findViewById(R.id.tv_printer_status)
        tvPrinterModel = view.findViewById(R.id.tv_printer_model)
        btnTestPrint = view.findViewById(R.id.btn_test_print)
        btnCheckStatus = view.findViewById(R.id.btn_check_status)

        btnTestPrint.setOnClickListener { testPrint() }
        btnCheckStatus.setOnClickListener { checkPrinterStatus() }
    }

    private fun checkPrinterStatus() {
        context?.let { ctx ->
            val isConnected = PrinterManager.isConnected()
            if (isConnected) {
                tvPrinterStatus.text = getString(R.string.printer_connected)
                tvPrinterStatus.setTextColor(resources.getColor(R.color.success_green, null))
                tvPrinterModel.text = getString(R.string.sunmi_built_in)
                btnTestPrint.isEnabled = true
            } else {
                tvPrinterStatus.text = getString(R.string.printer_not_connected)
                tvPrinterStatus.setTextColor(resources.getColor(R.color.error_red, null))
                tvPrinterModel.text = getString(R.string.connecting)
                btnTestPrint.isEnabled = false
                // Try to reconnect
                PrinterManager.init(ctx) {
                    if (isAdded) {
                        checkPrinterStatus()
                    }
                }
            }
        }
    }

    private fun testPrint() {
        context?.let { ctx ->
            PrinterManager.printTestPage(ctx)
            Toast.makeText(ctx, R.string.printing_test, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        checkPrinterStatus()
    }
}
