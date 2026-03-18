package com.njm.worker

import android.app.Application
import android.util.Log
import com.njm.worker.printer.PrinterManager

class NjmApp : Application() {

     companion object {
              lateinit var instance: NjmApp
                  private set
              var sunmiPrinterService: Any? = null
              var printerConnected = false
              var bindAttempted = false
     }

         override fun onCreate() {
                  super.onCreate()
                          instance = this
                  tryBindSunmiPrinter()
         }

             private fun tryBindSunmiPrinter() {
                      try {
                                   PrinterManager.init(applicationContext, forceRebind = false) {
                                                    if (PrinterManager.isConnected()) {
                                                                         Log.i("NjmApp", "Sunmi printer bound at startup OK")
                                                    } else {
                                                                         Log.i("NjmApp", "Sunmi printer not available at startup")
                                                    }
                                   }
                      } catch (e: Exception) {
                                   Log.w("NjmApp", "tryBindSunmiPrinter: ${e.message}")
                      }
             }
}
