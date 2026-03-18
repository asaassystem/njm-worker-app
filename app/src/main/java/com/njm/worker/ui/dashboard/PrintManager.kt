package com.njm.worker.ui.dashboard

import android.app.Activity
import android.content.Context
import com.njm.worker.data.model.CarDetail
import com.njm.worker.data.model.Invoice
import com.njm.worker.data.model.WashRecord
import com.njm.worker.data.model.WashResponse
import com.njm.worker.printer.PrinterManager
import com.njm.worker.utils.SessionManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * PrintManager - UI layer wrapper for PrinterManager
  * v6.0: Worker name fetched from SessionManager for all print calls
   * Developer: meshari.tech
    */
object PrintManager {

        fun bindPrinter(context: Context, onReady: () -> Unit = {}) = PrinterManager.init(context, onReady = onReady)
            fun isPrinterAvailable(): Boolean = PrinterManager.isConnected()
                fun unbindPrinter(context: Context) = PrinterManager.unbindService(context)

                    fun printWashReceipt(context: Context, wash: WashRecord, activity: Activity) {
                                val workerName = SessionManager.getWorkerName(context)
                                        PrinterManager.printWashReceipt(
                                                        workerName = workerName,
                                                        plateName = wash.plateNumber ?: "",
                                                        carType = wash.carType ?: "",
                                                        cost = wash.cost ?: 0.0,
                                                        orgName = wash.orgName ?: "NJM",
                                                        isPaid = (wash.isPaid ?: 1) == 1,
                                                        context = context
                                                    )
                    }

                        fun printWashReceipt(context: Context, car: CarDetail, resp: WashResponse, isPaid: Int) {
                                    val workerName = SessionManager.getWorkerName(context)
                                            PrinterManager.printWashReceipt(
                                                            workerName = workerName,
                                                            plateName = car.plateNumber,
                                                            carType = car.carTypeLabel ?: car.carType ?: "",
                                                            cost = car.washPrice ?: resp.cost ?: 0.0,
                                                            orgName = car.orgName ?: "NJM",
                                                            isPaid = isPaid == 1,
                                                            context = context
                                                        )
                        }

                            fun printReceiptForCar(context: Context, car: CarDetail) {
                                        val workerName = SessionManager.getWorkerName(context)
                                                PrinterManager.printWashReceipt(
                                                                workerName = workerName,
                                                                plateName = car.plateNumber,
                                                                carType = car.carTypeLabel ?: car.carType ?: "",
                                                                cost = car.washPrice ?: 0.0,
                                                                orgName = car.orgName ?: "NJM",
                                                                isPaid = true,
                                                                context = context
                                                            )
                            }

                                fun printInvoice(context: Context, invoice: Invoice) = PrinterManager.printInvoice(context, invoice)

                                    fun printTest(context: Context, activity: Activity) {
                                                val workerName = SessionManager.getWorkerName(context)
                                                        PrinterManager.printWashReceipt(
                                                                        workerName = if (workerName.isNotEmpty()) workerName else "Test Worker",
                                                                        plateName = "ABC-1234",
                                                                        carType = "\u0633\u064a\u0627\u0631\u0629 \u0635\u063a\u064a\u0631\u0629",
                                                                        cost = 20.0,
                                                                        orgName = "NJM - \u0645\u063a\u0633\u0644\u0629 \u0646\u062c\u0645",
                                                                        isPaid = true,
                                                                        context = context
                                                                    )
                                    }

                                        fun printDailyReport(context: Context, washes: List<WashRecord>, date: String, activity: Activity) {
                                                    PrinterManager.printDailyReport(context, washes, date)
                                        }

                                            fun printDailyReport(activity: Activity, washes: List<WashRecord>, total: Double, paid: Double, unpaid: Double) {
                                                        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                                                                PrinterManager.printDailyReport(activity, washes, date, total, paid, unpaid)
                                            }

                                                fun printMonthlyReport(context: Context, washes: List<WashRecord>, month: String, activity: Activity) {
                                                            PrinterManager.printMonthlyReport(context, washes, month)
                                                }

                                                    fun printMonthlyReport(activity: Activity, washes: List<WashRecord>, total: Double, paid: Double, unpaid: Double) {
                                                                val month = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
                                                                        PrinterManager.printMonthlyReport(activity, washes, month, total, paid, unpaid)
                                                    }
}
