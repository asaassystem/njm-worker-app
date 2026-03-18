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
 * v6.0: All Arabic strings converted to Unicode escapes for Kotlin build compatibility
 */
object PrinterManager {
    private const val TAG = "NJM_Printer"
    private var printerService: Any? = null
    private var connected = false
    private var bindingInProgress = false

    fun init(context: Context, forceRebind: Boolean = false, onReady: () -> Unit = {}) {
        val appPrinter = NjmApp.sunmiPrinterService
        if (appPrinter != null && NjmApp.printerConnected) {
            printerService = appPrinter; connected = true; onReady(); return
        }
        if (forceRebind) {
            NjmApp.bindAttempted = false; NjmApp.printerConnected = false
            NjmApp.sunmiPrinterService = null; connected = false; bindingInProgress = false
        }
        if (bindingInProgress && !forceRebind) {
            Handler(Looper.getMainLooper()).postDelayed({
                val p = NjmApp.sunmiPrinterService
                if (p != null) { printerService = p; connected = true; NjmApp.printerConnected = true }
                else { connected = false }
                onReady()
            }, 3000)
            return
        }
        if (NjmApp.bindAttempted && !NjmApp.printerConnected) { NjmApp.bindAttempted = false }
        bindingInProgress = true; NjmApp.bindAttempted = true
        try {
            val innerPrinterManagerClass = Class.forName("com.sunmi.peripheral.printer.InnerPrinterManager")
            val manager = innerPrinterManagerClass.getMethod("getInstance").invoke(null)
            val callbackClass = Class.forName("com.sunmi.peripheral.printer.InnerPrinterCallback")
            val proxy = java.lang.reflect.Proxy.newProxyInstance(
                context.classLoader, arrayOf(callbackClass)
            ) { _, method, args ->
                when (method.name) {
                    "onConnected" -> {
                        val service = args?.get(0)
                        printerService = service; connected = (service != null)
                        NjmApp.sunmiPrinterService = service; NjmApp.printerConnected = connected
                        bindingInProgress = false
                        Log.i(TAG, if (connected) "Sunmi printer connected OK" else "Sunmi service null")
                        Handler(Looper.getMainLooper()).post { onReady() }
                    }
                    "onDisconnected" -> {
                        printerService = null; connected = false
                        NjmApp.sunmiPrinterService = null; NjmApp.printerConnected = false
                        NjmApp.bindAttempted = false; bindingInProgress = false
                        Log.w(TAG, "Sunmi printer disconnected")
                        Handler(Looper.getMainLooper()).post { onReady() }
                    }
                }
                null
            }
            innerPrinterManagerClass.getMethod("bindService", Context::class.java, callbackClass)
                .invoke(manager, context.applicationContext, proxy)
        } catch (e: ClassNotFoundException) {
            Log.w(TAG, "Not a Sunmi device: ${e.message}"); connected = false; bindingInProgress = false; onReady()
        } catch (e: Exception) {
            Log.w(TAG, "Printer bind failed: ${e.message}"); connected = false; bindingInProgress = false; onReady()
        }
    }

    fun isConnected() = connected && printerService != null
    fun isAvailable() = isConnected()

    fun unbindService(context: Context) {
        try {
            val cls = Class.forName("com.sunmi.peripheral.printer.InnerPrinterManager")
            val manager = cls.getMethod("getInstance").invoke(null)
            val cbClass = Class.forName("com.sunmi.peripheral.printer.InnerPrinterCallback")
            val proxy = java.lang.reflect.Proxy.newProxyInstance(context.classLoader, arrayOf(cbClass)) { _, method, _ ->
                if (method.name == "onDisconnected") {
                    printerService = null; connected = false
                    NjmApp.sunmiPrinterService = null; NjmApp.printerConnected = false; NjmApp.bindAttempted = false
                }
                null
            }
            cls.getMethod("unBindService", Context::class.java, cbClass).invoke(manager, context, proxy)
        } catch (e: Exception) { Log.w(TAG, "Unbind: ${e.message}") }
    }

    private fun loadLogoBitmap(): Bitmap? {
        return try {
            val bmp = BitmapFactory.decodeStream(URL("https://njm.company/static/img/logo.png").openStream())
            if (bmp != null) {
                val ratio = 384f / bmp.width.toFloat()
                Bitmap.createScaledBitmap(bmp, 384, (bmp.height * ratio).toInt(), true)
            } else null
        } catch (e: Exception) { Log.w(TAG, "Logo load failed: ${e.message}"); null }
    }

