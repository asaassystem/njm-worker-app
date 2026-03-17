package com.njm.worker.printer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.njm.worker.NjmApp
import com.njm.worker.data.model.Invoice
import com.njm.worker.data.model.WashRecord
import java.net.URL

/**
 * PrinterManager - Sunmi Built-In Printer via Reflection (No SDK dependency)
 * ZATCA-Compliant Receipt with NJM Logo at top
 * Developer: meshari.tech
 * v5.1: Removed direct Sunmi SDK imports — uses java.lang.reflect
 */
object PrinterManager {
    private const val TAG = "NJM_Printer"
    private var printerService: Any? = null
    private var connected = false
    private var bindingInProgress = false

    fun init(context: Context, forceRebind: Boolean = false, onReady: () -> Unit = {}) {
        val appPrinter = NjmApp.sunmiPrinterService
        if (appPrinter != null && NjmApp.printerConnected) {
            printerService = appPrinter
            connected = true
            onReady()
            return
        }
        if (forceRebind) {
            NjmApp.bindAttempted = false
            NjmApp.printerConnected = false
            NjmApp.sunmiPrinterService = null
            connected = false
            bindingInProgress = false
        }
        if (bindingInProgress && !forceRebind) {
            Handler(Looper.getMainLooper()).postDelayed({
                val p = NjmApp.sunmiPrinterService
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
        if (NjmApp.bindAttempted && !NjmApp.printerConnected) {
            NjmApp.bindAttempted = false
        }
        bindingInProgress = true
        NjmApp.bindAttempted = true
        try {
            val innerPrinterManagerClass = Class.forName("com.sunmi.peripheral.printer.InnerPrinterManager")
            val getInstance = innerPrinterManagerClass.getMethod("getInstance")
            val manager = getInstance.invoke(null)
            val callbackClass = Class.forName("com.sunmi.peripheral.printer.InnerPrinterCallback")
            val proxy = java.lang.reflect.Proxy.newProxyInstance(
                context.classLoader,
                arrayOf(callbackClass)
            ) { _, method, args ->
                when (method.name) {
                    "onConnected" -> {
                        val service = args?.get(0)
                        printerService = service
                        connected = (service != null)
                        NjmApp.sunmiPrinterService = service
                        NjmApp.printerConnected = connected
                        bindingInProgress = false
                        Log.i(TAG, if (connected) "Sunmi printer connected OK" else "Sunmi service null")
                        Handler(Looper.getMainLooper()).post { onReady() }
                    }
                    "onDisconnected" -> {
                        printerService = null
                        connected = false
                        NjmApp.sunmiPrinterService = null
                        NjmApp.printerConnected = false
                        NjmApp.bindAttempted = false
                        bindingInProgress = false
                        Log.w(TAG, "Sunmi printer disconnected")
                        Handler(Looper.getMainLooper()).post { onReady() }
                    }
                }
                null
            }
            val bindMethod = innerPrinterManagerClass.getMethod("bindService", Context::class.java, callbackClass)
            bindMethod.invoke(manager, context.applicationContext, proxy)
        } catch (e: ClassNotFoundException) {
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
            val innerPrinterManagerClass = Class.forName("com.sunmi.peripheral.printer.InnerPrinterManager")
            val getInstance = innerPrinterManagerClass.getMethod("getInstance")
            val manager = getInstance.invoke(null)
            val callbackClass = Class.forName("com.sunmi.peripheral.printer.InnerPrinterCallback")
            val proxy = java.lang.reflect.Proxy.newProxyInstance(
                context.classLoader,
                arrayOf(callbackClass)
            ) { _, method, _ ->
                if (method.name == "onDisconnected") {
                    printerService = null
                    connected = false
                    NjmApp.sunmiPrinterService = null
                    NjmApp.printerConnected = false
                    NjmApp.bindAttempted = false
                }
                null
            }
            val unBindMethod = innerPrinterManagerClass.getMethod("unBindService", Context::class.java, callbackClass)
            unBindMethod.invoke(manager, context, proxy)
        } catch (e: Exception) {
            Log.w(TAG, "Unbind: ${e.message}")
        }
    }

    /** Load NJM logo bitmap from URL (background thread only) */
    private fun loadLogoBitmap(): Bitmap? {
        return try {
            val url = URL("https://njm.company/static/img/logo.png")
            val bmp = BitmapFactory.decodeStream(url.openStream())
            if (bmp != null) {
                val targetW = 384
                val ratio = targetW.toFloat() / bmp.width.toFloat()
                val targetH = (bmp.height * ratio).toInt()
                Bitmap.createScaledBitmap(bmp, targetW, targetH, true)
            } else null
        } catch (e: Exception) {
            Log.w(TAG, "Logo load failed: ${e.message}")
            null
        }
    }

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
            Log.w(TAG, "printWashReceipt: printer not connected")
            if (context != null) {
                init(context, forceRebind = false) {
                    val svc2 = printerService
                    if (svc2 != null) {
                        Thread {
                            val logo = loadLogoBitmap()
                            Handler(Looper.getMainLooper()).post {
                                doActualPrint(svc2, workerName, plateName, carType, cost,
                                    orgName, isPaid, washDate, washTime, invoiceNumber, logo)
                            }
                        }.start()
                    }
                }
            }
            return
        }
        Thread {
            val logo = loadLogoBitmap()
            Handler(Looper.getMainLooper()).post {
                doActualPrint(svc, workerName, plateName, carType, cost,
                    orgName, isPaid, washDate, washTime, invoiceNumber, logo)
            }
        }.start()
    }

    private fun reflectPrinterInit(svc: Any) {
        try { svc.javaClass.getMethod("printerInit", android.os.IInterface::class.java).invoke(svc, null) } catch (e: Exception) {
            try { svc.javaClass.getMethod("printerInit").invoke(svc) } catch (e2: Exception) { /* ignore */ }
        }
    }

    private fun reflectSetAlignment(svc: Any, align: Int) {
        try { svc.javaClass.getMethod("setAlignment", Int::class.java, android.os.IInterface::class.java).invoke(svc, align, null) } catch (e: Exception) {
            try { svc.javaClass.getMethod("setAlignment", Int::class.java).invoke(svc, align) } catch (e2: Exception) { /* ignore */ }
        }
    }

    private fun reflectSetFontSize(svc: Any, size: Float) {
        try { svc.javaClass.getMethod("setFontSize", Float::class.java, android.os.IInterface::class.java).invoke(svc, size, null) } catch (e: Exception) {
            try { svc.javaClass.getMethod("setFontSize", Float::class.java).invoke(svc, size) } catch (e2: Exception) { /* ignore */ }
        }
    }

    private fun reflectPrintText(svc: Any, text: String) {
        try { svc.javaClass.getMethod("printText", String::class.java, android.os.IInterface::class.java).invoke(svc, text, null) } catch (e: Exception) {
            try { svc.javaClass.getMethod("printText", String::class.java).invoke(svc, text) } catch (e2: Exception) { /* ignore */ }
        }
    }

    private fun reflectPrintBitmap(svc: Any, bitmap: Bitmap) {
        try { svc.javaClass.getMethod("printBitmap", Bitmap::class.java, android.os.IInterface::class.java).invoke(svc, bitmap, null) } catch (e: Exception) {
            try { svc.javaClass.getMethod("printBitmap", Bitmap::class.java).invoke(svc, bitmap) } catch (e2: Exception) { /* ignore */ }
        }
    }

    private fun reflectLineWrap(svc: Any, lines: Int) {
        try { svc.javaClass.getMethod("lineWrap", Int::class.java, android.os.IInterface::class.java).invoke(svc, lines, null) } catch (e: Exception) {
            try { svc.javaClass.getMethod("lineWrap", Int::class.java).invoke(svc, lines) } catch (e2: Exception) { /* ignore */ }
        }
    }

    private fun reflectCutPaper(svc: Any) {
        try { svc.javaClass.getMethod("cutPaper", android.os.IInterface::class.java).invoke(svc, null) } catch (e: Exception) {
            try { svc.javaClass.getMethod("cutPaper").invoke(svc) } catch (e2: Exception) { /* ignore */ }
        }
    }

    private fun doActualPrint(
        svc: Any,
        workerName: String,
        plateName: String,
        carType: String,
        cost: Double,
        orgName: String,
        isPaid: Boolean,
        washDate: String,
        washTime: String,
        invoiceNumber: String,
        logo: Bitmap? = null
    ) {
        try {
            reflectPrinterInit(svc)

            // ===== LOGO AT TOP =====
            if (logo != null) {
                reflectSetAlignment(svc, 1)
                reflectPrintBitmap(svc, logo)
                reflectLineWrap(svc, 1)
            }

            // ===== HEADER =====
            reflectSetAlignment(svc, 1)
            reflectSetFontSize(svc, 28f)
            reflectPrintText(svc, "$orgName
")
            reflectSetFontSize(svc, 20f)
            reflectPrintText(svc, "نظام إدارة غسيل السيارات - NJM
")
            reflectSetFontSize(svc, 18f)
            reflectPrintText(svc, "حفر الباطن - المملكة العربية السعودية
")
            reflectSetAlignment(svc, 0)
            reflectSetFontSize(svc, 19f)
            reflectPrintText(svc, "================================
")

            // ===== ZATCA INVOICE INFO =====
            val prefs = try { NjmApp.instance.getSharedPreferences("print_settings", Context.MODE_PRIVATE) } catch (e: Exception) { null }
            val vatNumber = prefs?.getString("vat_number", "") ?: ""
            val crNumber = prefs?.getString("cr_number", "") ?: ""
            val address = prefs?.getString("address", "حفر الباطن") ?: "حفر الباطن"

            val finalInvNum = if (invoiceNumber.isNotBlank()) invoiceNumber
            else "NJM-${System.currentTimeMillis() % 1000000}"
            reflectPrintText(svc, "فاتورة ضريبية مبسطة
")
            reflectPrintText(svc, "رقم الفاتورة : $finalInvNum
")

            val dateStr = if (washDate.isNotBlank()) washDate
            else java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
            val timeStr = if (washTime.isNotBlank()) washTime
            else java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US).format(java.util.Date())
            reflectPrintText(svc, "التاريخ : $dateStr
")
            reflectPrintText(svc, "الوقت : $timeStr
")

            if (vatNumber.isNotBlank()) reflectPrintText(svc, "الرقم الضريبي : $vatNumber
")
            if (crNumber.isNotBlank()) reflectPrintText(svc, "السجل التجاري : $crNumber
")
            reflectPrintText(svc, "العنوان : $address
")
            reflectPrintText(svc, "================================
")

            // ===== VEHICLE & SERVICE =====
            reflectPrintText(svc, "بيانات المركبة والخدمة
")
            reflectPrintText(svc, "--------------------------------
")
            reflectPrintText(svc, "رقم اللوحة : $plateName
")
            reflectPrintText(svc, "نوع المركبة : $carType
")
            reflectPrintText(svc, "المنشأة : $orgName
")
            if (workerName.isNotBlank()) reflectPrintText(svc, "الموظف : $workerName
")
            reflectPrintText(svc, "نوع الخدمة : غسيل سيارة
")
            reflectPrintText(svc, "================================
")

            // ===== PRICE BREAKDOWN (ZATCA) =====
            reflectPrintText(svc, "تفاصيل المبلغ
")
            reflectPrintText(svc, "--------------------------------
")
            val vat = cost * 15.0 / 115.0
            val subtotal = cost - vat
            reflectPrintText(svc, "المبلغ قبل الضريبة : ${String.format("%.2f", subtotal)} ر.س
")
            reflectPrintText(svc, "ضريبة القيمة المضافة
")
            reflectPrintText(svc, " نسبة الضريبة 15%% : ${String.format("%.2f", vat)} ر.س
")
            reflectPrintText(svc, "================================
")
            reflectSetFontSize(svc, 24f)
            reflectPrintText(svc, "الإجمالي شامل الضريبة
")
            reflectPrintText(svc, "${String.format("%.2f", cost)} ريال سعودي
")
            reflectSetFontSize(svc, 19f)
            reflectPrintText(svc, "================================
")
            reflectPrintText(svc, "حالة الدفع : ${if (isPaid) "مدفوع" else "غير مدفوع"}
")
            reflectPrintText(svc, "================================
")

            // ===== FOOTER =====
            reflectSetAlignment(svc, 1)
            reflectSetFontSize(svc, 18f)
            reflectPrintText(svc, "شكراً لتعاملكم معنا
")
            reflectPrintText(svc, "NJM Car Wash Management System
")
            reflectPrintText(svc, "www.njm.company
")
            reflectLineWrap(svc, 3)
            reflectCutPaper(svc)

            Log.i(TAG, "ZATCA Print OK: plate=$plateName inv=$finalInvNum")
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
            reflectPrinterInit(svc)
            reflectSetAlignment(svc, 1)
            reflectSetFontSize(svc, 26f)
            reflectPrintText(svc, "تقرير اليوم
$date
")
            reflectSetAlignment(svc, 0)
            reflectSetFontSize(svc, 20f)
            reflectPrintText(svc, "================================
")
            reflectPrintText(svc, "عدد الغسيل : ${washes.size}
")
            reflectPrintText(svc, "الإجمالي : ${String.format("%.2f", total)} ر.س
")
            reflectPrintText(svc, "المدفوع : ${String.format("%.2f", paid)} ر.س
")
            reflectPrintText(svc, "غير المدفوع: ${String.format("%.2f", unpaid)} ر.س
")
            reflectPrintText(svc, "================================
")
            reflectSetAlignment(svc, 1)
            reflectPrintText(svc, "نظام نجم


")
            reflectCutPaper(svc)
        } catch (e: Exception) {
            Log.e(TAG, "printDailyReport: ${e.message}")
        }
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
            reflectPrinterInit(svc)
            reflectSetAlignment(svc, 1)
            reflectSetFontSize(svc, 26f)
            reflectPrintText(svc, "تقرير الشهر
$month
")
            reflectSetAlignment(svc, 0)
            reflectSetFontSize(svc, 20f)
            reflectPrintText(svc, "================================
")
            reflectPrintText(svc, "عدد الغسيل : ${washes.size}
")
            reflectPrintText(svc, "الإجمالي : ${String.format("%.2f", total)} ر.س
")
            reflectPrintText(svc, "المدفوع : ${String.format("%.2f", paid)} ر.س
")
            reflectPrintText(svc, "غير المدفوع: ${String.format("%.2f", unpaid)} ر.س
")
            reflectPrintText(svc, "================================
")
            reflectSetAlignment(svc, 1)
            reflectPrintText(svc, "نظام نجم


")
            reflectCutPaper(svc)
        } catch (e: Exception) {
            Log.e(TAG, "printMonthlyReport: ${e.message}")
        }
    }

    fun printInvoice(context: Context, invoice: Invoice) {
        val svc = printerService ?: return
        val prefs = context.getSharedPreferences("print_settings", Context.MODE_PRIVATE)
        val orgName = prefs.getString("org_name", "مغسلة نجم") ?: "مغسلة نجم"
        val vatNumber = prefs.getString("vat_number", "") ?: ""
        val crNumber = prefs.getString("cr_number", "") ?: ""
        val address = prefs.getString("address", "حفر الباطن") ?: "حفر الباطن"
        Thread {
            val logo = loadLogoBitmap()
            Handler(Looper.getMainLooper()).post {
                try {
                    reflectPrinterInit(svc)
                    if (logo != null) {
                        reflectSetAlignment(svc, 1)
                        reflectPrintBitmap(svc, logo)
                        reflectLineWrap(svc, 1)
                    }
                    reflectSetAlignment(svc, 1)
                    reflectSetFontSize(svc, 26f)
                    reflectPrintText(svc, "فاتورة ضريبية - ZATCA
$orgName
")
                    reflectSetAlignment(svc, 0)
                    reflectSetFontSize(svc, 20f)
                    reflectPrintText(svc, "================================
")
                    reflectPrintText(svc, "رقم الفاتورة : ${invoice.invoiceNumber}
")
                    reflectPrintText(svc, "التاريخ : ${invoice.invoiceDate ?: ""}
")
                    if (vatNumber.isNotBlank()) reflectPrintText(svc, "الرقم الضريبي : $vatNumber
")
                    if (crNumber.isNotBlank()) reflectPrintText(svc, "السجل التجاري : $crNumber
")
                    reflectPrintText(svc, "العنوان : $address
")
                    reflectPrintText(svc, "================================
")
                    reflectPrintText(svc, "المنشأة : ${invoice.orgName ?: ""}
")
                    reflectPrintText(svc, "الفترة : ${invoice.periodStart ?: ""} - ${invoice.periodEnd ?: ""}
")
                    reflectPrintText(svc, "عدد الغسيل : ${invoice.totalWashes ?: 0}
")
                    reflectPrintText(svc, "================================
")
                    reflectPrintText(svc, "المبلغ الإجمالي: ${String.format("%.2f", invoice.totalAmount ?: 0.0)} ر.س
")
                    reflectPrintText(svc, "ضريبة 15%% : ${String.format("%.2f", invoice.vatAmount ?: 0.0)} ر.س
")
                    reflectSetFontSize(svc, 24f)
                    reflectPrintText(svc, "المجموع الكلي : ${String.format("%.2f", invoice.grandTotal ?: 0.0)} ر.س
")
                    reflectSetFontSize(svc, 20f)
                    reflectPrintText(svc, "================================
")
                    reflectSetAlignment(svc, 1)
                    reflectPrintText(svc, "نظام نجم - NJM
www.njm.company


")
                    reflectCutPaper(svc)
                } catch (e: Exception) {
                    Log.e(TAG, "printInvoice: ${e.message}")
                }
            }
        }.start()
    }
}
