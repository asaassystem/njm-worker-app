package com.njm.worker.ui.dashboard

import android.app.Activity
import android.content.Context
import com.njm.worker.data.model.CarDetail
import com.njm.worker.data.model.Invoice
import com.njm.worker.data.model.WashRecord
import com.njm.worker.data.model.WashResponse
import com.njm.worker.printer.PrinterManager

/**
 * PrintManager - UI layer wrapper for PrinterManager
 * Developer: meshari.tech
 */
object PrintManager {

    fun bindPrinter(context: Context, onReady: () -> Unit = {}) {
        PrinterManager.init(context, onReady)
    }

    fun isPrinterAvailable(): Boolean = PrinterManager.isConnected()

    fun unbindPrinter(context: Context) {
        PrinterManager.unbindService(context)
    }

    /** Called from TodayFragment / MonthFragment */
    fun printWashReceipt(context: Context, wash: WashRecord, activity: Activity) {
        PrinterManager.printWashReceipt(
            workerName = "",
            plateName = wash.plateNumber ?: "",
            carType = wash.carType ?: "",
            cost = wash.cost ?: 0.0,
            orgName = wash.orgName ?: "NJM",
            isPaid = (wash.isPaid ?: 1) == 1
        )
    }

    /** Called from NewWashFragment after recording */
    fun printWashReceipt(context: Context, car: CarDetail, resp: WashResponse, isPaid: Int) {
        PrinterManager.printWashReceipt(
            workerName = "",
            plateName = car.plateNumber,
            carType = car.carTypeLabel ?: car.carType ?: "",
            cost = car.washPrice ?: resp.cost ?: 0.0,
            orgName = car.orgName ?: "NJM",
            isPaid = isPaid == 1
        )
    }

    /** Called from NewWashFragment btnPrint */
    fun printReceiptForCar(context: Context, car: CarDetail) {
        PrinterManager.printWashReceipt(
            workerName = "",
            plateName = car.plateNumber,
            carType = car.carTypeLabel ?: car.carType ?: "",
            cost = car.washPrice ?: 0.0,
            orgName = car.orgName ?: "NJM",
            isPaid = true
        )
    }

    /** Called from InvoicesFragment */
    fun printInvoice(context: Context, invoice: Invoice) {
        PrinterManager.printInvoice(context, invoice)
    }

    /** Called from PrintSettingsFragment test button */
    fun printTest(context: Context, activity: Activity) {
        PrinterManager.printWashReceipt(
            workerName = "Test - اختبار",
            plateName = "ABC-1234",
            carType = "سيارة صغيرة",
            cost = 20.0,
            orgName = "NJM - مغسلة نجم",
            isPaid = true
        )
    }

    fun printDailyReport(context: Context, washes: List<WashRecord>, date: String, activity: Activity) {
        PrinterManager.printDailyReport(context, washes, date)
    }

    fun printMonthlyReport(context: Context, washes: List<WashRecord>, month: String, activity: Activity) {
        PrinterManager.printMonthlyReport(context, washes, month)
    }
}
