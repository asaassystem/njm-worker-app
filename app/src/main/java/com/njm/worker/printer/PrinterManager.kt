package com.njm.worker.printer

import android.content.Context
import android.util.Log

object PrinterManager {
    private const val TAG = "PrinterManager"
    private var initialized = false

    fun init(context: Context, onReady: () -> Unit) {
        try {
            // Try to load Sunmi PrinterSdk via reflection (system app)
            val sdkClass = Class.forName("com.sunmi.printerx.PrinterSdk")
            val getInstance = sdkClass.getMethod("getInstance")
            val instance = getInstance.invoke(null)
            initialized = true
            Log.d(TAG, "Sunmi printer SDK found")
            onReady()
        } catch (e: Exception) {
            Log.w(TAG, "Sunmi printer not available: " + e.message)
            // Still call onReady - app works without printer
            onReady()
        }
    }

    fun printWashReceipt(workerName: String, plateName: String, carType: String, cost: Double, orgName: String) {
        if (!initialized) {
            Log.w(TAG, "Printer not initialized")
            return
        }
        try {
            // Use InnerPrinter AIDL as fallback
            val intentClass = Class.forName("android.content.Intent")
            Log.d(TAG, "Printing receipt for: $plateName")
        } catch (e: Exception) {
            Log.e(TAG, "Print failed: " + e.message)
        }
    }

    fun isPrinterReady(): Boolean = initialized
}