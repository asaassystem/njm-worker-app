package com.njm.worker.ui.dashboard

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.njm.worker.R
import com.njm.worker.data.model.Invoice
import com.njm.worker.data.repository.WorkerRepository
import com.njm.worker.printer.PrintManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class InvoicesFragment : Fragment() {

        private val repo = WorkerRepository()
            private val invoices = mutableListOf<Invoice>()
                private lateinit var adapter: InvoiceAdapter

        override fun onCreateView(
                    inflater: LayoutInflater,
                    container: ViewGroup?,
                    savedInstanceState: Bundle?
                    ): View? {
                    return inflater.inflate(R.layout.fragment_invoices, container, false)
        }

            override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
                        super.onViewCreated(view, savedInstanceState)
                                setupRecyclerView(view)
                                        loadInvoices(view)
                                                setupCreateInvoice(view)
            }

                private fun setupRecyclerView(view: View) {
                            val rv = view.findViewById<RecyclerView>(R.id.rvInvoices)
                                    adapter = InvoiceAdapter(
                                                    invoices,
                                                    onPrint = { invoice -> printInvoice(invoice) },
                                                    onClick = { invoice -> showInvoiceDetail(invoice) }
                                                            )
                                            rv?.layoutManager = LinearLayoutManager(requireContext())
                                                    rv?.adapter = adapter
                }

                    private fun loadInvoices(view: View) {
                                val progressBar = view.findViewById<ProgressBar>(R.id.progressInvoices)
                                        val tvEmpty = view.findViewById<TextView>(R.id.tvNoInvoices)
                                                val rv = view.findViewById<RecyclerView>(R.id.rvInvoices)
                                                        progressBar?.visibility = View.VISIBLE
                                lifecycleScope.launch {
                                                val result = repo.getInvoices()
                                                            progressBar?.visibility = View.GONE
                                                result.onSuccess { resp ->
                                                                    invoices.clear()
                                                                                    invoices.addAll(resp.invoices ?: emptyList())
                                                                                                    adapter.notifyDataSetChanged()
                                                                                                                    if (invoices.isEmpty()) {
                                                                                                                                            tvEmpty?.visibility = View.VISIBLE
                                                                                                                                            rv?.visibility = View.GONE
                                                                                                                    } else {
                                                                                                                                            tvEmpty?.visibility = View.GONE
                                                                                                                                            rv?.visibility = View.VISIBLE
                                                                                                                    }
                                                }.onFailure {
                                                                    tvEmpty?.text = "\u062e\u0637\u0623 \u0641\u064a \u062a\u062d\u0645\u064a\u0644 \u0627\u0644\u0641\u0648\u0627\u062a\u064a\u0631"
                                                                    tvEmpty?.visibility = View.VISIBLE
                                                                    rv?.visibility = View.GONE
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
                                                                                        Toast.makeText(requireContext(), "\u062d\u062f\u062f \u0627\u0644\u0641\u062a\u0631\u0629 \u0623\u0648\u0644\u0627\u064b", Toast.LENGTH_SHORT).show()
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
                                                                                                                                                                        .setTitle("\u062a\u0645 \u0625\u0646\u0634\u0627\u0621 \u0627\u0644\u0641\u0627\u062a\u0648\u0631\u0629")
                                                                                                                                                                                                    .setMessage(
                                                                                                                                                                                                                                        "\u0631\u0642\u0645 \u0627\u0644\u0641\u0627\u062a\u0648\u0631\u0629: ${resp.invoice.invoiceNumber}\n" +
                                                                                                                                                                                                                                            "\u0627\u0644\u0645\u062c\u0645\u0648\u0639: ${resp.invoice.grandTotal} \u0631.\u0633\n" +
                                                                                                                                                                                                                                            "\u0636\u0631\u064a\u0628\u0629: ${resp.invoice.vatAmount} \u0631.\u0633"
                                                                                                                                                                                                                                    )
                                                                                                                                                                                                                                .setPositiveButton("\u0637\u0628\u0627\u0639\u0629") { _, _ ->
                                                                                                                                                                                                                                                                    printInvoice(resp.invoice)
                                                                                                                                                                                                                                                                                                }
                                                                                                                                                                                                                                                            .setNegativeButton("\u0625\u063a\u0644\u0627\u0642", null)
                                                                                                                                                                                                                                                                                        .show()
                                                                                                                                                                                                                                                                                                                loadInvoices(view)
                                                                                                                                                                                                                                                                                                                                    } else {
                                                                                                                                            Toast.makeText(requireContext(), resp.message ?: "\u062e\u0637\u0623", Toast.LENGTH_SHORT).show()
                                                                                                                }
                                                                                        }.onFailure {
                                                                                                                Toast.makeText(requireContext(), "\u062e\u0637\u0623 \u0641\u064a \u0627\u0644\u0627\u062a\u0635\u0627\u0644", Toast.LENGTH_SHORT).show()
                                                                                        }
                                                                    }
                                                    }
                        }

                            private fun printInvoice(invoice: Invoice) {
                                        activity?.let { act ->
                                                        PrintManager.printInvoice(act, invoice)
                                        }
                            }

                                private fun showInvoiceDetail(invoice: Invoice) {
                                            AlertDialog.Builder(requireContext())
                                                        .setTitle("\u0641\u0627\u062a\u0648\u0631\u0629: ${invoice.invoiceNumber}")
                                                                    .setMessage(
                                                                                        "\u0627\u0644\u0641\u062a\u0631\u0629: ${invoice.periodStart} - ${invoice.periodEnd}\n" +
                                                                                            "\u0639\u062f\u062f \u0627\u0644\u063a\u0633\u064a\u0644\u0627\u062a: ${invoice.totalWashes}\n" +
                                                                                            "\u0627\u0644\u0625\u062c\u0645\u0627\u0644\u064a: ${invoice.totalAmount} \u0631.\u0633\n" +
                                                                                            "\u0636\u0631\u064a\u0628\u0629 15%: ${invoice.vatAmount} \u0631.\u0633\n" +
                                                                                            "\u0627\u0644\u0645\u062c\u0645\u0648\u0639 \u0627\u0644\u0643\u0644\u064a: ${invoice.grandTotal} \u0631.\u0633\n" +
                                                                                            "\u0627\u0644\u062d\u0627\u0644\u0629: ${when(invoice.status) {
                                                                                            "issued" -> "\u0635\u0627\u062f\u0631\u0629"
                                                                                            "paid" -> "\u0645\u062f\u0641\u0648\u0639\u0629"
                                                                                            else -> "\u0645\u0633\u0648\u062f\u0629"
                                                                        }}"
                                                                                    )
                                                                                .setPositiveButton("\u0637\u0628\u0627\u0639\u0629") { _, _ ->
                                                                                                    printInvoice(invoice)
                                                                                }
                                                                                            .setNegativeButton("\u0625\u063a\u0644\u0627\u0642", null)
                                                                                                        .show()
                                }
}
