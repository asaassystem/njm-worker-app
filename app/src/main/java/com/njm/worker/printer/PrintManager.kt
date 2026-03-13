package com.njm.worker.printer

import android.app.Activity
import android.content.Context
import com.njm.worker.data.model.Invoice
import com.njm.worker.data.model.WashRecord

/**
 * PrintManager wrapper - delegates to PrinterManager (Sunmi AIDL via reflection)
 * Matches all call signatures used by UI fragments.
 */
object PrintManager {

    fun bindPrinter(context: Context, onReady: () -> Unit = {}) {
        PrinterManager.init(context, onReady)
    }

    fun isPrinterAvailable(): Boolean = PrinterManager.isConnected()

    fun unbindPrinter(context: Context) {
        PrinterManager.unbindService(context)
    }

    // Called by TodayFragment and MonthFragment:
    // PrintManager.printWashReceipt(it, wash, it)  where it = DashboardActivity
    fun printWashReceipt(context: Context, wash: WashRecord, activity: Activity) {
        PrinterManager.printWashReceipt(
            workerName = "",
            plateName = wash.plateNumber ?: "",
            carType = wash.carType ?: "",
            cost = wash.cost ?: 0.0,
            orgName = wash.orgName ?: ""
        )
    }

    // Called by InvoicesFragment:
    // PrintManager.printInvoice(act, invoice)  - 2 args
    fun printInvoice(context: Context, invoice: Invoice) {
        PrinterManager.printInvoice(context, invoice)
    }

    // Called by PrintSettingsFragment:
    // PrintManager.printTest(requireContext(), requireActivity())
    fun printTest(context: Context, activity: Activity) {
        PrinterManager.printWashReceipt(
            workerName = "Test Worker",
            plateName = "ABC-123",
            carType = "صغير",
            cost = 20.0,
            orgName = "NJM - مغسلة نجم"
        )
    }

    fun printDailyReport(context: Context, washes: List<WashRecord>, date: String, activity: Activity) {
        PrinterManager.printDailyReport(context, washes, date)
    }

    fun printMonthlyReport(context: Context, washes: List<WashRecord>, month: String, activity: Activity) {
        PrinterManager.printMonthlyReport(context, washes, month)
    }
}
