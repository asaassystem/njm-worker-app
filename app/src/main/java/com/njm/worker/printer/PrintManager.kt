package com.njm.worker.printer

import android.content.Context
import android.widget.Toast
import com.njm.worker.data.model.Invoice
import com.njm.worker.data.model.WashRecord
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * PrintManager - delegates to PrinterManager for Sunmi V2s printer
 * Uses reflection-based AIDL (no Sunmi SDK dependency required)
 */
object PrintManager {

    fun bindPrinter(context: Context) {
        PrinterManager.bindService(context) {
            // Printer ready
        }
    }

    fun printWashReceipt(context: Context, wash: WashRecord, activity: android.app.Activity) {
        PrinterManager.printWashReceipt(context, wash)
    }

    fun printInvoice(context: Context, invoice: Invoice, activity: android.app.Activity) {
        PrinterManager.printInvoice(context, invoice)
    }

    fun printDailyReport(context: Context, washes: List<WashRecord>, date: String, activity: android.app.Activity) {
        PrinterManager.printDailyReport(context, washes, date)
    }

    fun printMonthlyReport(context: Context, washes: List<WashRecord>, month: String, activity: android.app.Activity) {
        PrinterManager.printMonthlyReport(context, washes, month)
    }

    fun isPrinterAvailable(): Boolean = PrinterManager.isConnected()

    fun unbindPrinter(context: Context) {
        PrinterManager.unbindService(context)
    }
}
