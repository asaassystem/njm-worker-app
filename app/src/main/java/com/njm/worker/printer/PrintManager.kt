package com.njm.worker.printer

import android.content.Context
import com.njm.worker.data.model.Invoice
import com.njm.worker.data.model.WashRecord

/**
 * PrintManager - delegates to PrinterManager for Sunmi V2s
 * Uses reflection-based AIDL (no Sunmi SDK dependency required)
 */
object PrintManager {

    fun bindPrinter(context: Context, onReady: () -> Unit = {}) {
        PrinterManager.init(context, onReady)
    }

    fun printWashReceipt(context: Context, wash: WashRecord, activity: android.app.Activity) {
        PrinterManager.printWashReceipt(
            workerName = "",
            plateName = wash.plateNumber ?: "",
            carType = wash.carType ?: "",
            cost = wash.cost ?: 0.0,
            orgName = wash.orgName ?: ""
        )
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
