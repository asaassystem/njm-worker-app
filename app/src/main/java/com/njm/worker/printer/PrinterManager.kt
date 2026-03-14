package com.njm.worker.printer

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.sunmi.peripheral.printer.InnerPrinterCallback
import com.sunmi.peripheral.printer.InnerPrinterException
import com.sunmi.peripheral.printer.InnerPrinterManager
import com.sunmi.peripheral.printer.SunmiPrinterService
import com.njm.worker.NjmApp
import com.njm.worker.data.model.Invoice
import com.njm.worker.data.model.WashRecord

/**
 * PrinterManager - Sunmi Built-in Printer via InnerPrinterManager (Official SDK)
  * FIXED: Robust binding with retry, force-rebind on explicit request, invoice number in receipt
   * Per Sunmi developer guide: developer.sunmi.com
    * Developer: meshari.tech
     */
object PrinterManager {
        private const val TAG = "NJM_Printer"
        private var printerService: SunmiPrinterService? = null
        private var connected = false
        private var bindingInProgress = false

        /**
             * Initialize / bind Sunmi printer.
                  * forceRebind=true: always try to rebind even if previously attempted (for "Detect" button).
                       */
        fun init(context: Context, forceRebind: Boolean = false, onReady: () -> Unit = {}) {
                    // If already connected, return immediately
                    val appPrinter = NjmApp.sunmiPrinter
                    if (appPrinter != null && NjmApp.printerConnected) {
                                    printerService = appPrinter
                                    connected = true
                                    onReady()
                                                return
                    }

                            // If force rebind: reset state and try again
                                    if (forceRebind) {
                                                    NjmApp.bindAttempted = false
                                                    NjmApp.printerConnected = false
                                                    NjmApp.sunmiPrinter = null
                                                    connected = false
                                                    bindingInProgress = false
                                    }

                                            // If binding already in progress, wait for it
                                                    if (bindingInProgress) {
                                                                    Handler(Looper.getMainLooper()).postDelayed({
                                                                                        val p = NjmApp.sunmiPrinter
                                                                                        if (p != null) {
                                                                                                                printerService = p
                                                                                                                connected = true
                                                                                                                NjmApp.printerConnected = true
                                                                                        } else {
                                                                                                                connected = false
                                                                                        }
                                                                                                        onReady()
                                                                    }, 3000)
                                                                                return
                                                    }

                                                            // If already tried and failed, try once more with delay
                                                                    if (NjmApp.bindAttempted && !NjmApp.printerConnected) {
                                                                                    // Reset and retry
                                                                                    NjmApp.bindAttempted = false
                                                                    }

                                                                            // Start binding
                                                                                    bindingInProgress = true
                    NjmApp.bindAttempted = true

                    try {
                                    InnerPrinterManager.getInstance().bindService(
                                                        context.applicationContext,
                                                        object : InnerPrinterCallback() {
                                                                                override fun onConnected(service: SunmiPrinterService?) {
                                                                                                            printerService = service
                                                                                                            connected = (service != null)
                                                                                                                                    NjmApp.sunmiPrinter = service
                                                                                                            NjmApp.printerConnected = connected
                                                                                                            bindingInProgress = false
                                                                                                            Log.i(TAG, if (connected) "Sunmi printer connected OK" else "Sunmi service null")
                                                                                                                                    onReady()
                                                                                }

                                                                                                    override fun onDisconnected() {
                                                                                                                                printerService = null
                                                                                                                                connected = false
                                                                                                                                NjmApp.sunmiPrinter = null
                                                                                                                                NjmApp.printerConnected = false
                                                                                                                                NjmApp.bindAttempted = false
                                                                                                                                bindingInProgress = false
                                                                                                                                Log.w(TAG, "Sunmi printer disconnected")
                                                                                                                                                        onReady()
                                                                                                    }
                                                        }
                                                                    )
                    } catch (e: InnerPrinterException) {
                                    Log.w(TAG, "Not a Sunmi device or AIDL error: ${e.message}")
                                                connected = false
                                    bindingInProgress = false
                                    onReady()
                    } catch (e: Exception) {
                                    Log.w(TAG, "Printer bind failed: ${e.message}")
                                                connected = false
                                    bindingInProgress = false
                                    onReady()
                    }
        }

