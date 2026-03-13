package com.njm.worker.ui.dashboard

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import com.njm.worker.data.model.WashRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.Socket
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PrintManager {
    private var printerService: Any? = null
    private var serviceConnected = false

    private fun bindSunmiPrinter(context: Context, onReady: (Any?) -> Unit) {
        if (serviceConnected && printerService != null) {
            onReady(printerService)
            return
        }
        try {
            val intent = Intent()
            intent.setPackage("woyou.aidlservice.jiuiv5")
            intent.action = "woyou.aidlservice.jiuiv5.IWoyouService"
            val conn = object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                    try {
                        val stubClass = Class.forName("woyou.aidlservice.jiuiv5.IWoyouService\$Stub")
                        val asInterface = stubClass.getMethod("asInterface", IBinder::class.java)
                        printerService = asInterface.invoke(null, binder)
                        serviceConnected = true
                        onReady(printerService)
                    } catch (e: Exception) {
                        serviceConnected = false
                        onReady(null)
                    }
                }
                override fun onServiceDisconnected(name: ComponentName?) {
                    printerService = null
                    serviceConnected = false
                }
            }
            val bound = context.applicationContext.bindService(intent, conn, Context.BIND_AUTO_CREATE)
            if (!bound) onReady(null)
        } catch (e: Exception) {
            onReady(null)
        }
    }

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

    fun printDailyReport(
        context: Context,
        washes: List<WashRecord>,
        totalAmount: Double,
        paidAmount: Double,
        unpaidAmount: Double
    ) {
        val prefs = context.getSharedPreferences("print_settings", Context.MODE_PRIVATE)
        val method = prefs.getString("print_method", "sunmi")
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        if (method == "network") {
            val ip = prefs.getString("printer_ip", "") ?: ""
            val port = prefs.getInt("printer_port", 9100)
            if (ip.isNotEmpty()) {
                printNetworkReport(ip, port, context, "تقرير اليوم", today, washes, totalAmount, paidAmount, unpaidAmount)
            } else {
                Toast.makeText(context, "Set printer IP in settings", Toast.LENGTH_SHORT).show()
            }
        } else {
            printSunmiReport(context, "تقرير اليوم", today, washes, totalAmount, paidAmount, unpaidAmount)
        }
    }

    fun printMonthlyReport(
        context: Context,
        washes: List<WashRecord>,
        totalAmount: Double,
        paidAmount: Double,
        unpaidAmount: Double
    ) {
        val prefs = context.getSharedPreferences("print_settings", Context.MODE_PRIVATE)
        val method = prefs.getString("print_method", "sunmi")
        val month = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
        if (method == "network") {
            val ip = prefs.getString("printer_ip", "") ?: ""
            val port = prefs.getInt("printer_port", 9100)
            if (ip.isNotEmpty()) {
                printNetworkReport(ip, port, context, "تقرير الشهر", month, washes, totalAmount, paidAmount, unpaidAmount)
            } else {
                Toast.makeText(context, "Set printer IP in settings", Toast.LENGTH_SHORT).show()
            }
        } else {
            printSunmiReport(context, "تقرير الشهر", month, washes, totalAmount, paidAmount, unpaidAmount)
        }
    }

    private fun printSunmiReport(
        context: Context,
        title: String,
        period: String,
        washes: List<WashRecord>,
        totalAmount: Double,
        paidAmount: Double,
        unpaidAmount: Double
    ) {
        bindSunmiPrinter(context) { svc ->
            if (svc == null) {
                Toast.makeText(context, "طابعة Sunmi غير متاحة", Toast.LENGTH_SHORT).show()
                return@bindSunmiPrinter
            }
            try {
                val cls = svc.javaClass
                fun txt(s: String) {
                    try { cls.getMethod("printText", String::class.java, Any::class.java).invoke(svc, s, null) } catch (e: Exception) {}
                }
                fun align(a: Int) {
                    try { cls.getMethod("setAlignment", Int::class.java, Any::class.java).invoke(svc, a, null) } catch (e: Exception) {}
                }
                align(1)
                txt("نجم الموقود\n")
                txt("NJM Car Wash\n")
                txt("==================\n")
                txt("$title\n")
                txt("$period\n")
                txt("==================\n")
                align(0)
                txt("عدد الغسيل: ${washes.size}\n")
                txt("الإجمالي: ${"%.2f".format(totalAmount)} ر.س\n")
                txt("المدفوع: ${"%.2f".format(paidAmount)} ر.س\n")
                txt("غير المدفوع: ${"%.2f".format(unpaidAmount)} ر.س\n")
                txt("==================\n")
                txt("تفاصيل الغسيل:\n")
                txt("------------------\n")
                washes.forEachIndexed { i, w ->
                    txt("${i + 1}. ${w.plateNumber}\n")
                    txt("   النوع: ${w.carType ?: "-"}\n")
                    txt("   المبلغ: ${w.cost} ر.س\n")
                    txt("   الحالة: ${if (w.isPaid == true) "مدفوع" else "غير مدفوع"}\n")
                }
                txt("==================\n")
                align(1)
                txt("شكراً لاستخدامكم\n\n\n")
                try { cls.getMethod("cutPaper", Int::class.java, Any::class.java).invoke(svc, 1, null) } catch (e: Exception) {}
                Toast.makeText(context, "تم طباعة التقرير", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "خطأ في الطباعة: ${e.message}", Toast.LENGTH_SHORT).show()
            }
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
            bindSunmiPrinter(context) { svc ->
                if (svc == null) {
                    Toast.makeText(context, "Sunmi printer not available", Toast.LENGTH_SHORT).show()
                } else {
                    try {
                        val cls = svc.javaClass
                        cls.getMethod("printText", String::class.java, Any::class.java).invoke(svc, "Test Print - NJM Car Wash\n\n\n", null)
                        Toast.makeText(context, "Test sent to printer", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Print error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun printSunmi(wash: WashRecord, context: Context) {
        bindSunmiPrinter(context) { svc ->
            if (svc == null) {
                Toast.makeText(context, "Sunmi printer not found", Toast.LENGTH_SHORT).show()
                return@bindSunmiPrinter
            }
            try {
                val cls = svc.javaClass
                fun txt(s: String) {
                    try { cls.getMethod("printText", String::class.java, Any::class.java).invoke(svc, s, null) } catch (e: Exception) {}
                }
                fun align(a: Int) {
                    try { cls.getMethod("setAlignment", Int::class.java, Any::class.java).invoke(svc, a, null) } catch (e: Exception) {}
                }
                align(1)
                txt("نجم الموقود\n")
                txt("NJM Car Wash\n")
                txt("==================\n")
                align(0)
                txt("لوحة: ${wash.plateNumber}\n")
                txt("النوع: ${wash.carType ?: ""}\n")
                txt("الجهة: ${wash.orgName ?: ""}\n")
                txt("المبلغ: ${wash.cost} ر.س\n")
                txt("الوقت: ${wash.washTime ?: ""}\n")
                txt("الحالة: ${if (wash.isPaid == true) "مدفوع" else "غير مدفوع"}\n")
                txt("==================\n")
                align(1)
                txt("شكراً لاستخدامكم\n\n\n")
                try { cls.getMethod("cutPaper", Int::class.java, Any::class.java).invoke(svc, 1, null) } catch (e: Exception) {}
            } catch (e: Exception) {
                Toast.makeText(context, "Print error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun buildEscPosReport(
        title: String,
        period: String,
        washes: List<WashRecord>,
        totalAmount: Double,
        paidAmount: Double,
        unpaidAmount: Double
    ): ByteArray {
        val lf = 10.toByte()
        val lines2 = mutableListOf<Byte>()
        lines2.addAll(byteArrayOf(0x1B, 0x40).toList())
        lines2.addAll(byteArrayOf(0x1B, 0x61, 0x01).toList())
        lines2.addAll(("NJM Car Wash").toByteArray().toList()); lines2.add(lf)
        lines2.addAll((title).toByteArray().toList()); lines2.add(lf)
        lines2.addAll((period).toByteArray().toList()); lines2.add(lf)
        lines2.addAll(("================").toByteArray().toList()); lines2.add(lf)
        lines2.addAll(byteArrayOf(0x1B, 0x61, 0x00).toList())
        lines2.addAll(("Count: ${washes.size}").toByteArray().toList()); lines2.add(lf)
        lines2.addAll(("Total: ${"%.2f".format(totalAmount)} SAR").toByteArray().toList()); lines2.add(lf)
        lines2.addAll(("Paid: ${"%.2f".format(paidAmount)} SAR").toByteArray().toList()); lines2.add(lf)
        lines2.addAll(("Unpaid: ${"%.2f".format(unpaidAmount)} SAR").toByteArray().toList()); lines2.add(lf)
        lines2.addAll(("================").toByteArray().toList()); lines2.add(lf)
        washes.forEachIndexed { i, w ->
            lines2.addAll(("${i+1}. ${w.plateNumber} - ${w.cost} SAR").toByteArray().toList()); lines2.add(lf)
        }
        lines2.addAll(byteArrayOf(lf, lf, lf).toList())
        lines2.addAll(byteArrayOf(0x1D, 0x56, 0x00).toList())
        return lines2.toByteArray()
    }

    private fun printNetworkReport(
        ip: String,
        port: Int,
        context: Context,
        title: String,
        period: String,
        washes: List<WashRecord>,
        totalAmount: Double,
        paidAmount: Double,
        unpaidAmount: Double
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val data = buildEscPosReport(title, period, washes, totalAmount, paidAmount, unpaidAmount)
                val socket = Socket(ip, port)
                socket.getOutputStream().write(data)
                socket.getOutputStream().flush()
                socket.close()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "تم طباعة التقرير", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "خطأ: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
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
        lines2.addAll(("Type: ${wash.carType ?: ""}").toByteArray().toList()); lines2.add(lf)
        lines2.addAll(("Amount: ${wash.cost ?: 0} SAR").toByteArray().toList()); lines2.add(lf)
        lines2.addAll(("Time: ${wash.washTime ?: ""}").toByteArray().toList()); lines2.add(lf)
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