    fun printWashReceipt(
        workerName: String, plateName: String, carType: String, cost: Double,
        orgName: String, isPaid: Boolean = true, washDate: String = "",
        washTime: String = "", invoiceNumber: String = "", context: Context? = null
    ) {
        val svc = printerService
        if (svc == null) {
            if (context != null) {
                init(context, forceRebind = false) {
                    val svc2 = printerService
                    if (svc2 != null) {
                        Thread { val logo = loadLogoBitmap()
                            Handler(Looper.getMainLooper()).post {
                                doActualPrint(svc2, workerName, plateName, carType, cost, orgName, isPaid, washDate, washTime, invoiceNumber, logo)
                            }
                        }.start()
                    }
                }
            }
            return
        }
        Thread { val logo = loadLogoBitmap()
            Handler(Looper.getMainLooper()).post {
                doActualPrint(svc, workerName, plateName, carType, cost, orgName, isPaid, washDate, washTime, invoiceNumber, logo)
            }
        }.start()
    }

    private fun ri(svc: Any) { try { svc.javaClass.getMethod("printerInit", android.os.IInterface::class.java).invoke(svc, null) } catch (e: Exception) { try { svc.javaClass.getMethod("printerInit").invoke(svc) } catch (e2: Exception) {} } }
    private fun ra(svc: Any, a: Int) { try { svc.javaClass.getMethod("setAlignment", Int::class.java, android.os.IInterface::class.java).invoke(svc, a, null) } catch (e: Exception) { try { svc.javaClass.getMethod("setAlignment", Int::class.java).invoke(svc, a) } catch (e2: Exception) {} } }
    private fun rf(svc: Any, s: Float) { try { svc.javaClass.getMethod("setFontSize", Float::class.java, android.os.IInterface::class.java).invoke(svc, s, null) } catch (e: Exception) { try { svc.javaClass.getMethod("setFontSize", Float::class.java).invoke(svc, s) } catch (e2: Exception) {} } }
    private fun rt(svc: Any, t: String) { try { svc.javaClass.getMethod("printText", String::class.java, android.os.IInterface::class.java).invoke(svc, t, null) } catch (e: Exception) { try { svc.javaClass.getMethod("printText", String::class.java).invoke(svc, t) } catch (e2: Exception) {} } }
    private fun rb(svc: Any, bmp: Bitmap) { try { svc.javaClass.getMethod("printBitmap", Bitmap::class.java, android.os.IInterface::class.java).invoke(svc, bmp, null) } catch (e: Exception) { try { svc.javaClass.getMethod("printBitmap", Bitmap::class.java).invoke(svc, bmp) } catch (e2: Exception) {} } }
    private fun rw(svc: Any, n: Int) { try { svc.javaClass.getMethod("lineWrap", Int::class.java, android.os.IInterface::class.java).invoke(svc, n, null) } catch (e: Exception) { try { svc.javaClass.getMethod("lineWrap", Int::class.java).invoke(svc, n) } catch (e2: Exception) {} } }
    private fun rc(svc: Any) { try { svc.javaClass.getMethod("cutPaper", android.os.IInterface::class.java).invoke(svc, null) } catch (e: Exception) { try { svc.javaClass.getMethod("cutPaper").invoke(svc) } catch (e2: Exception) {} } }

