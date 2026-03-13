package com.njm.worker.printer

import android.app.Activity
import android.content.Context
import android.widget.Toast
import com.njm.worker.data.model.CarDetail
import com.njm.worker.data.model.Invoice
import com.njm.worker.data.model.WashRecord
import com.njm.worker.data.model.WashResponse
import com.sunmi.peripheral.printer.InnerPrinterCallback
import com.sunmi.peripheral.printer.InnerPrinterManager
import com.sunmi.peripheral.printer.SunmiPrinterService
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PrintManager {

    private var printerService: SunmiPrinterService? = null
    private var isPrinterBound = false

    fun bindPrinter(context: Context) {
        InnerPrinterManager.getInstance().bindService(context, object : InnerPrinterCallback() {
            override fun onConnected(service: SunmiPrinterService?) {
                printerService = service
                isPrinterBound = true
            }
            override fun onDisconnected() {
                printerService = null
                isPrinterBound = false
            }
        })
    }

    fun unbindPrinter(context: Context) {
        try {
            if (isPrinterBound) {
                InnerPrinterManager.getInstance().unBindService(context, object : InnerPrinterCallback() {
                    override fun onConnected(service: SunmiPrinterService?) {}
                    override fun onDisconnected() {}
                })
            }
        } catch (_: Exception) {}
        isPrinterBound = false
    }

    fun isPrinterAvailable(): Boolean = isPrinterBound && printerService != null

    fun getPrinterStatus(): String {
        if (!isPrinterBound || printerService == null) return "غير متصلة"
        return try {
            when (printerService!!.updatePrinterState()) {
                1 -> "جاهزة للطباعة"
                2 -> "غير جاهزة"
                3 -> "ورق ينفد"
                4 -> "لا يوجد ورق"
                5 -> "درجة حرارة عالية"
                else -> "متصلة"
            }
        } catch (_: Exception) { "خطأ في الاتصال" }
    }

    private fun txt(s: String) = try { printerService?.printText(s) } catch (_: Exception) {}
    private fun center() = try { printerService?.setAlignment(1, null) } catch (_: Exception) {}
    private fun left() = try { printerService?.setAlignment(0, null) } catch (_: Exception) {}
    private fun bold(on: Boolean) = try {
        printerService?.sendRAWData(if (on) byteArrayOf(0x1B, 0x45, 0x01) else byteArrayOf(0x1B, 0x45, 0x00))
    } catch (_: Exception) {}
    private fun bigFont() = try { printerService?.setFontSize(28f, null) } catch (_: Exception) {}
    private fun normalFont() = try { printerService?.setFontSize(22f, null) } catch (_: Exception) {}
    private fun divider() = txt("--------------------------------\n")
    private fun ln() = txt("\n")
    private fun now() = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
    private fun cut() = try { printerService?.paperCut(null) } catch (_: Exception) {}

    private fun printNjmHeader() {
        center()
        bigFont()
        bold(true)
        txt("\nنجم الموقود\n")
        bold(false)
        normalFont()
        txt("NJM Car Wash\n")
        txt("حفر الباطن\n")
        divider()
        txt("Developed by meshari.tech\n")
        divider()
    }

    private fun printFooter() {
        center()
        divider()
        txt("شكراً لاختياركم\n")
        txt("Thank you for choosing us\n")
        txt("\n\n\n")
        cut()
    }

    private fun checkPrinter(activity: Activity): Boolean {
        if (!isPrinterAvailable()) {
            activity.runOnUiThread {
                Toast.makeText(activity, "الطابعة Sunmi غير متصلة", Toast.LENGTH_LONG).show()
            }
            return false
        }
        return true
    }

    fun printWashReceipt(activity: Activity, car: CarDetail, wash: WashResponse, isPaid: Int) {
        if (!checkPrinter(activity)) return
        try {
            printerService?.initPrinter()
            printNjmHeader()
            left()
            bold(true)
            txt("وصل غسيل\n")
            bold(false)
            divider()
            txt("اللوحة: ${car.plateNumber}\n")
            txt("السيارة: ${car.carTypeLabel ?: car.carType ?: "-"}\n")
            txt("المالك: ${car.ownerName ?: "-"}\n")
            txt("الجهة: ${car.orgName ?: "-"}\n")
            divider()
            txt("المبلغ: ${wash.cost ?: car.washPrice ?: 0.0} ر.س\n")
            txt("الدفع: ${if (isPaid == 1) "مدفوع" else "غير مدفوع"}\n")
            wash.invoiceNumber?.let { txt("الفاتورة: $it\n") }
            txt("الوقت: ${now()}\n")
            printFooter()
        } catch (e: Exception) {
            activity.runOnUiThread { Toast.makeText(activity, "خطأ: ${e.message}", Toast.LENGTH_SHORT).show() }
        }
    }

    fun printReceiptForCar(activity: Activity, car: CarDetail) {
        if (!checkPrinter(activity)) return
        try {
            printerService?.initPrinter()
            printNjmHeader()
            left()
            txt("بيانات السيارة\n")
            divider()
            txt("اللوحة: ${car.plateNumber}\n")
            txt("النوع: ${car.carTypeLabel ?: car.carType ?: "-"}\n")
            txt("المالك: ${car.ownerName ?: "-"}\n")
            txt("الهاتف: ${car.ownerPhone ?: "-"}\n")
            txt("سعر الغسيل: ${car.washPrice ?: 0.0} ر.س\n")
            txt("الوقت: ${now()}\n")
            printFooter()
        } catch (e: Exception) {
            activity.runOnUiThread { Toast.makeText(activity, "خطأ: ${e.message}", Toast.LENGTH_SHORT).show() }
        }
    }

    fun printDailyReport(activity: Activity, washes: List<WashRecord>, total: Double, paid: Double, unpaid: Double) {
        if (!checkPrinter(activity)) return
        try {
            printerService?.initPrinter()
            printNjmHeader()
            center()
            bold(true); txt("تقرير اليوم\n"); bold(false)
            txt("${now()}\n")
            divider()
            left()
            txt("العدد: ${washes.size}\n")
            txt("الإجمالي: $total ر.س\n")
            txt("المدفوع: $paid ر.س\n")
            txt("غير مدفوع: $unpaid ر.س\n")
            divider()
            washes.forEachIndexed { i, w ->
                txt("${i+1}. ${w.plateNumber ?: "?"} | ${w.cost ?: 0.0}ر.س | ${if ((w.isPaid ?: 1) == 1) "م" else "غ"}\n")
            }
            printFooter()
        } catch (e: Exception) {
            activity.runOnUiThread { Toast.makeText(activity, "خطأ: ${e.message}", Toast.LENGTH_SHORT).show() }
        }
    }

    fun printMonthlyReport(activity: Activity, washes: List<WashRecord>, total: Double, paid: Double, unpaid: Double) {
        if (!checkPrinter(activity)) return
        try {
            printerService?.initPrinter()
            printNjmHeader()
            center()
            bold(true); txt("تقرير الشهر\n"); bold(false)
            txt("${now()}\n")
            divider()
            left()
            txt("العدد: ${washes.size}\n")
            txt("الإجمالي: $total ر.س\n")
            txt("المدفوع: $paid ر.س\n")
            txt("غير مدفوع: $unpaid ر.س\n")
            divider()
            washes.forEachIndexed { i, w ->
                txt("${i+1}. ${w.plateNumber ?: "?"} | ${w.washDate?.takeLast(5) ?: ""} | ${w.cost ?: 0.0}ر.س\n")
            }
            printFooter()
        } catch (e: Exception) {
            activity.runOnUiThread { Toast.makeText(activity, "خطأ: ${e.message}", Toast.LENGTH_SHORT).show() }
        }
    }

    fun printInvoice(activity: Activity, invoice: Invoice) {
        if (!checkPrinter(activity)) return
        try {
            printerService?.initPrinter()
            center()
            bigFont(); bold(true)
            txt("\nمغسلة نجم الموقود\n")
            bold(false); normalFont()
            txt("NJM Car Wash - Hafr Al Batin\n")
            divider()
            txt("meshari.tech\n")
            divider()
            left()
            bold(true); txt("فاتورة ضريبية مبسطة\n"); bold(false)
            txt("Simplified Tax Invoice (VAT)\n")
            divider()
            txt("رقم الفاتورة: ${invoice.invoiceNumber}\n")
            txt("التاريخ: ${invoice.invoiceDate ?: now()}\n")
            txt("الفترة: ${invoice.periodStart} - ${invoice.periodEnd}\n")
            divider()
            txt("عدد الغسيلات: ${invoice.totalWashes ?: 0}\n")
            txt("الإجمالي (قبل ض.ق.م): ${invoice.totalAmount ?: 0.0} ر.س\n")
            txt("ضريبة القيمة المضافة 15%: ${invoice.vatAmount ?: 0.0} ر.س\n")
            bold(true)
            txt("الإجمالي الكلي: ${invoice.grandTotal ?: 0.0} ر.س\n")
            bold(false)
            divider()
            center()
            txt("[ QR Code ZATCA ]\n")
            txt("رقم ض.ق.م: ${invoice.orgName ?: ""}\n")
            printFooter()
        } catch (e: Exception) {
            activity.runOnUiThread { Toast.makeText(activity, "خطأ: ${e.message}", Toast.LENGTH_SHORT).show() }
        }
    }
}
