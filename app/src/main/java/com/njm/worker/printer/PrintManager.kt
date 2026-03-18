package com.njm.worker.printer

import android.app.Activity
import android.content.Context
import com.njm.worker.data.model.CarDetail
import com.njm.worker.data.model.Invoice
import com.njm.worker.data.model.WashRecord
import com.njm.worker.data.model.WashResponse

/**
 * PrintManager wrapper - delegates to PrinterManager (Sunmi AIDL via InnerPrinterManager SDK)
 * Matches all call signatures used by UI fragments.
 * v6.0: Added workerName overload for TodayFragment/MonthFragment.
 * Developer: meshari.tech
 */
object PrintManager {

    fun bindPrinter(context: Context, onReady: () -> Unit = {}) {
        PrinterManager.init(context, onReady = onReady)
    }

    fun isPrinterAvailable(): Boolean = PrinterManager.isConnected()

    fun unbindPrinter(context: Context) {
        PrinterManager.unbindService(context)
    }

    // Called by TodayFragment and MonthFragment v6.0:
    // PrintManager.printWashReceipt(it, wash, workerName)
    fun printWashReceipt(context: Context, wash: WashRecord, workerName: String) {
        PrinterManager.printWashReceipt(
            workerName = workerName,
            plateName = wash.plateNumber ?: "",
            carType = wash.carType ?: "",
            cost = wash.cost ?: 0.0,
            orgName = wash.orgName ?: "",
            isPaid = (wash.isPaid ?: 1) == 1,
            context = context
        )
    }

    // Legacy overload: Called by old TodayFragment/MonthFragment versions
    // PrintManager.printWashReceipt(it, wash, it) where it = DashboardActivity
    fun printWashReceipt(context: Context, wash: WashRecord, activity: Activity) {
        PrinterManager.printWashReceipt(
            workerName = "",
            plateName = wash.plateNumber ?: "",
            carType = wash.carType ?: "",
            cost = wash.cost ?: 0.0,
            orgName = wash.orgName ?: "",
            isPaid = (wash.isPaid ?: 1) == 1,
            context = context
        )
    }

    // Called by NewWashFragment after recording wash:
    // PrintManager.printWashReceipt(act, car, resp, isPaid)
    fun printWashReceipt(context: Context, car: CarDetail, resp: WashResponse, isPaid: Int) {
        PrinterManager.printWashReceipt(
            workerName = "",
            plateName = car.plateNumber,
            carType = car.carTypeLabel ?: car.carType ?: "",
            cost = car.washPrice ?: resp.cost ?: 0.0,
            orgName = car.orgName ?: "",
            isPaid = (isPaid == 1),
            context = context
        )
    }

    // Called by NewWashFragment btnPrint:
    // PrintManager.printReceiptForCar(act, car)
    fun printReceiptForCar(context: Context, car: CarDetail) {
        PrinterManager.printWashReceipt(
            workerName = "",
            plateName = car.plateNumber,
            carType = car.carTypeLabel ?: car.carType ?: "",
            cost = car.washPrice ?: 0.0,
            orgName = car.orgName ?: "",
            context = context
        )
    }

    // Called by InvoicesFragment:
    // PrintManager.printInvoice(act, invoice)
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
            orgName = "NJM - مغسلة نجم",
            context = context
        )
    }

    fun printDailyReport(context: Context, washes: List<WashRecord>, date: String, activity: Activity) {
        PrinterManager.printDailyReport(context, washes, date)
    }

    fun printMonthlyReport(context: Context, washes: List<WashRecord>, month: String, activity: Activity) {
        PrinterManager.printMonthlyReport(context, washes, month)
    }
}