            fun isConnected() = connected && printerService != null
        fun isAvailable() = isConnected()

            fun unbindService(context: Context) {
                        try {
                                        InnerPrinterManager.getInstance().unBindService(context, object : InnerPrinterCallback() {
                                                            override fun onConnected(service: SunmiPrinterService?) {}
                                                                            override fun onDisconnected() {
                                                                                                    printerService = null
                                                                                                    connected = false
                                                                                                    NjmApp.sunmiPrinter = null
                                                                                                    NjmApp.printerConnected = false
                                                                                                    NjmApp.bindAttempted = false
                                                                            }
                                        })
                        } catch (e: Exception) {
                                        Log.w(TAG, "Unbind: ${e.message}")
                        }
            }

                /**
                     * Print wash receipt immediately.
                          * If printer not connected, try to connect first then print.
                               */
                                   fun printWashReceipt(
                                               workerName: String,
                                               plateName: String,
                                               carType: String,
                                               cost: Double,
                                               orgName: String,
                                               isPaid: Boolean = true,
                                               washDate: String = "",
                                               washTime: String = "",
                                               invoiceNumber: String = "",
                                               context: Context? = null
                                           ) {
                                               val svc = printerService
                                               if (svc == null) {
                                                               Log.w(TAG, "printWashReceipt: printer not connected, skipping")
                                                                           // If context provided, try to connect then print
                                                                                       if (context != null) {
                                                                                                           init(context, forceRebind = false) {
                                                                                                                                   val svc2 = printerService
                                                                                                                                   if (svc2 != null) {
                                                                                                                                                               doActualPrint(svc2, workerName, plateName, carType, cost, orgName, isPaid, washDate, washTime, invoiceNumber)
                                                                                                                                   }
                                                                                                           }
                                                                                       }
                                                                                                   return
                                               }
                                                       doActualPrint(svc, workerName, plateName, carType, cost, orgName, isPaid, washDate, washTime, invoiceNumber)
                                   }

                                       private fun doActualPrint(
                                                   svc: SunmiPrinterService,
                                                   workerName: String,
                                                   plateName: String,
                                                   carType: String,
                                                   cost: Double,
                                                   orgName: String,
                                                   isPaid: Boolean,
                                                   washDate: String,
                                                   washTime: String,
                                                   invoiceNumber: String
                                               ) {
                                                   try {
                                                                   svc.printerInit(null)
                                                                               // Header
                                                                                           svc.setAlignment(1, null)
                                                                                                       svc.setFontSize(30f, null)
                                                                                                                   svc.printText("$orgName\n", null)
                                                                                                                               svc.setFontSize(22f, null)
                                                                                                                                           svc.printText("نظام نجم | غسيل سيارات\n", null)
                                                                                                                                                       svc.setAlignment(0, null)
                                                                                                                                                                   svc.setFontSize(20f, null)
                                                                                                                                                                               svc.printText("================================\n", null)
                                                                                                                                                                                           // Invoice number
                                                                                                                                                                                                       if (invoiceNumber.isNotBlank()) {
                                                                                                                                                                                                                           svc.printText("رقم الفاتورة: $invoiceNumber\n", null)
                                                                                                                                                                                                                                       }
                                                                                                                                                                                                                   // Car info
                                                                                                                                                                                                                               svc.printText("اللوحة  : $plateName\n", null)
                                                                                                                                                                                                                                           svc.printText("النوع   : $carType\n", null)
                                                                                                                                                                                                                                                       if (workerName.isNotBlank()) svc.printText("الموظف  : $workerName\n", null)
                                                                                                                                                                                                                                                                   if (washDate.isNotBlank()) svc.printText("التاريخ : $washDate\n", null)
                                                                                                                                                                                                                                                                               if (washTime.isNotBlank()) svc.printText("الوقت   : $washTime\n", null)
                                                                                                                                                                                                                                                                                           svc.printText("================================\n", null)
                                                                                                                                                                                                                                                                                                       // Cost breakdown
                                                                                                                                                                                                                                                                                                                   val vat = cost * 15.0 / 115.0
                                                                   svc.printText("قبل الضريبة: ${String.format("%.2f", cost - vat)} ر.س\n", null)
                                                                               svc.printText("ضريبة 15%%  : ${String.format("%.2f", vat)} ر.س\n", null)
                                                                                           svc.setFontSize(24f, null)
                                                                                                       svc.printText("الإجمالي  : ${String.format("%.2f", cost)} ر.س\n", null)
                                                                                                                   svc.setFontSize(20f, null)
                                                                                                                               svc.printText("الحالة    : ${if (isPaid) "مدفوع" else "غير مدفوع"}\n", null)
                                                                                                                                           svc.printText("================================\n", null)
                                                                                                                                                       svc.setAlignment(1, null)
                                                                                                                                                                   svc.printText("شكراً لزيارتكم\n\n\n", null)
                                                                                                                                                                               svc.cutPaper(null)
                                                                                                                                                                                           Log.i(TAG, "Print OK: plate=$plateName inv=$invoiceNumber")
                                                   } catch (e: Exception) {
                                                                   Log.e(TAG, "doActualPrint error: ${e.message}")
                                                   }
                                       }

