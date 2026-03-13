package com.njm.worker.printer

import android.content.Context
import android.util.Log
import com.sunmi.peripheral.printer.InnerPrinterCallback
import com.sunmi.peripheral.printer.InnerPrinterException
import com.sunmi.peripheral.printer.InnerPrinterManager
import com.sunmi.peripheral.printer.SunmiPrinterService
import com.njm.worker.data.model.Invoice
import com.njm.worker.data.model.WashRecord

/**
 * PrinterManager - Official Sunmi SDK integration
 * Uses InnerPrinterManager (com.sunmi:printerlibrary:1.0.23)
 * Developer: meshari.tech
 */
object PrinterManager {

    private const val TAG = "NJM_Printer"
    private var printerService: SunmiPrinterService? = null
    private var connected = false

    /**
     * Initialize and bind Sunmi printer service using official SDK
     */
    fun init(context: Context, onReady: () -> Unit = {}) {
        // If already connected from NjmApp, use it
        val appPrinter = com.njm.worker.NjmApp.sunmiPrinter
        if (appPrinter != null && com.njm.worker.NjmApp.printerConnected) {
            printerService = appPrinter
            connected = true
            Log.d(TAG, "Reusing printer from NjmApp")
            onReady()
            return
        }

        try {
            InnerPrinterManager.getInstance().bindService(
                context,
                object : InnerPrinterCallback() {
                    override fun onConnected(service: SunmiPrinterService?) {
                        printerService = service
                        connected = true
                        com.njm.worker.NjmApp.sunmiPrinter = service
                        com.njm.worker.NjmApp.printerConnected = true
                        Log.d(TAG, "✅ Sunmi printer connected")
                        onReady()
                    }

                    override fun onDisconnected() {
                        printerService = null
                        connected = false
                        com.njm.worker.NjmApp.printerConnected = false
                        Log.w(TAG, "❌ Printer disconnected")
                        onReady()
                    }
                }
            )
        } catch (e: InnerPrinterException) {
            Log.w(TAG, "Not a Sunmi device: ${e.message}")
            connected = false
            onReady()
        } catch (e: Exception) {
            Log.w(TAG, "Printer init error: ${e.message}")
            connected = false
            onReady()
        }
    }

    fun isConnected(): Boolean = connected && printerService != null

    fun unbindService(context: Context) {
        try {
            if (connected) {
                InnerPrinterManager.getInstance().unBindService(context, object : InnerPrinterCallback() {
                    override fun onConnected(service: SunmiPrinterService?) {}
                    override fun onDisconnected() {
                        printerService = null
                        connected = false
                    }
                })
            }
        } catch (e: Exception) {
            Log.w(TAG, "unbind error: ${e.message}")
        }
    }

    /**
     * Print wash receipt - full ZATCA format
     */
    fun printWashReceipt(
        workerName: String,
        plateName: String,
        carType: String,
        cost: Double,
        orgName: String,
        isPaid: Boolean = true
    ) {
        val svc = printerService ?: run {
            Log.w(TAG, "Printer not connected")
            return
        }
        try {
            svc.printerInit(null)
            svc.setAlignment(1, null)         // center
            svc.setFontSize(28f, null)
            svc.printText("$orgName\n", null)
            svc.setFontSize(20f, null)
            svc.printText("مغسلة نجم - NJM Car Wash\n", null)
            svc.printText("حفر الباطن - Hafar Al Batin\n", null)
            svc.setAlignment(0, null)         // left
            svc.setFontSize(24f, null)
            svc.printText("--------------------------------\n", null)
            svc.printText("رقم اللوحة: $plateName\n", null)
            svc.printText("نوع السيارة: $carType\n", null)
            if (workerName.isNotBlank()) {
                svc.printText("الموظف: $workerName\n", null)
            }
            svc.printText("المبلغ: ${String.format("%.2f", cost)} ر.س\n", null)
            val vatAmount = cost * 0.15 / 1.15
            val preTax = cost - vatAmount
            svc.printText("المبلغ قبل الضريبة: ${String.format("%.2f", preTax)} ر.س\n", null)
            svc.printText("ضريبة القيمة المضافة 15%: ${String.format("%.2f", vatAmount)} ر.س\n", null)
            svc.printText("الحالة: ${if (isPaid) "مدفوع ✅" else "غير مدفوع ⏳"}\n", null)
            svc.printText("--------------------------------\n", null)
            svc.setAlignment(1, null)
            svc.printText("شكراً لزيارتكم\n", null)
            svc.printText("meshari.tech\n", null)
            svc.printText("\n\n\n", null)
            svc.cutPaper(1, null)
            Log.d(TAG, "Receipt printed OK")
        } catch (e: Exception) {
            Log.e(TAG, "printWashReceipt failed: ${e.message}")
        }
    }

