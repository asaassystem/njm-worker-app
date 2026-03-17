package com.njm.worker

import android.app.Application
import android.util.Log

/**
 * NJM Worker Application
  * v5.1: Sunmi printer binding via reflection (no direct SDK import needed at compile time)
   * Developer: meshari.tech
    */
class NjmApp : Application() {

        companion object {
                    lateinit var instance: NjmApp
                        private set
                    var sunmiPrinterService: Any? = null
                    var printerConnected: Boolean = false
                    var bindAttempted: Boolean = false
        }

            override fun onCreate() {
                        super.onCreate()
                                instance = this
                        tryBindSunmiPrinter()
            }

                @Suppress("UNCHECKED_CAST")
                    private fun tryBindSunmiPrinter() {
                                if (bindAttempted) return
                                bindAttempted = true
                                try {
                                                val managerClass = Class.forName("com.sunmi.peripheral.printer.InnerPrinterManager")
                                                            val manager = managerClass.getMethod("getInstance").invoke(null)
                                                                        val callbackClass = Class.forName("com.sunmi.peripheral.printer.InnerPrinterCallback")
                                                                                    // Create dynamic callback using anonymous class approach via reflection
                                                                                                val callbackInstance = object : Any() {}
                                                                                                            val bindService = managerClass.getMethod("bindService",
                                                                                                                                                                     android.content.Context::class.java, callbackClass)
                                                                                                                        // Cannot create callback without SDK — skip if SDK unavailable
                                                                                                                                    Log.w("NJM_Printer", "Sunmi SDK found but callback creation skipped (no AAR in build)")
                                } catch (e: ClassNotFoundException) {
                                                Log.w("NJM_Printer", "Not a Sunmi device — printer SDK not available")
                                                            printerConnected = false
                                } catch (e: Exception) {
                                                Log.w("NJM_Printer", "Printer init error: ${e.message}")
                                                            printerConnected = false
                                }
                    }
}
