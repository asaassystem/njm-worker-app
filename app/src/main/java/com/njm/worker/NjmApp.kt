package com.njm.worker

import android.app.Application
import android.util.Log
import com.sunmi.peripheral.printer.InnerPrinterCallback
import com.sunmi.peripheral.printer.InnerPrinterException
import com.sunmi.peripheral.printer.InnerPrinterManager
import com.sunmi.peripheral.printer.SunmiPrinterService

/**
 * NJM Worker Application
 * Binds Sunmi built-in printer on startup via official InnerPrinterManager SDK
 * Developer: meshari.tech
 */
class NjmApp : Application() {

    companion object {
        lateinit var instance: NjmApp
            private set
        var sunmiPrinter: SunmiPrinterService? = null
        var printerConnected: Boolean = false
        var bindAttempted: Boolean = false
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        bindSunmiPrinter()
    }

    private fun bindSunmiPrinter() {
        if (bindAttempted) return
        try {
            bindAttempted = true
            InnerPrinterManager.getInstance().bindService(
                this,
                object : InnerPrinterCallback() {
                    override fun onConnected(service: SunmiPrinterService?) {
                        sunmiPrinter = service
                        printerConnected = (service != null)
                        Log.i("NJM_Printer", "Sunmi printer connected")
                    }
                    override fun onDisconnected() {
                        sunmiPrinter = null
                        printerConnected = false
                        bindAttempted = false
                        Log.w("NJM_Printer", "Sunmi printer disconnected")
                    }
                }
            )
        } catch (e: InnerPrinterException) {
            Log.w("NJM_Printer", "Not a Sunmi device: ${e.message}")
            printerConnected = false
        } catch (e: Exception) {
            Log.w("NJM_Printer", "Printer bind error: ${e.message}")
            printerConnected = false
        }
    }
}