                                           fun printDailyReport(
                                                       context: Context,
                                                       washes: List<WashRecord>,
                                                       date: String,
                                                       totalOverride: Double = -1.0,
                                                       paidOverride: Double = -1.0,
                                                       unpaidOverride: Double = -1.0
                                                   ) {
                                                       val svc = printerService ?: return
                                                       val total = if (totalOverride >= 0) totalOverride else washes.sumOf { it.cost ?: 0.0 }
                                                               val paid = if (paidOverride >= 0) paidOverride else washes.filter { (it.isPaid ?: 1) == 1 }.sumOf { it.cost ?: 0.0 }
                                                                       val unpaid = if (unpaidOverride >= 0) unpaidOverride else total - paid
                                                       try {
                                                                       svc.printerInit(null)
                                                                                   svc.setAlignment(1, null); svc.setFontSize(26f, null)
                                                                                               svc.printText("تقرير اليوم\n$date\n", null)
                                                                                                           svc.setAlignment(0, null); svc.setFontSize(20f, null)
                                                                                                                       svc.printText("================================\n", null)
                                                                                                                                   svc.printText("عدد الغسيل : ${washes.size}\n", null)
                                                                                                                                               svc.printText("الإجمالي   : ${String.format("%.2f", total)} ر.س\n", null)
                                                                                                                                                           svc.printText("المدفوع    : ${String.format("%.2f", paid)} ر.س\n", null)
                                                                                                                                                                       svc.printText("غير المدفوع: ${String.format("%.2f", unpaid)} ر.س\n", null)
                                                                                                                                                                                   svc.printText("================================\n", null)
                                                                                                                                                                                               svc.setAlignment(1, null); svc.printText("نظام نجم\n\n\n", null)
                                                                                                                                                                                                           svc.cutPaper(null)
                                                                                                                                                                                                                   } catch (e: Exception) { Log.e(TAG, "printDailyReport: ${e.message}") }
                                           }

