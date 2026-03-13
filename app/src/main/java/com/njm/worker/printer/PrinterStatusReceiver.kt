package com.njm.worker.printer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class PrinterStatusReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "PrinterStatus"
        var onStatusChange: ((String) -> Unit)? = null
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            "woyou.aidlservice.jiuv5.NORMAL_ACTION" -> {
                Log.d(TAG, "Printer normal")
                onStatusChange?.invoke("NORMAL")
            }
            "woyou.aidlservice.jiuv5.OUT_OF_PAPER_ACTION" -> {
                Log.d(TAG, "Printer out of paper")
                onStatusChange?.invoke("NO_PAPER")
            }
            "woyou.aidlservice.jiuv5.OVER_HEATING_ACITON" -> {
                Log.d(TAG, "Printer overheating")
                onStatusChange?.invoke("OVERHEATING")
            }
            "woyou.aidlservice.jiuv5.COVER_OPEN_ACTION" -> {
                Log.d(TAG, "Printer cover open")
                onStatusChange?.invoke("COVER_OPEN")
            }
        }
    }
}