    /**
     * Print ZATCA-compliant invoice
     */
    fun printInvoice(context: Context, invoice: Invoice) {
        val svc = printerService ?: run { Log.w(TAG, "Printer not connected"); return }
        val prefs = context.getSharedPreferences("print_settings", Context.MODE_PRIVATE)
        val orgName = prefs.getString("org_name", "مغسلة نجم") ?: "مغسلة نجم"
        val vatNumber = prefs.getString("vat_number", "") ?: ""
        val crNumber = prefs.getString("cr_number", "") ?: ""
        val address = prefs.getString("address", "حفر الباطن") ?: "حفر الباطن"

        try {
            svc.printerInit(null)
            svc.setAlignment(1, null)
            svc.setFontSize(26f, null)
            svc.printText("فاتورة ضريبية\n", null)
            svc.printText("TAX INVOICE - ZATCA\n", null)
            svc.setFontSize(22f, null)
            svc.printText("$orgName\n", null)
            svc.setAlignment(0, null)
            svc.setFontSize(20f, null)
            svc.printText("--------------------------------\n", null)
            svc.printText("رقم الفاتورة: ${invoice.invoiceNumber}\n", null)
            svc.printText("التاريخ: ${invoice.invoiceDate ?: ""}\n", null)
            if (vatNumber.isNotBlank()) svc.printText("الرقم الضريبي: $vatNumber\n", null)
            if (crNumber.isNotBlank()) svc.printText("السجل التجاري: $crNumber\n", null)
            svc.printText("العنوان: $address\n", null)
            svc.printText("من: ${invoice.periodStart ?: ""} إلى: ${invoice.periodEnd ?: ""}\n", null)
            svc.printText("--------------------------------\n", null)
            svc.printText("عدد الغسيل: ${invoice.totalWashes ?: 0}\n", null)
            svc.printText("المجموع قبل الضريبة: ${String.format("%.2f", invoice.totalAmount ?: 0.0)} ر.س\n", null)
            svc.printText("ضريبة القيمة المضافة 15%: ${String.format("%.2f", invoice.vatAmount ?: 0.0)} ر.س\n", null)
            svc.printText("الإجمالي شامل الضريبة: ${String.format("%.2f", invoice.grandTotal ?: 0.0)} ر.س\n", null)
            svc.printText("--------------------------------\n", null)
            svc.setAlignment(1, null)
            svc.printText("meshari.tech\n\n\n", null)
            svc.cutPaper(1, null)
        } catch (e: Exception) {
            Log.e(TAG, "printInvoice failed: ${e.message}")
        }
    }

    fun printDailyReport(context: Context, washes: List<WashRecord>, date: String) {
        val svc = printerService ?: return
        try {
            svc.printerInit(null)
            svc.setAlignment(1, null)
            svc.setFontSize(24f, null)
            svc.printText("تقرير اليوم\n", null)
            svc.printText("$date\n", null)
            svc.setAlignment(0, null)
            svc.setFontSize(20f, null)
            svc.printText("--------------------------------\n", null)
            val total = washes.sumOf { it.cost ?: 0.0 }
            val paid = washes.filter { (it.isPaid ?: 1) == 1 }.sumOf { it.cost ?: 0.0 }
            val unpaid = total - paid
            svc.printText("عدد الغسيل: ${washes.size}\n", null)
            svc.printText("المجموع: ${String.format("%.2f", total)} ر.س\n", null)
            svc.printText("مدفوع: ${String.format("%.2f", paid)} ر.س\n", null)
            svc.printText("غير مدفوع: ${String.format("%.2f", unpaid)} ر.س\n", null)
            svc.printText("--------------------------------\n", null)
            svc.setAlignment(1, null)
            svc.printText("meshari.tech\n\n\n", null)
            svc.cutPaper(1, null)
        } catch (e: Exception) {
            Log.e(TAG, "printDailyReport failed: ${e.message}")
        }
    }

    fun printMonthlyReport(context: Context, washes: List<WashRecord>, month: String) {
        val svc = printerService ?: return
        try {
            svc.printerInit(null)
            svc.setAlignment(1, null)
            svc.setFontSize(24f, null)
            svc.printText("تقرير الشهر\n", null)
            svc.printText("$month\n", null)
            svc.setAlignment(0, null)
            svc.setFontSize(20f, null)
            svc.printText("--------------------------------\n", null)
            val total = washes.sumOf { it.cost ?: 0.0 }
            val paid = washes.filter { (it.isPaid ?: 1) == 1 }.sumOf { it.cost ?: 0.0 }
            val unpaid = total - paid
            svc.printText("عدد الغسيل: ${washes.size}\n", null)
            svc.printText("المجموع: ${String.format("%.2f", total)} ر.س\n", null)
            svc.printText("مدفوع: ${String.format("%.2f", paid)} ر.س\n", null)
            svc.printText("غير مدفوع: ${String.format("%.2f", unpaid)} ر.س\n", null)
            svc.printText("--------------------------------\n", null)
            svc.setAlignment(1, null)
            svc.printText("meshari.tech\n\n\n", null)
            svc.cutPaper(1, null)
        } catch (e: Exception) {
            Log.e(TAG, "printMonthlyReport failed: ${e.message}")
        }
    }

    fun isAvailable(): Boolean = isConnected()
}