    private fun doActualPrint(svc: Any, workerName: String, plateName: String, carType: String,
        cost: Double, orgName: String, isPaid: Boolean, washDate: String, washTime: String,
        invoiceNumber: String, logo: Bitmap? = null) {
        try {
            ri(svc)
            if (logo != null) { ra(svc, 1); rb(svc, logo); rw(svc, 1) }
            ra(svc, 1); rf(svc, 28f); rt(svc, "$orgName \n")
            rf(svc, 20f); rt(svc, "\u0646\u0638\u0627\u0645 \u0625\u062f\u0627\u0631\u0629 \u063a\u0633\u064a\u0644 \u0627\u0644\u0633\u064a\u0627\u0631\u0627\u062a - NJM \n")
            rf(svc, 18f); rt(svc, "\u062d\u0641\u0631 \u0627\u0644\u0628\u0627\u0637\u0646 - \u0627\u0644\u0633\u0639\u0648\u062f\u064a\u0629 \n")
            ra(svc, 0); rf(svc, 19f); rt(svc, "================================ \n")
            val prefs = try { NjmApp.instance.getSharedPreferences("print_settings", Context.MODE_PRIVATE) } catch (e: Exception) { null }
            val vatNum = prefs?.getString("vat_number", "") ?: ""
            val crNum = prefs?.getString("cr_number", "") ?: ""
            val addr = prefs?.getString("address", "\u062d\u0641\u0631 \u0627\u0644\u0628\u0627\u0637\u0646") ?: "\u062d\u0641\u0631 \u0627\u0644\u0628\u0627\u0637\u0646"
            val invNum = if (invoiceNumber.isNotBlank()) invoiceNumber else "NJM-${System.currentTimeMillis() % 1000000}"
            rt(svc, "\u0641\u0627\u062a\u0648\u0631\u0629 \u0636\u0631\u064a\u0628\u064a\u0629 \u0645\u0628\u0633\u0637\u0629 \n")
            rt(svc, "\u0631\u0642\u0645 \u0627\u0644\u0641\u0627\u062a\u0648\u0631\u0629 : $invNum \n")
            val ds = if (washDate.isNotBlank()) washDate else java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
            val ts = if (washTime.isNotBlank()) washTime else java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US).format(java.util.Date())
            rt(svc, "\u0627\u0644\u062a\u0627\u0631\u064a\u062e : $ds \n"); rt(svc, "\u0627\u0644\u0648\u0642\u062a : $ts \n")
            if (vatNum.isNotBlank()) rt(svc, "\u0627\u0644\u0631\u0642\u0645 \u0627\u0644\u0636\u0631\u064a\u0628\u064a : $vatNum \n")
            if (crNum.isNotBlank()) rt(svc, "\u0627\u0644\u0633\u062c\u0644 \u0627\u0644\u062a\u062c\u0627\u0631\u064a : $crNum \n")
            rt(svc, "\u0627\u0644\u0639\u0646\u0648\u0627\u0646 : $addr \n"); rt(svc, "================================ \n")
            rt(svc, "\u0628\u064a\u0627\u0646\u0627\u062a \u0627\u0644\u0645\u0631\u0643\u0628\u0629 \u0648\u0627\u0644\u062e\u062f\u0645\u0629 \n")
            rt(svc, "-------------------------------- \n")
            rt(svc, "\u0631\u0642\u0645 \u0627\u0644\u0644\u0648\u062d\u0629 : $plateName \n")
            rt(svc, "\u0646\u0648\u0639 \u0627\u0644\u0645\u0631\u0643\u0628\u0629 : $carType \n")
            rt(svc, "\u0627\u0644\u0645\u0646\u0634\u0623\u0629 : $orgName \n")
            if (workerName.isNotBlank()) rt(svc, "\u0627\u0644\u0645\u0648\u0638\u0641 : $workerName \n")
            rt(svc, "\u0646\u0648\u0639 \u0627\u0644\u062e\u062f\u0645\u0629 : \u063a\u0633\u064a\u0644 \u0633\u064a\u0627\u0631\u0629 \n")
            rt(svc, "================================ \n"); rt(svc, "\u062a\u0641\u0627\u0635\u064a\u0644 \u0627\u0644\u0645\u0628\u0644\u063a \n")
            rt(svc, "-------------------------------- \n")
            val vat = cost * 15.0 / 115.0; val sub = cost - vat
            rt(svc, "\u0627\u0644\u0645\u0628\u0644\u063a \u0642\u0628\u0644 \u0627\u0644\u0636\u0631\u064a\u0628\u0629 : ${String.format("%.2f", sub)} \u0631.\u0633 \n")
            rt(svc, "\u0636\u0631\u064a\u0628\u0629 \u0627\u0644\u0642\u064a\u0645\u0629 \u0627\u0644\u0645\u0636\u0627\u0641\u0629 \n")
            rt(svc, " \u0646\u0633\u0628\u0629 \u0627\u0644\u0636\u0631\u064a\u0628\u0629 15%% : ${String.format("%.2f", vat)} \u0631.\u0633 \n")
            rt(svc, "================================ \n"); rf(svc, 24f)
            rt(svc, "\u0627\u0644\u0625\u062c\u0645\u0627\u0644\u064a \u0634\u0627\u0645\u0644 \u0627\u0644\u0636\u0631\u064a\u0628\u0629 \n")
            rt(svc, "${String.format("%.2f", cost)} \u0631\u064a\u0627\u0644 \u0633\u0639\u0648\u062f\u064a \n")
            rf(svc, 19f); rt(svc, "================================ \n")
            rt(svc, "\u062d\u0627\u0644\u0629 \u0627\u0644\u062f\u0641\u0639 : ${if (isPaid) "\u0645\u062f\u0641\u0648\u0639" else "\u063a\u064a\u0631 \u0645\u062f\u0641\u0648\u0639"} \n")
            rt(svc, "================================ \n"); ra(svc, 1); rf(svc, 18f)
            rt(svc, "\u0634\u0643\u0631\u0627\u064b \u0644\u062a\u0639\u0627\u0645\u0644\u0643\u0645 \u0645\u0639\u0646\u0627 \n")
            rt(svc, "NJM Car Wash Management System \n"); rt(svc, "www.njm.company \n")
            rw(svc, 3); rc(svc)
            Log.i(TAG, "Print OK: plate=$plateName inv=$invNum")
        } catch (e: Exception) { Log.e(TAG, "doActualPrint error: ${e.message}") }
    }

    fun printDailyReport(context: Context, washes: List<WashRecord>, date: String,
        totalOverride: Double = -1.0, paidOverride: Double = -1.0, unpaidOverride: Double = -1.0) {
        val svc = printerService ?: return
        val total = if (totalOverride >= 0) totalOverride else washes.sumOf { it.cost ?: 0.0 }
        val paid = if (paidOverride >= 0) paidOverride else washes.filter { (it.isPaid ?: 1) == 1 }.sumOf { it.cost ?: 0.0 }
        val unpaid = if (unpaidOverride >= 0) unpaidOverride else total - paid
        try {
            ri(svc); ra(svc, 1); rf(svc, 26f)
            rt(svc, "\u062a\u0642\u0631\u064a\u0631 \u0627\u0644\u064a\u0648\u0645 $date \n")
            ra(svc, 0); rf(svc, 20f); rt(svc, "================================ \n")
            rt(svc, "\u0639\u062f\u062f \u0627\u0644\u063a\u0633\u064a\u0644 : ${washes.size} \n")
            rt(svc, "\u0627\u0644\u0625\u062c\u0645\u0627\u0644\u064a : ${String.format("%.2f", total)} \u0631.\u0633 \n")
            rt(svc, "\u0627\u0644\u0645\u062f\u0641\u0648\u0639 : ${String.format("%.2f", paid)} \u0631.\u0633 \n")
            rt(svc, "\u063a\u064a\u0631 \u0627\u0644\u0645\u062f\u0641\u0648\u0639: ${String.format("%.2f", unpaid)} \u0631.\u0633 \n")
            rt(svc, "================================ \n"); ra(svc, 1)
            rt(svc, "\u0646\u0638\u0627\u0645 \u0646\u062c\u0645 \n"); rc(svc)
        } catch (e: Exception) { Log.e(TAG, "printDailyReport: ${e.message}") }
    }

    fun printMonthlyReport(context: Context, washes: List<WashRecord>, month: String,
        totalOverride: Double = -1.0, paidOverride: Double = -1.0, unpaidOverride: Double = -1.0) {
        val svc = printerService ?: return
        val total = if (totalOverride >= 0) totalOverride else washes.sumOf { it.cost ?: 0.0 }
        val paid = if (paidOverride >= 0) paidOverride else washes.filter { (it.isPaid ?: 1) == 1 }.sumOf { it.cost ?: 0.0 }
        val unpaid = if (unpaidOverride >= 0) unpaidOverride else total - paid
        try {
            ri(svc); ra(svc, 1); rf(svc, 26f)
            rt(svc, "\u062a\u0642\u0631\u064a\u0631 \u0627\u0644\u0634\u0647\u0631 $month \n")
            ra(svc, 0); rf(svc, 20f); rt(svc, "================================ \n")
            rt(svc, "\u0639\u062f\u062f \u0627\u0644\u063a\u0633\u064a\u0644 : ${washes.size} \n")
            rt(svc, "\u0627\u0644\u0625\u062c\u0645\u0627\u0644\u064a : ${String.format("%.2f", total)} \u0631.\u0633 \n")
            rt(svc, "\u0627\u0644\u0645\u062f\u0641\u0648\u0639 : ${String.format("%.2f", paid)} \u0631.\u0633 \n")
            rt(svc, "\u063a\u064a\u0631 \u0627\u0644\u0645\u062f\u0641\u0648\u0639: ${String.format("%.2f", unpaid)} \u0631.\u0633 \n")
            rt(svc, "================================ \n"); ra(svc, 1)
            rt(svc, "\u0646\u0638\u0627\u0645 \u0646\u062c\u0645 \n"); rc(svc)
        } catch (e: Exception) { Log.e(TAG, "printMonthlyReport: ${e.message}") }
    }

    fun printInvoice(context: Context, invoice: Invoice) {
        val svc = printerService ?: return
        val prefs = context.getSharedPreferences("print_settings", Context.MODE_PRIVATE)
        val orgName = prefs.getString("org_name", "\u0645\u063a\u0633\u0644\u0629 \u0646\u062c\u0645") ?: "\u0645\u063a\u0633\u0644\u0629 \u0646\u062c\u0645"
        val vatNum = prefs.getString("vat_number", "") ?: ""
        val crNum = prefs.getString("cr_number", "") ?: ""
        val addr = prefs.getString("address", "\u062d\u0641\u0631 \u0627\u0644\u0628\u0627\u0637\u0646") ?: "\u062d\u0641\u0631 \u0627\u0644\u0628\u0627\u0637\u0646"
        Thread {
            val logo = loadLogoBitmap()
            Handler(Looper.getMainLooper()).post {
                try {
                    ri(svc)
                    if (logo != null) { ra(svc, 1); rb(svc, logo); rw(svc, 1) }
                    ra(svc, 1); rf(svc, 26f)
                    rt(svc, "\u0641\u0627\u062a\u0648\u0631\u0629 \u0636\u0631\u064a\u0628\u064a\u0629 - ZATCA $orgName \n")
                    ra(svc, 0); rf(svc, 20f); rt(svc, "================================ \n")
                    rt(svc, "\u0631\u0642\u0645 \u0627\u0644\u0641\u0627\u062a\u0648\u0631\u0629 : ${invoice.invoiceNumber} \n")
                    rt(svc, "\u0627\u0644\u062a\u0627\u0631\u064a\u062e : ${invoice.invoiceDate ?: ""} \n")
                    if (vatNum.isNotBlank()) rt(svc, "\u0627\u0644\u0631\u0642\u0645 \u0627\u0644\u0636\u0631\u064a\u0628\u064a : $vatNum \n")
                    if (crNum.isNotBlank()) rt(svc, "\u0627\u0644\u0633\u062c\u0644 \u0627\u0644\u062a\u062c\u0627\u0631\u064a : $crNum \n")
                    rt(svc, "\u0627\u0644\u0639\u0646\u0648\u0627\u0646 : $addr \n"); rt(svc, "================================ \n")
                    rt(svc, "\u0627\u0644\u0645\u0646\u0634\u0623\u0629 : ${invoice.orgName ?: ""} \n")
                    rt(svc, "\u0627\u0644\u0641\u062a\u0631\u0629 : ${invoice.periodStart ?: ""} - ${invoice.periodEnd ?: ""} \n")
                    rt(svc, "\u0639\u062f\u062f \u0627\u0644\u063a\u0633\u064a\u0644 : ${invoice.totalWashes ?: 0} \n")
                    rt(svc, "================================ \n")
                    rt(svc, "\u0627\u0644\u0645\u0628\u0644\u063a \u0627\u0644\u0625\u062c\u0645\u0627\u0644\u064a: ${String.format("%.2f", invoice.totalAmount ?: 0.0)} \u0631.\u0633 \n")
                    rt(svc, "\u0636\u0631\u064a\u0628\u0629 15%% : ${String.format("%.2f", invoice.vatAmount ?: 0.0)} \u0631.\u0633 \n")
                    rf(svc, 24f); rt(svc, "\u0627\u0644\u0645\u062c\u0645\u0648\u0639 \u0627\u0644\u0643\u0644\u064a : ${String.format("%.2f", invoice.grandTotal ?: 0.0)} \u0631.\u0633 \n")
                    rf(svc, 20f); rt(svc, "================================ \n"); ra(svc, 1)
                    rt(svc, "\u0646\u0638\u0627\u0645 \u0646\u062c\u0645 - NJM www.njm.company \n")
                    rc(svc)
                } catch (e: Exception) { Log.e(TAG, "printInvoice: ${e.message}") }
            }
        }.start()
    }
}
