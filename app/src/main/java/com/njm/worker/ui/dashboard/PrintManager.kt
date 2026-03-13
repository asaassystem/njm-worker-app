package com.njm.worker.ui.dashboard

import android.content.Context
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import com.njm.worker.data.model.WashRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.Socket

object PrintManager {

    fun printWashReceipt(context: Context, wash: WashRecord, activity: FragmentActivity) {
        val prefs = context.getSharedPreferences("print_settings", Context.MODE_PRIVATE)
        val method = prefs.getString("print_method", "sunmi")
        val autoPrint = prefs.getBoolean("auto_print", true)
        if (!autoPrint) return
        if (method == "network") {
            val ip = prefs.getString("printer_ip", "") ?: ""
            val port = prefs.getInt("printer_port", 9100)
            if (ip.isNotEmpty()) printNetworkEscPos(ip, port, wash, context)
            else Toast.makeText(context, "Set printer IP in settings", Toast.LENGTH_SHORT).show()
        } else {
            printSunmi(wash, context)
        }
    }

    fun printTest(context: Context, activity: FragmentActivity) {
        val prefs = context.getSharedPreferences("print_settings", Context.MODE_PRIVATE)
        val method = prefs.getString("print_method", "sunmi")
        if (method == "network") {
            val ip = prefs.getString("printer_ip", "") ?: ""
            val port = prefs.getInt("printer_port", 9100)
            if (ip.isNotEmpty()) printNetworkTest(ip, port, context)
            else Toast.makeText(context, "Set printer IP first", Toast.LENGTH_SHORT).show()
        } else {
            printSunmiText(context, "Test Print - NJM Car Wash")
        }
    }

    private fun printSunmi(wash: WashRecord, context: Context) {
        try {
            val serviceClass = Class.forName("woyou.aidlservice.jiuiv5.IWoyouService")
            val sm = context.getSystemService(serviceClass.name)
            if (sm == null) {
                Toast.makeText(context, "Sunmi printer not found", Toast.LENGTH_SHORT).show()
                return
            }
            val pt = sm.javaClass.getMethod("printText", String::class.java, Class.forName("android.os.IInterface"))
            val lw = sm.javaClass.getMethod("lineWrap", Int::class.java, Class.forName("android.os.IInterface"))
            pt.invoke(sm, "NJM Car Wash\n", null)
            pt.invoke(sm, "==================\n", null)
            pt.invoke(sm, "Plate: ${wash.plateNumber}\n", null)
            pt.invoke(sm, "Amount: ${wash.cost} SAR\n", null)
            pt.invoke(sm, "Time: ${wash.washTime}\n", null)
            pt.invoke(sm, "==================\n", null)
            lw.invoke(sm, 3, null)
        } catch (e: Exception) {
            Toast.makeText(context, "Print error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun printSunmiText(context: Context, text: String) {
        try {
            val sm = context.getSystemService("woyou.aidlservice.jiuiv5.IWoyouService")
            if (sm == null) { Toast.makeText(context, "Sunmi not available", Toast.LENGTH_SHORT).show(); return }
            val pt = sm.javaClass.getMethod("printText", String::class.java, Class.forName("android.os.IInterface"))
            val lw = sm.javaClass.getMethod("lineWrap", Int::class.java, Class.forName("android.os.IInterface"))
            pt.invoke(sm, text + "\n", null)
            lw.invoke(sm, 3, null)
            Toast.makeText(context, "Test sent to printer", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Sunmi error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun buildEscPos(wash: WashRecord): ByteArray {
        val lf = 10.toByte()
        val lines2 = mutableListOf<Byte>()
        lines2.addAll(byteArrayOf(0x1B, 0x40).toList())
        lines2.addAll(byteArrayOf(0x1B, 0x61, 0x01).toList())
        lines2.addAll(("NJM Car Wash").toByteArray().toList()); lines2.add(lf)
        lines2.addAll(("================").toByteArray().toList()); lines2.add(lf)
        lines2.addAll(byteArrayOf(0x1B, 0x61, 0x00).toList())
        lines2.addAll(("Plate: ${wash.plateNumber}").toByteArray().toList()); lines2.add(lf)
        lines2.addAll(("Type:  ${wash.carType ?: ""}").toByteArray().toList()); lines2.add(lf)
        lines2.addAll(("Amount:${wash.cost ?: 0} SAR").toByteArray().toList()); lines2.add(lf)
        lines2.addAll(("Time:  ${wash.washTime ?: ""}").toByteArray().toList()); lines2.add(lf)
        lines2.addAll(("================").toByteArray().toList()); lines2.add(lf)
        lines2.addAll(byteArrayOf(lf, lf, lf).toList())
        lines2.addAll(byteArrayOf(0x1D, 0x56, 0x00).toList())
        return lines2.toByteArray()
    }

    private fun printNetworkEscPos(ip: String, port: Int, wash: WashRecord, context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val data = buildEscPos(wash)
                val socket = Socket(ip, port)
                socket.getOutputStream().write(data)
                socket.getOutputStream().flush()
                socket.close()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Receipt printed", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Print error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun printNetworkTest(ip: String, port: Int, context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val data = ("Test Print - NJM\n\n\n").toByteArray()
                val socket = Socket(ip, port)
                socket.getOutputStream().write(data)
                socket.close()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Test sent to $ip:$port", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}