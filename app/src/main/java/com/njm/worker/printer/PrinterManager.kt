package com.njm.worker.printer

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log

/**
 * PrinterManager for Sunmi V2s - uses AIDL via reflection
  * Supports: woyou.aidlservice.jiuiv5 (Sunmi built-in printer service)
   */
object PrinterManager {
        private const val TAG = "NJM_Printer"

        private var printerService: Any? = null
        private var serviceConnected = false
        private var pendingOnReady: (() -> Unit)? = null

        private val serviceConnection = object : ServiceConnection {
                    override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                                    try {
                                                        val stubClass = Class.forName("woyou.aidlservice.jiuiv5.IWoyouService\$Stub")
                                                                        val asInterface = stubClass.getMethod("asInterface", IBinder::class.java)
                                                                                        printerService = asInterface.invoke(null, binder)
                                                                                                        serviceConnected = true
                                                        Log.d(TAG, "Sunmi printer connected")
                                    } catch (e: Exception) {
                                                        Log.w(TAG, "AIDL not available: " + e.message)
                                                                        serviceConnected = false
                                    }
                                                pendingOnReady?.invoke()
                                                            pendingOnReady = null
                    }
                            override fun onServiceDisconnected(name: ComponentName?) {
                                            printerService = null
                                            serviceConnected = false
                            }
        }

            fun init(context: Context, onReady: () -> Unit) {
                        pendingOnReady = onReady
                        try {
                                        val intent = Intent()
                                                    intent.setPackage("woyou.aidlservice.jiuiv5")
                                                                intent.action = "woyou.aidlservice.jiuiv5.IWoyouService"
                                        val bound = context.applicationContext.bindService(
                                                            intent, serviceConnection, Context.BIND_AUTO_CREATE
                                                        )
                                                    if (!bound) {
                                                                        Log.w(TAG, "Not a Sunmi device or service unavailable")
                                                                                        onReady()
                                                                                                        pendingOnReady = null
                                                    }
                        } catch (e: Exception) {
                                        Log.w(TAG, "Printer init error: " + e.message)
                                                    onReady()
                                                                pendingOnReady = null
                        }
            }

                fun isAvailable(): Boolean = serviceConnected && printerService != null

        fun printWashReceipt(
                    workerName: String,
                    plateName: String,
                    carType: String,
                    cost: Double,
                    orgName: String
                ) {
                    val svc = printerService ?: run {
                                    Log.w(TAG, "Printer not available")
                                                return
                    }
                            try {
                                            val cls = svc.javaClass
                                            fun txt(s: String) {
                                                                try { cls.getMethod("printText", String::class.java, Any::class.java).invoke(svc, s, null) }
                                                                                catch (e: Exception) { Log.w(TAG, "printText err: ${e.message}") }
                                            }
                                                        fun align(a: Int) {
                                                                            try { cls.getMethod("setAlignment", Int::class.java, Any::class.java).invoke(svc, a, null) }
                                                                                            catch (e: Exception) {}
                                                        }
                                                                    fun bold(on: Boolean) {
                                                                                        try { cls.getMethod("setFontName", String::class.java, Any::class.java).invoke(svc, if (on) "bold" else "normal", null) }
                                                                                                        catch (e: Exception) {}
                                                                    }
                                                                                align(1)
                                                                                            txt("$orgName\n")
                                                                                                        txt("------------------------\n")
                                                                                                                    align(0)
                                                                                                                                txt("رقم اللوحة: $plateName\n")
                                                                                                                                            txt("نوع السيارة: $carType\n")
                                                                                                                                                        txt("الموظف: $workerName\n")
                                                                                                                                                                    txt("المبلغ: ${String.format("%.2f", cost)} ر.س\n")
                                                                                                                                                                                txt("------------------------\n")
                                                                                                                                                                                            align(1)
                                                                                                                                                                                                        txt("شكراً لزيارتكم\n\n\n")
                                                                                                                                                                                                                    try { cls.getMethod("cutPaper", Int::class.java, Any::class.java).invoke(svc, 1, null) }
                                                                                                                                                                                                                                catch (e: Exception) {}
                                                                                                                                                                                                                                            Log.d(TAG, "Receipt printed OK")
                                                                                                                                                                                                                                                    } catch (e: Exception) {
                                            Log.e(TAG, "Print failed: ${e.message}")
                            }
        }
    fun isConnected(): Boolean = serviceConnected

    fun unbindService(context: Context) {
        if (serviceConnected) {
            try { context.unbindService(serviceConnection) } catch (e: Exception) {}
            serviceConnected = false
            printerService = null
        }
    }

    fun printInvoice(context: Context, invoice: com.njm.worker.data.model.Invoice) {
        val svc = printerService ?: return
        try {
            val cls = svc.javaClass
            fun txt(s: String) = cls.getMethod("printText", String::class.java, Any::class.java).invoke(svc, s, null)
            fun align(a: Int) = cls.getMethod("setAlignment", Int::class.java, Any::class.java).invoke(svc, a, null)
            align(1); txt("NJM - ZATCA Invoice\n")
            txt("Invoice: " + (invoice.invoiceNumber ?: "") + "\n")
            txt("Date: " + (invoice.invoiceDate ?: "") + "\n")
            txt("Total: " + (invoice.totalAmount ?: 0.0) + " SAR\n")
            txt("VAT 15%: " + (invoice.vatAmount ?: 0.0) + " SAR\n")
            txt("Grand Total: " + (invoice.grandTotal ?: 0.0) + " SAR\n")
            txt("---------------------------\n")
            align(1); txt("meshari.tech\n\n\n")
            try { cls.getMethod("cutPaper", Int::class.java, Any::class.java).invoke(svc, 1, null) } catch (e: Exception) {}
        } catch (e: Exception) { android.util.Log.e(TAG, "Invoice print failed: " + e.message) }
    }

    fun printDailyReport(context: Context, washes: List<com.njm.worker.data.model.WashRecord>, date: String) {
        val svc = printerService ?: return
        try {
            val cls = svc.javaClass
            fun txt(s: String) = cls.getMethod("printText", String::class.java, Any::class.java).invoke(svc, s, null)
            fun align(a: Int) = cls.getMethod("setAlignment", Int::class.java, Any::class.java).invoke(svc, a, null)
            align(1); txt("NJM Daily Report - " + date + "\n")
            val total = washes.sumOf { it.cost ?: 0.0 }
            txt("Count: " + washes.size + "\n")
            txt("Total: " + total + " SAR\n")
            align(1); txt("meshari.tech\n\n\n")
            try { cls.getMethod("cutPaper", Int::class.java, Any::class.java).invoke(svc, 1, null) } catch (e: Exception) {}
        } catch (e: Exception) { android.util.Log.e(TAG, "Daily report failed: " + e.message) }
    }

    fun printMonthlyReport(context: Context, washes: List<com.njm.worker.data.model.WashRecord>, month: String) {
        val svc = printerService ?: return
        try {
            val cls = svc.javaClass
            fun txt(s: String) = cls.getMethod("printText", String::class.java, Any::class.java).invoke(svc, s, null)
            fun align(a: Int) = cls.getMethod("setAlignment", Int::class.java, Any::class.java).invoke(svc, a, null)
            align(1); txt("NJM Monthly Report - " + month + "\n")
            val total = washes.sumOf { it.cost ?: 0.0 }
            txt("Count: " + washes.size + "\n")
            txt("Total: " + total + " SAR\n")
            align(1); txt("meshari.tech\n\n\n")
            try { cls.getMethod("cutPaper", Int::class.java, Any::class.java).invoke(svc, 1, null) } catch (e: Exception) {}
        } catch (e: Exception) { android.util.Log.e(TAG, "Monthly report failed: " + e.message) }
    }

}
