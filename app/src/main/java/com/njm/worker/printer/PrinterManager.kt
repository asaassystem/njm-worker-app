package com.njm.worker.printer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.njm.worker.R
import com.sunmi.printerx.PrinterSdk
import com.sunmi.printerx.api.PrintResult
import com.sunmi.printerx.enums.*
import com.sunmi.printerx.style.*
import com.sunmi.printerx.SdkException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class PrinterManager(private val context: Context) {

    private var printerSdk: PrinterSdk? = null
    private var printer: com.sunmi.printerx.Printer? = null
    private var isInitialized = false

    // 58mm paper = 384px max width on V2s
    companion object {
        const val PAPER_WIDTH = 384
        const val LOGO_WIDTH = 180
        const val LOGO_HEIGHT = 60
    }

    fun initialize(onReady: (Boolean, String) -> Unit) {
        try {
            PrinterSdk.getInstance().getPrinter(context, object : PrinterSdk.PrinterListen {
                override fun onDefPrinter(p: com.sunmi.printerx.Printer?) {
                    if (p != null) {
                        printer = p
                        isInitialized = true
                        onReady(true, "الطابعة جاهزة")
                    } else {
                        onReady(false, "لا توجد طابعة")
                    }
                }
                override fun onPrinters(printers: MutableList<com.sunmi.printerx.Printer>?) {
                    // Additional printers available
                }
            })
        } catch (e: SdkException) {
            onReady(false, "خطأ في تهيئة الطابعة: " + e.message)
        } catch (e: Exception) {
            onReady(false, "خطأ: " + e.message)
        }
    }

    fun isReady(): Boolean {
        if (!isInitialized || printer == null) return false
        return try {
            printer?.queryApi()?.status == QueryApi.Status.READY
        } catch (e: Exception) {
            false
        }
    }

    fun getStatusText(): String {
        if (!isInitialized || printer == null) return "غير متصلة"
        return try {
            when (printer?.queryApi()?.status) {
                QueryApi.Status.READY -> "جاهزة ✓"
                QueryApi.Status.ERR_PAPER_OUT -> "نفاد الورق"
                QueryApi.Status.ERR_COVER -> "الغطاء مفتوح"
                QueryApi.Status.ERR_PRINTER_HOT -> "سخونة زائدة"
                QueryApi.Status.OFFLINE -> "غير متصلة"
                else -> "حالة غير معروفة"
            }
        } catch (e: Exception) {
            "غير متصلة"
        }
    }

    fun printWashReceipt(
        carPlate: String,
        carType: String,
        orgName: String,
        price: Double,
        workerName: String,
        washId: Int,
        onResult: (Boolean, String) -> Unit
    ) {
        val p = printer
        if (p == null || !isInitialized) {
            onResult(false, "الطابعة غير متصلة")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val lineApi = p.lineApi()
                val dateTime = SimpleDateFormat("dd/MM/yyyy  HH:mm", Locale.getDefault()).format(Date())

                // Enable Transaction Mode for reliability
                lineApi.enableTransMode(true)

                // ===== HEADER =====
                lineApi.initLine(BaseStyle.getStyle().setAlign(Align.CENTER))

                // Logo
                getLogo()?.let { logo ->
                    lineApi.printBitmap(
                        logo,
                        BitmapStyle.getStyle()
                            .setAlgorithm(ImageAlgorithm.BINARIZATION)
                            .setValue(200f)
                    )
                }

                // Company name
                lineApi.printText(
                    "شركة NGM Plus
",
                    TextStyle.getStyle().setTextSize(28).enableBold(true)
                )
                lineApi.printText(
                    "إيصال غسيل سيارة
",
                    TextStyle.getStyle().setTextSize(20)
                )
                lineApi.printDividingLine(DividingLine.DOTTED, 2)

                // ===== DETAILS =====
                lineApi.initLine(BaseStyle.getStyle().setAlign(Align.LEFT))

                printRow(lineApi, "التاريخ:", dateTime)
                printRow(lineApi, "السيارة:", carPlate)
                printRow(lineApi, "النوع:", carType)
                printRow(lineApi, "المنشأة:", orgName)
                printRow(lineApi, "الموظف:", workerName)
                printRow(lineApi, "رقم العملية:", "#$washId")

                lineApi.printDividingLine(DividingLine.SOLID, 2)

                // ===== AMOUNT =====
                lineApi.initLine(BaseStyle.getStyle().setAlign(Align.CENTER))
                lineApi.printText("المبلغ المستحق
", TextStyle.getStyle().setTextSize(18))
                lineApi.printText(
                    String.format("%.2f ريال
", price),
                    TextStyle.getStyle().setTextSize(36).enableBold(true)
                )
                lineApi.printDividingLine(DividingLine.DOTTED, 2)

                // ===== FOOTER =====
                lineApi.printText("شكراً لاختياركم خدماتنا
", TextStyle.getStyle().setTextSize(16))
                lineApi.printText("njm.company
", TextStyle.getStyle().setTextSize(14))
                lineApi.printDividingLine(DividingLine.EMPTY, 40)

                // ===== SUBMIT TRANSACTION =====
                lineApi.printTrans { resultCode, message ->
                    CoroutineScope(Dispatchers.Main).launch {
                        if (resultCode == 0) {
                            onResult(true, "تمت الطباعة بنجاح")
                        } else {
                            onResult(false, "خطأ في الطباعة: $message")
                        }
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onResult(false, "خطأ: " + (e.message ?: "خطأ غير معروف"))
                }
            }
        }
    }

    private fun printRow(lineApi: com.sunmi.printerx.api.LineApi, label: String, value: String) {
        lineApi.printTexts(
            arrayOf(label, value),
            intArrayOf(1, 2),
            arrayOf(TextStyle.getStyle(), TextStyle.getStyle())
        )
    }

    private fun getLogo(): Bitmap? {
        return try {
            val opts = BitmapFactory.Options().apply { inScaled = false }
            val bmp = BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher, opts)
            Bitmap.createScaledBitmap(bmp, LOGO_WIDTH, LOGO_HEIGHT, true)
        } catch (e: Exception) {
            null
        }
    }

    fun destroy() {
        try {
            PrinterSdk.getInstance().destroy()
        } catch (e: Exception) { /* ignore */ }
    }
}