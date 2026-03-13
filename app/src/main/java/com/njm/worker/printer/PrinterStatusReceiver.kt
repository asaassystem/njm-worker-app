package com.njm.worker.printer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class PrinterStatusReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val action = intent?.action ?: return
        // Broadcast printer status to any listening Activities
        val localIntent = Intent("com.njm.worker.PRINTER_STATUS")
        localIntent.putExtra("action", action)
        context?.sendBroadcast(localIntent)
    }
}