                                               fun printMonthlyReport(
                                                           context: Context,
                                                           washes: List<WashRecord>,
                                                           month: String,
                                                           totalOverride: Double = -1.0,
                                                           paidOverride: Double = -1.0,
                                                           unpaidOverride: Double = -1.0
                                                       ) {
                                                           val svc = printerService ?: return
                                                           val total = if (totalOverride >= 0) totalOverride else washes.sumOf { it.cost ?: 0.0 }
                                                                   val paid = if (paidOverride >= 0) paidOverride else washes.filter { (it.isPaid ?: 1) == 1 }.sumOf { it.cost ?: 0.0 }
                                                                           val unpaid = if (unpaidOverride >= 0) unpaidOverride else total - paid
                                                           try {
                                                                           svc.printerInit(null)
                                                                                       svc.setAlignment(1, null); svc.setFontSize(26f, null)
                                                                                                   svc.printText("تقرير الشهر\n$month\n", null)
                                                                                                               svc.setAlignment(0, null); svc.setFontSize(20f, null)
                                                                                                                           svc.printText("================================\n", null)
                                                                                                                                       svc.printText("عدد الغسيل : ${washes.size}\n", null)
                                                                                                                                                   svc.printText("الإجمالي   : ${String.format("%.2f", total)} ر.س\n", null)
                                                                                                                                                               svc.printText("المدفوع    : ${String.format("%.2f", paid)} ر.س\n", null)
                                                                                                                                                                           svc.printText("غير المدفوع: ${String.format("%.2f", unpaid)} ر.س\n", null)
                                                                                                                                                                                       svc.printText("================================\n", null)
                                                                                                                                                                                                   svc.setAlignment(1, null); svc.printText("نظام نجم\n\n\n", null)
                                                                                                                                                                                                               svc.cutPaper(null)
                                                                                                                                                                                                                       } catch (e: Exception) { Log.e(TAG, "printMonthlyReport: ${e.message}") }
                                               }

                                                   fun printInvoice(context: Context, invoice: Invoice) {
                                                               val svc = printerService ?: return
                                                               val prefs = context.getSharedPreferences("print_settings", Context.MODE_PRIVATE)
                                                                       val orgName = prefs.getString("org_name", "مغسلة نجم") ?: "مغسلة نجم"
                                                               val vatNumber = prefs.getString("vat_number", "") ?: ""
                                                               val crNumber = prefs.getString("cr_number", "") ?: ""
                                                               val address = prefs.getString("address", "حفر الباطن") ?: "حفر الباطن"
                                                               try {
                                                                               svc.printerInit(null)
                                                                                           svc.setAlignment(1, null); svc.setFontSize(26f, null)
                                                                                                       svc.printText("فاتورة ضريبية - ZATCA\n$orgName\n", null)
                                                                                                                   svc.setAlignment(0, null); svc.setFontSize(20f, null)
                                                                                                                               svc.printText("================================\n", null)
                                                                                                                                           svc.printText("رقم الفاتورة: ${invoice.invoiceNumber}\n", null)
                                                                                                                                                       svc.printText("التاريخ: ${invoice.invoiceDate ?: ""}\n", null)
                                                                                                                                                                   if (vatNumber.isNotBlank()) svc.printText("الرقم الضريبي: $vatNumber\n", null)
                                                                                                                                                                               if (crNumber.isNotBlank()) svc.printText("السجل التجاري: $crNumber\n", null)
                                                                                                                                                                                           svc.printText("العنوان: $address\n", null)
                                                                                                                                                                                                       svc.printText("================================\n", null)
                                                                                                                                                                                                                   svc.printText("الإجمالي: ${String.format("%.2f", invoice.totalAmount ?: 0.0)} ر.س\n", null)
                                                                                                                                                                                                                               svc.printText("الضريبة : ${String.format("%.2f", invoice.vatAmount ?: 0.0)} ر.س\n", null)
                                                                                                                                                                                                                                           svc.printText("المجموع : ${String.format("%.2f", invoice.grandTotal ?: 0.0)} ر.س\n", null)
                                                                                                                                                                                                                                                       svc.printText("================================\n", null)
                                                                                                                                                                                                                                                                   svc.setAlignment(1, null)
                                                                                                                                                                                                                                                                               svc.printText("نظام نجم\n\n\n", null)
                                                                                                                                                                                                                                                                                           svc.cutPaper(null)
                                                                                                                                                                                                                                                                                                   } catch (e: Exception) { Log.e(TAG, "printInvoice: ${e.message}") }
                                                   }
}
