package com.njm.worker.printer

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.sunmi.printerx.PrinterSdk
import com.sunmi.printerx.api.PrinterSdk as PSdk
import com.sunmi.printerx.enums.Align
import com.sunmi.printerx.style.BaseStyle
import com.sunmi.printerx.style.TextStyle

object PrinterManager {
    private const val TAG = "PrinterManager"
    private var printer: PrinterSdk.Printer? = null

    fun init(context: Context, onReady: () -> Unit) {
        try {
            PrinterSdk.getInstance().getPrinter(context, object : PrinterSdk.PrinterListen {
                override fun onDefPrinter(p: PrinterSdk.Printer?) {
                    printer = p
                    Log.d(TAG, "Printer ready")
                    onReady()
                }
                override fun onPrinters(printers: MutableList<PrinterSdk.Printer>?) {}
            })
        } catch (e: Exception) {
            Log.e(TAG, "Init failed: " + e.message)
        }
    }

    fun printWashReceipt(workerName: String, plateName: String, carType: String, cost: Double, orgName: String) {
        val p = printer ?: run { Log.w(TAG, "Printer not ready"); return }
        try {
            val line = p.lineApi()
            line.initLine(BaseStyle.getStyle().setAlign(Align.CENTER))
            line.printText(orgName + "\n", TextStyle.getStyle().setTextSize(28).enableBold(true))
            line.printText("-------------------\n", TextStyle.getStyle())
            line.printText("Wash Receipt\n", TextStyle.getStyle().setTextSize(24))
            line.printText("-------------------\n", TextStyle.getStyle())
            line.initLine(BaseStyle.getStyle().setAlign(Align.LEFT))
            line.printText("Worker: " + workerName + "\n", TextStyle.getStyle())
            line.printText("Plate: " + plateName + "\n", TextStyle.getStyle())
            line.printText("Type: " + carType + "\n", TextStyle.getStyle())
            line.printText("Cost: " + cost + " SAR\n", TextStyle.getStyle())
            line.initLine(BaseStyle.getStyle().setAlign(Align.CENTER))
            line.printText("-------------------\n", TextStyle.getStyle())
            line.printText("Thank you!\n", TextStyle.getStyle())
            line.printText("\n\n\n", TextStyle.getStyle())
            line.autoOut()
        } catch (e: Exception) {
            Log.e(TAG, "Print failed: " + e.message)
        }
    }

    fun isPrinterReady(): Boolean = printer != null
}