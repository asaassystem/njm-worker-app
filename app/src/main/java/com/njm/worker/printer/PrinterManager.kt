package com.njm.worker.printer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.sunmi.peripheral.printer.InnerPrinterCallback
import com.sunmi.peripheral.printer.InnerPrinterException
import com.sunmi.peripheral.printer.InnerPrinterManager
import com.sunmi.peripheral.printer.SunmiPrinterService
import com.njm.worker.NjmApp
import com.njm.worker.data.model.Invoice
import com.njm.worker.data.model.WashRecord
import java.net.URL

/**
 * PrinterManager - Sunmi Built-in Printer via InnerPrinterManager (Official SDK)
  * ZATCA-Compliant Receipt with NJM Logo at top
   * Developer: meshari.tech
    */
object PrinterManager {
     private const val TAG = "NJM_Printer"
     private var printerService: SunmiPrinterService? = null
     private var connected = false
     private var bindingInProgress = false

     fun init(context: Context, forceRebind: Boolean = false, onReady: () -> Unit = {}) {
              val appPrinter = NjmApp.sunmiPrinter
              if (appPrinter != null && NjmApp.printerConnected) {
                           printerService = appPrinter
                           connected = true
                           onReady()
                                       return
              }
                      if (forceRebind) {
                                   NjmApp.bindAttempted = false
                                   NjmApp.printerConnected = false
                                   NjmApp.sunmiPrinter = null
                                   connected = false
                                   bindingInProgress = false
                      }
                              if (bindingInProgress) {
                                           Handler(Looper.getMainLooper()).postDelayed({
                                                            val p = NjmApp.sunmiPrinter
                                                            if (p != null) {
                                                                                 printerService = p
                                                                                 connected = true
                                                                                 NjmApp.printerConnected = true
                                                            } else {
                                                                                 connected = false
                                                            }
                                                                            onReady()
                                           }, 3000)
                                                       return
                              }
                                      if (NjmApp.bindAttempted && !NjmApp.printerConnected) {
                                                   NjmApp.bindAttempted = false
                                      }
                                              bindingInProgress = true
              NjmApp.bindAttempted = true
              try {
                           InnerPrinterManager.getInstance().bindService(
                                            context.applicationContext,
                                            object : InnerPrinterCallback() {
                                                                 override fun onConnected(service: SunmiPrinterService?) {
                                                                                          printerService = service
                                                                                          connected = (service != null)
                                                                                                                  NjmApp.sunmiPrinter = service
                                                                                          NjmApp.printerConnected = connected
                                                                                          bindingInProgress = false
                                                                                          Log.i(TAG, if (connected) "Sunmi printer connected OK" else "Sunmi service null")
                                                                                                                  onReady()
                                                                 }
                                                                                     override fun onDisconnected() {
                                                                                                              printerService = null
                                                                                                              connected = false
                                                                                                              NjmApp.sunmiPrinter = null
                                                                                                              NjmApp.printerConnected = false
                                                                                                              NjmApp.bindAttempted = false
                                                                                                              bindingInProgress = false
                                                                                                              Log.w(TAG, "Sunmi printer disconnected")
                                                                                                                                      onReady()
                                                                                     }
                                            }
                                                        )
              } catch (e: InnerPrinterException) {
                           Log.w(TAG, "Not a Sunmi device or AIDL error: ${e.message}")
                                       connected = false
                           bindingInProgress = false
                           onReady()
              } catch (e: Exception) {
                           Log.w(TAG, "Printer bind failed: ${e.message}")
                                       connected = false
                           bindingInProgress = false
                           onReady()
              }
     }

         fun isConnected() = connected && printerService != null
     fun isAvailable() = isConnected()

         fun unbindService(context: Context) {
                  try {
                               InnerPrinterManager.getInstance().unBindService(context, object : InnerPrinterCallback() {
                                                override fun onConnected(service: SunmiPrinterService?) {}
                                                                override fun onDisconnected() {
                                                                                     printerService = null
                                                                                     connected = false
                                                                                     NjmApp.sunmiPrinter = null
                                                                                     NjmApp.printerConnected = false
                                                                                     NjmApp.bindAttempted = false
                                                                }
                               })
                  } catch (e: Exception) {
                               Log.w(TAG, "Unbind: ${e.message}")
                  }
         }

