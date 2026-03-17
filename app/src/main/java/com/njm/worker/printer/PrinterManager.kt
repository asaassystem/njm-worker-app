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
        if (bindingInProgress && !forceRebind) return
        bindingInProgress = true
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
                        NjmApp.sunmiPrinterService = service
                        NjmApp.printerConnected = true
                        connected = true
                        bindingInProgress = false
                        Log.d(TAG, "Printer connected via reflection")
                        Handler(Looper.getMainLooper()).post { onReady() }
                    }
                    "onDisconnected" -> {
                        printerService = null
                        NjmApp.sunmiPrinterService = null
                        NjmApp.printerConnected = false
                        connected = false
                        bindingInProgress = false
                        Log.d(TAG, "Printer disconnected")
                    }
                }
                null
            }
            val bindMethod = innerPrinterManagerClass.getMethod("bindService", Context::class.java, callbackClass)
            bindMethod.invoke(manager, context, proxy)
        } catch (e: ClassNotFoundException) {
            Log.w(TAG, "Sunmi printer SDK not available: ${e.message}")
            bindingInProgress = false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to init printer: ${e.message}")
            bindingInProgress = false
        }
    }

    fun isConnected(): Boolean = connected && printerService != null

    fun printWashReceipt(record: WashRecord, onDone: (Boolean) -> Unit = {}) {
        val service = printerService
        if (!isConnected() || service == null) {
            Log.w(TAG, "Printer not connected")
            onDone(false)
            return
        }
        try {
            val serviceClass = service.javaClass
            // Start transaction
            invokeMethod(service, "enterPrinterBuffer", listOf(Boolean::class.java to true))
            // Print logo if available
            printLogoInternal(service)
            // Store header
            setAlign(service, 1) // CENTER
            printTextLine(service, "\n=============================\n")
            printTextLine(service, "مغسلة نجم - NJM Laundry\n")
            printTextLine(service, "=============================\n")
            setAlign(service, 0) // LEFT
            printTextLine(service, "رقم الغسيل: ${record.washId}\n")
            printTextLine(service, "التاريخ: ${record.createdAt}\n")
            printTextLine(service, "المركبة: ${record.carType} - ${record.carNumber}\n")
            printTextLine(service, "نوع الغسيل: ${record.serviceType}\n")
            printTextLine(service, "السعر: ${record.price} ريال\n")
            setAlign(service, 1) // CENTER
            printTextLine(service, "\nشكراً لزيارتكم\n")
            printTextLine(service, "=============================\n\n")
            // Commit transaction
            invokeMethod(service, "exitPrinterBufferWithCallback", listOf(Boolean::class.java to true), ignoreResult = true)
            onDone(true)
        } catch (e: Exception) {
            Log.e(TAG, "Print error: ${e.message}")
            onDone(false)
        }
    }

    fun printInvoice(invoice: Invoice, onDone: (Boolean) -> Unit = {}) {
        val service = printerService
        if (!isConnected() || service == null) {
            Log.w(TAG, "Printer not connected")
            onDone(false)
            return
        }
        try {
            invokeMethod(service, "enterPrinterBuffer", listOf(Boolean::class.java to true))
            printLogoInternal(service)
            setAlign(service, 1)
            printTextLine(service, "\n=============================\n")
            printTextLine(service, "فاتورة - NJM Invoice\n")
            printTextLine(service, "=============================\n")
            setAlign(service, 0)
            printTextLine(service, "رقم الفاتورة: ${invoice.invoiceId}\n")
            printTextLine(service, "التاريخ: ${invoice.createdAt}\n")
            printTextLine(service, "العميل: ${invoice.customerName}\n")
            printTextLine(service, "المبلغ: ${invoice.totalAmount} ريال\n")
            if (!invoice.vatNumber.isNullOrEmpty()) {
                printTextLine(service, "الرقم الضريبي: ${invoice.vatNumber}\n")
            }
            setAlign(service, 1)
            printTextLine(service, "\nشكراً لثقتكم\n")
            printTextLine(service, "=============================\n\n")
            invokeMethod(service, "exitPrinterBufferWithCallback", listOf(Boolean::class.java to true), ignoreResult = true)
            onDone(true)
        } catch (e: Exception) {
            Log.e(TAG, "Print invoice error: ${e.message}")
            onDone(false)
        }
    }

    fun printLogo(context: Context, logoUrl: String, onDone: (Boolean) -> Unit = {}) {
        val service = printerService
        if (!isConnected() || service == null) {
            onDone(false)
            return
        }
        Thread {
            try {
                val url = URL(logoUrl)
                val bitmap = BitmapFactory.decodeStream(url.openStream())
                if (bitmap != null) {
                    printBitmapInternal(service, bitmap)
                    onDone(true)
                } else {
                    onDone(false)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Print logo error: ${e.message}")
                onDone(false)
            }
        }.start()
    }

    private fun printLogoInternal(service: Any) {
        try {
            val context = NjmApp.instance?.applicationContext ?: return
            val drawable = context.resources.getDrawable(
                context.resources.getIdentifier("ic_launcher", "mipmap", context.packageName),
                null
            )
            val bitmap = (drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap ?: return
            printBitmapInternal(service, bitmap)
        } catch (e: Exception) {
            Log.w(TAG, "Logo print skipped: ${e.message}")
        }
    }

    private fun printBitmapInternal(service: Any, bitmap: Bitmap) {
        try {
            invokeMethod(service, "printBitmap", listOf(Bitmap::class.java to bitmap, Int::class.java to 0))
        } catch (e: Exception) {
            Log.w(TAG, "Bitmap print error: ${e.message}")
        }
    }

    private fun printTextLine(service: Any, text: String) {
        try {
            invokeMethod(service, "printText", listOf(String::class.java to text))
        } catch (e: Exception) {
            Log.w(TAG, "printText error: ${e.message}")
        }
    }

    private fun setAlign(service: Any, align: Int) {
        try {
            invokeMethod(service, "setAlignment", listOf(Int::class.java to align))
        } catch (e: Exception) {
            Log.w(TAG, "setAlignment error: ${e.message}")
        }
    }

    private fun invokeMethod(
        obj: Any,
        methodName: String,
        params: List<Pair<Class<*>, Any?>> = emptyList(),
        ignoreResult: Boolean = false
    ): Any? {
        return try {
            val paramTypes = params.map { it.first }.toTypedArray()
            val paramValues = params.map { it.second }.toTypedArray()
            val method = obj.javaClass.getMethod(methodName, *paramTypes)
            method.invoke(obj, *paramValues)
        } catch (e: Exception) {
            if (!ignoreResult) Log.w(TAG, "Method $methodName not found: ${e.message}")
            null
        }
    }
}
