package com.njm.worker.ui.dashboard

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.njm.worker.R
import com.njm.worker.data.model.Invoice
import com.njm.worker.data.repository.WorkerRepository
import com.njm.worker.printer.PrintManager
import com.njm.worker.utils.SessionManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class InvoicesFragment : Fragment() {

    private val repo = WorkerRepository()
    private val invoices = mutableListOf<Invoice>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_invoices, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadInvoices(view)
        setupCreateInvoice(view)
    }

    private fun loadInvoices(view: View) {
        val progressBar = view.findViewById<ProgressBar>(R.id.progressInvoices)
        val lvInvoices = view.findViewById<ListView>(R.id.lvInvoices)
        val tvEmpty = view.findViewById<TextView>(R.id.tvNoInvoices)
        progressBar?.visibility = View.VISIBLE

        lifecycleScope.launch {
            val result = repo.getInvoices()
            progressBar?.visibility = View.GONE

            result.onSuccess { resp ->
                invoices.clear()
                invoices.addAll(resp.invoices ?: emptyList())
                if (invoices.isEmpty()) {
                    tvEmpty?.visibility = View.VISIBLE
                    lvInvoices?.visibility = View.GONE
                } else {
                    tvEmpty?.visibility = View.GONE
                    lvInvoices?.visibility = View.VISIBLE
                    val adapter = ArrayAdapter(requireContext(),
                        android.R.layout.simple_list_item_2,
                        android.R.id.text1,
                        invoices.map { "${it.invoiceNumber} - ${it.status ?: "draft"}" }
                    )
                    lvInvoices?.adapter = adapter
                    lvInvoices?.setOnItemClickListener { _, _, pos, _ ->
                        showInvoiceDetail(invoices[pos])
                    }
                }
            }.onFailure {
                tvEmpty?.text = "خطأ في تحميل الفواتير"
                tvEmpty?.visibility = View.VISIBLE
            }
        }
    }

    private fun setupCreateInvoice(view: View) {
        val btnCreate = view.findViewById<Button>(R.id.btnCreateInvoice)
        val tvDateRange = view.findViewById<TextView>(R.id.tvDateRange)
        var startDate = ""
        var endDate = ""

        val cal = Calendar.getInstance()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        // Set default: first day of month to today
        val first = Calendar.getInstance()
        first.set(Calendar.DAY_OF_MONTH, 1)
        startDate = sdf.format(first.time)
        endDate = sdf.format(cal.time)
        tvDateRange?.text = "$startDate -> $endDate"

        view.findViewById<Button>(R.id.btnSelectStart)?.setOnClickListener {
            DatePickerDialog(requireContext(), { _, y, m, d ->
                val c = Calendar.getInstance()
                c.set(y, m, d)
                startDate = sdf.format(c.time)
                tvDateRange?.text = "$startDate -> $endDate"
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        view.findViewById<Button>(R.id.btnSelectEnd)?.setOnClickListener {
            DatePickerDialog(requireContext(), { _, y, m, d ->
                val c = Calendar.getInstance()
                c.set(y, m, d)
                endDate = sdf.format(c.time)
                tvDateRange?.text = "$startDate -> $endDate"
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        btnCreate?.setOnClickListener {
            if (startDate.isEmpty() || endDate.isEmpty()) {
                Toast.makeText(requireContext(), "حدد الفترة أولاً", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val progressBar = view.findViewById<ProgressBar>(R.id.progressInvoices)
            progressBar?.visibility = View.VISIBLE
            btnCreate.isEnabled = false

            lifecycleScope.launch {
                val result = repo.createInvoice(startDate, endDate)
                progressBar?.visibility = View.GONE
                btnCreate.isEnabled = true

                result.onSuccess { resp ->
                    if (resp.success && resp.invoice != null) {
                        AlertDialog.Builder(requireContext())
                            .setTitle("تم إنشاء الفاتورة")
                            .setMessage("رقم الفاتورة: ${resp.invoice.invoiceNumber}\nالإجمالي: ${resp.invoice.grandTotal} ر.س\nضريبة القيمة المضافة: ${resp.invoice.vatAmount} ر.س")
                            .setPositiveButton("طباعة") { _, _ ->
                                activity?.let { act ->
                                    PrintManager.printInvoice(act, resp.invoice)
                                }
                            }
                            .setNegativeButton("إغلاق", null)
                            .show()
                        loadInvoices(view)
                    } else {
                        Toast.makeText(requireContext(), resp.message ?: "خطأ", Toast.LENGTH_SHORT).show()
                    }
                }.onFailure {
                    Toast.makeText(requireContext(), "خطأ في الاتصال", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showInvoiceDetail(invoice: Invoice) {
        AlertDialog.Builder(requireContext())
            .setTitle("فاتورة: ${invoice.invoiceNumber}")
            .setMessage(
                "الفترة: ${invoice.periodStart} - ${invoice.periodEnd}\n" +
                "عدد الغسيلات: ${invoice.totalWashes}\n" +
                "الإجمالي: ${invoice.totalAmount} ر.س\n" +
                "ضريبة 15%: ${invoice.vatAmount} ر.س\n" +
                "المجموع الكلي: ${invoice.grandTotal} ر.س\n" +
                "الحالة: ${when(invoice.status) { "issued" -> "صادرة"; "paid" -> "مدفوعة"; else -> "مسودة" }}"
            )
            .setPositiveButton("طباعة") { _, _ ->
                activity?.let { act -> PrintManager.printInvoice(act, invoice) }
            }
            .setNegativeButton("إغلاق", null)
            .show()
    }
}