             /** Load NJM logo bitmap from URL (run on background thread) */
                 private fun loadLogoBitmap(): Bitmap? {
                          return try {
                                       val url = URL("https://njm.company/static/img/logo.png")
                                                   val bmp = BitmapFactory.decodeStream(url.openStream())
                                                               // Scale to 384px wide (Sunmi paper width) keeping aspect ratio
                                                                           if (bmp != null) {
                                                                                            val targetW = 384
                                                                                            val ratio = targetW.toFloat() / bmp.width.toFloat()
                                                                                                            val targetH = (bmp.height * ratio).toInt()
                                                                                                                            Bitmap.createScaledBitmap(bmp, targetW, targetH, true)
                                                                           } else null
                          } catch (e: Exception) {
                                       Log.w(TAG, "Logo load failed: ${e.message}")
                                                   null
                          }
                 }

                     fun printWashReceipt(
                              workerName: String,
                              plateName: String,
                              carType: String,
                              cost: Double,
                              orgName: String,
                              isPaid: Boolean = true,
                              washDate: String = "",
                              washTime: String = "",
                              invoiceNumber: String = "",
                              context: Context? = null
                          ) {
                              val svc = printerService
                              if (svc == null) {
                                           Log.w(TAG, "printWashReceipt: printer not connected, skipping")
                                                       if (context != null) {
                                                                        init(context, forceRebind = false) {
                                                                                             val svc2 = printerService
                                                                                             if (svc2 != null) {
                                                                                                                      Thread {
                                                                                                                                                   val logo = loadLogoBitmap()
                                                                                                                                                                               Handler(Looper.getMainLooper()).post {
                                                                                                                                                                                                                doActualPrint(svc2, workerName, plateName, carType, cost, orgName,
                                                                                                                                                                                                                                                                  isPaid, washDate, washTime, invoiceNumber, logo)
                                                                                                                                                                                                                                            }
                                                                                                                      }.start()
                                                                                             }
                                                                        }
                                                       }
                                                                   return
                              }
                                      Thread {
                                                   val logo = loadLogoBitmap()
                                                               Handler(Looper.getMainLooper()).post {
                                                                                doActualPrint(svc, workerName, plateName, carType, cost, orgName,
                                                                                                                  isPaid, washDate, washTime, invoiceNumber, logo)
                                                               }
                                      }.start()
                     }

                         private fun doActualPrint(
                                  svc: SunmiPrinterService,
                                  workerName: String,
                                  plateName: String,
                                  carType: String,
                                  cost: Double,
                                  orgName: String,
                                  isPaid: Boolean,
                                  washDate: String,
                                  washTime: String,
                                  invoiceNumber: String,
                                  logo: Bitmap? = null
                              ) {
                                  try {
                                               svc.printerInit(null)

                                                           // ========== LOGO AT TOP ==========
                                                                       if (logo != null) {
                                                                                        svc.setAlignment(1, null)
                                                                                                        svc.printBitmap(logo, null)
                                                                                                                        svc.lineWrap(1, null)
                                                                       }

                                                                                   // ========== HEADER ==========
                                                                                               svc.setAlignment(1, null)
                                                                                                           svc.setFontSize(28f, null)
                                                                                                                       svc.printText("$orgName\n", null)
                                                                                                                                   svc.setFontSize(20f, null)
                                                                                                                                               svc.printText("نظام إدارة غسيل السيارات - NJM\n", null)
                                                                                                                                                           svc.setFontSize(18f, null)
                                                                                                                                                                       svc.printText("حفر الباطن - المملكة العربية السعودية\n", null)
                                                                                                                                                                                   svc.setAlignment(0, null)
                                                                                                                                                                                               svc.setFontSize(19f, null)
                                                                                                                                                                                                           svc.printText("================================\n", null)
                                                                                                                                                                                                           
                                                                                                                                                                                                                       // ========== ZATCA INVOICE INFO ==========
                                                                                                                                                                                                                                   val prefs = try {
                                                                                                                                                                                                                                                    android.app.Application::class.java
                                                                                                                                                                                                                                                        .getDeclaredMethod("getApplicationContext")
                                                                                                                                                                                                                                                                            .invoke(NjmApp.instance)
                                                                                                                                                                                                                                                                                                .let { (it as Context).getSharedPreferences("print_settings", Context.MODE_PRIVATE) }
                                                                                                                                                                                                                                                                                                            } catch (e: Exception) { null }
                                                                                                                                                                                                                                   
                                                                                                                                                                                                                                               val vatNumber = prefs?.getString("vat_number", "") ?: ""
                                               val crNumber  = prefs?.getString("cr_number", "")  ?: ""
                                               val address   = prefs?.getString("address", "حفر الباطن") ?: "حفر الباطن"

                                               // Invoice number
                                               val finalInvNum = if (invoiceNumber.isNotBlank()) invoiceNumber
                                                                 else "NJM-${System.currentTimeMillis() % 1000000}"
                                               svc.printText("فاتورة ضريبية مبسطة\n", null)
                                                           svc.printText("رقم الفاتورة : $finalInvNum\n", null)

                                                                       // Date/Time
                                                                                   val dateStr = if (washDate.isNotBlank()) washDate
                                                             else java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                                                                                           .format(java.util.Date())
                                                                                                       val timeStr = if (washTime.isNotBlank()) washTime
                                                             else java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US)
                                                                                           .format(java.util.Date())
                                                                                                       svc.printText("التاريخ       : $dateStr\n", null)
                                                                                                                   svc.printText("الوقت         : $timeStr\n", null)
                                                                                                                   
                                                                                                                               if (vatNumber.isNotBlank())
                                                                                                                                               svc.printText("الرقم الضريبي : $vatNumber\n", null)
                                                                                                                                                           if (crNumber.isNotBlank())
                                                                                                                                                                           svc.printText("السجل التجاري : $crNumber\n", null)
                                                                                                                                                                                       svc.printText("العنوان       : $address\n", null)
                                                                                                                                                                                       
                                                                                                                                                                                                   svc.printText("================================\n", null)
                                                                                                                                                                                                   
                                                                                                                                                                                                               // ========== VEHICLE & SERVICE INFO ==========
                                                                                                                                                                                                                           svc.printText("بيانات المركبة والخدمة\n", null)
                                                                                                                                                                                                                                       svc.printText("--------------------------------\n", null)
                                                                                                                                                                                                                                                   svc.printText("رقم اللوحة    : $plateName\n", null)
                                                                                                                                                                                                                                                               svc.printText("نوع المركبة   : $carType\n", null)
                                                                                                                                                                                                                                                                           svc.printText("المنشأة       : $orgName\n", null)
                                                                                                                                                                                                                                                                                       if (workerName.isNotBlank())
                                                                                                                                                                                                                                                                                                       svc.printText("الموظف        : $workerName\n", null)
                                                                                                                                                                                                                                                                                                                   svc.printText("نوع الخدمة    : غسيل سيارة\n", null)
                                                                                                                                                                                                                                                                                                                               svc.printText("================================\n", null)
                                                                                                                                                                                                                                                                                                                               
                                                                                                                                                                                                                                                                                                                                           // ========== PRICE BREAKDOWN (ZATCA) ==========
                                                                                                                                                                                                                                                                                                                                                       svc.printText("تفاصيل المبلغ\n", null)
                                                                                                                                                                                                                                                                                                                                                                   svc.printText("--------------------------------\n", null)
                                                                                                                                                                                                                                                                                                                                                                               val vat     = cost * 15.0 / 115.0
                                               val subtotal = cost - vat
                                               svc.printText("المبلغ قبل الضريبة : ${String.format("%.2f", subtotal)} ر.س\n", null)
                                                           svc.printText("ضريبة القيمة المضافة\n", null)
                                                                       svc.printText("  نسبة الضريبة 15%%  : ${String.format("%.2f", vat)} ر.س\n", null)
                                                                                   svc.printText("================================\n", null)
                                                                                               svc.setFontSize(24f, null)
                                                                                                           svc.printText("الإجمالي شامل الضريبة\n", null)
                                                                                                                       svc.printText("${String.format("%.2f", cost)} ريال سعودي\n", null)
                                                                                                                                   svc.setFontSize(19f, null)
                                                                                                                                               svc.printText("================================\n", null)
                                                                                                                                                           svc.printText("حالة الدفع    : ${if (isPaid) "✓ مدفوع" else "✗ غير مدفوع"}\n", null)
                                                                                                                                                                       svc.printText("================================\n", null)
                                                                                                                                                                       
                                                                                                                                                                                   // ========== ZATCA QR CODE PLACEHOLDER ==========
                                                                                                                                                                                               svc.setAlignment(1, null)
                                                                                                                                                                                                           svc.printText("[ رمز QR - ZATCA ]\n", null)
                                                                                                                                                                                                                       svc.setAlignment(0, null)
                                                                                                                                                                                                                                   svc.printText("================================\n", null)
                                                                                                                                                                                                                                   
                                                                                                                                                                                                                                               // ========== FOOTER ==========
                                                                                                                                                                                                                                                           svc.setAlignment(1, null)
                                                                                                                                                                                                                                                                       svc.setFontSize(18f, null)
                                                                                                                                                                                                                                                                                   svc.printText("شكراً لتعاملكم معنا\n", null)
                                                                                                                                                                                                                                                                                               svc.printText("NJM Car Wash Management System\n", null)
                                                                                                                                                                                                                                                                                                           svc.printText("www.njm.company\n", null)
                                                                                                                                                                                                                                                                                                                       svc.lineWrap(3, null)
                                                                                                                                                                                                                                                                                                                                   svc.cutPaper(null)
                                                                                                                                                                                                                                                                                                                                   
                                                                                                                                                                                                                                                                                                                                               Log.i(TAG, "ZATCA Print OK: plate=$plateName inv=$finalInvNum")
                                                                                                                                                                                                                                                                                                                                                       } catch (e: Exception) {
                                               Log.e(TAG, "doActualPrint error: ${e.message}")
                                  }
                         }

                             fun printDailyReport(
                                      context: Context,
                                      washes: List<WashRecord>,
                                      date: String,
                                      totalOverride: Double = -1.0,
                                      paidOverride: Double = -1.0,
                                      unpaidOverride: Double = -1.0
                                  ) {
                                      val svc = printerService ?: return
                                      val total  = if (totalOverride  >= 0) totalOverride  else washes.sumOf { it.cost ?: 0.0 }
                                              val paid   = if (paidOverride   >= 0) paidOverride   else washes.filter { (it.isPaid ?: 1) == 1 }.sumOf { it.cost ?: 0.0 }
                                                      val unpaid = if (unpaidOverride >= 0) unpaidOverride else total - paid
                                      try {
                                                   svc.printerInit(null)
                                                               svc.setAlignment(1, null); svc.setFontSize(26f, null)
                                                                           svc.printText("تقرير اليوم\n$date\n", null)
                                                                                       svc.setAlignment(0, null); svc.setFontSize(20f, null)
                                                                                                   svc.printText("================================\n", null)
                                                                                                               svc.printText("عدد الغسيل : ${washes.size}\n", null)
                                                                                                                           svc.printText("الإجمالي   : ${String.format("%.2f", total)} ر.س\n", null)
                                                                                                                                       svc.printText("المدفوع    : ${String.format("%.2f", paid)} ر.س\n", null)
                                                                                                                                                   svc.printText("غير المدفوع: ${String.format("%.2f", unpaid)} ر.س\n", null)
                                                                                                                                                               svc.printText("================================\n", null)
                                                                                                                                                                           svc.setAlignment(1, null); svc.printText("نظام نجم\n\n\n", null)
                                                                                                                                                                                       svc.cutPaper(null)
                                      } catch (e: Exception) {
                                                   Log.e(TAG, "printDailyReport: ${e.message}")
                                      }
                             }

                                 fun printMonthlyReport(
                                          context: Context,
                                          washes: List<WashRecord>,
                                          month: String,
                                          totalOverride: Double = -1.0,
                                          paidOverride: Double = -1.0,
                                          unpaidOverride: Double = -1.0
                                      ) {
                                          val svc = printerService ?: return
                                          val total  = if (totalOverride  >= 0) totalOverride  else washes.sumOf { it.cost ?: 0.0 }
                                                  val paid   = if (paidOverride   >= 0) paidOverride   else washes.filter { (it.isPaid ?: 1) == 1 }.sumOf { it.cost ?: 0.0 }
                                                          val unpaid = if (unpaidOverride >= 0) unpaidOverride else total - paid
                                          try {
                                                       svc.printerInit(null)
                                                                   svc.setAlignment(1, null); svc.setFontSize(26f, null)
                                                                               svc.printText("تقرير الشهر\n$month\n", null)
                                                                                           svc.setAlignment(0, null); svc.setFontSize(20f, null)
                                                                                                       svc.printText("================================\n", null)
                                                                                                                   svc.printText("عدد الغسيل : ${washes.size}\n", null)
                                                                                                                               svc.printText("الإجمالي   : ${String.format("%.2f", total)} ر.س\n", null)
                                                                                                                                           svc.printText("المدفوع    : ${String.format("%.2f", paid)} ر.س\n", null)
                                                                                                                                                       svc.printText("غير المدفوع: ${String.format("%.2f", unpaid)} ر.س\n", null)
                                                                                                                                                                   svc.printText("================================\n", null)
                                                                                                                                                                               svc.setAlignment(1, null); svc.printText("نظام نجم\n\n\n", null)
                                                                                                                                                                                           svc.cutPaper(null)
                                          } catch (e: Exception) {
                                                       Log.e(TAG, "printMonthlyReport: ${e.message}")
                                          }
                                 }

                                     fun printInvoice(context: Context, invoice: Invoice) {
                                              val svc = printerService ?: return
                                              val prefs = context.getSharedPreferences("print_settings", Context.MODE_PRIVATE)
                                                      val orgName    = prefs.getString("org_name", "مغسلة نجم")    ?: "مغسلة نجم"
                                              val vatNumber  = prefs.getString("vat_number", "")            ?: ""
                                              val crNumber   = prefs.getString("cr_number", "")             ?: ""
                                              val address    = prefs.getString("address", "حفر الباطن")    ?: "حفر الباطن"
                                              Thread {
                                                           val logo = loadLogoBitmap()
                                                                       Handler(Looper.getMainLooper()).post {
                                                                                        try {
                                                                                                             svc.printerInit(null)
                                                                                                                                 // Logo
                                                                                                                                                     if (logo != null) {
                                                                                                                                                                              svc.setAlignment(1, null)
                                                                                                                                                                                                      svc.printBitmap(logo, null)
                                                                                                                                                                                                                              svc.lineWrap(1, null)
                                                                                                                                                                                                                                                  }
                                                                                                                                                                         svc.setAlignment(1, null); svc.setFontSize(26f, null)
                                                                                                                                                                                             svc.printText("فاتورة ضريبية - ZATCA\n$orgName\n", null)
                                                                                                                                                                                                                 svc.setAlignment(0, null); svc.setFontSize(20f, null)
                                                                                                                                                                                                                                     svc.printText("================================\n", null)
                                                                                                                                                                                                                                                         svc.printText("رقم الفاتورة  : ${invoice.invoiceNumber}\n", null)
                                                                                                                                                                                                                                                                             svc.printText("التاريخ       : ${invoice.invoiceDate ?: ""}\n", null)
                                                                                                                                                                                                                                                                                                 if (vatNumber.isNotBlank()) svc.printText("الرقم الضريبي : $vatNumber\n", null)
                                                                                                                                                                                                                                                                                                                     if (crNumber.isNotBlank())  svc.printText("السجل التجاري : $crNumber\n", null)
                                                                                                                                                                                                                                                                                                                                         svc.printText("العنوان       : $address\n", null)
                                                                                                                                                                                                                                                                                                                                                             svc.printText("================================\n", null)
                                                                                                                                                                                                                                                                                                                                                                                 svc.printText("المنشأة       : ${invoice.organizationName ?: ""}\n", null)
                                                                                                                                                                                                                                                                                                                                                                                                     svc.printText("الفترة        : ${invoice.periodStart ?: ""} - ${invoice.periodEnd ?: ""}\n", null)
                                                                                                                                                                                                                                                                                                                                                                                                                         svc.printText("عدد الغسيل    : ${invoice.totalWashes ?: 0}\n", null)
                                                                                                                                                                                                                                                                                                                                                                                                                                             svc.printText("================================\n", null)
                                                                                                                                                                                                                                                                                                                                                                                                                                                                 svc.printText("المبلغ الإجمالي: ${String.format("%.2f", invoice.totalAmount ?: 0.0)} ر.س\n", null)
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     svc.printText("ضريبة 15%%     : ${String.format("%.2f", invoice.vatAmount ?: 0.0)} ر.س\n", null)
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         svc.setFontSize(24f, null)
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             svc.printText("المجموع الكلي : ${String.format("%.2f", invoice.grandTotal ?: 0.0)} ر.س\n", null)
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 svc.setFontSize(20f, null)
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     svc.printText("================================\n", null)
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         svc.setAlignment(1, null)
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             svc.printText("نظام نجم - NJM\nwww.njm.company\n\n\n", null)
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 svc.cutPaper(null)
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 } catch (e: Exception) {
                                                                                                             Log.e(TAG, "printInvoice: ${e.message}")
                                                                                        }
                                                                       }
                                              }.start()
                                     }
}
