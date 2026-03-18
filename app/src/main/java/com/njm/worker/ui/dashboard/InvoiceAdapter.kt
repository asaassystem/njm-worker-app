package com.njm.worker.ui.dashboard

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.njm.worker.R
import com.njm.worker.data.model.Invoice

class InvoiceAdapter(
          private val invoices: MutableList<Invoice>,
          private val onPrint: (Invoice) -> Unit,
          private val onClick: (Invoice) -> Unit
      ) : RecyclerView.Adapter<InvoiceAdapter.ViewHolder>() {

          inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
                        val tvNumber: TextView = view.findViewById(R.id.tvInvoiceNumber)
                                val tvStatus: TextView = view.findViewById(R.id.tvInvoiceStatus)
                                        val tvPeriod: TextView = view.findViewById(R.id.tvInvoicePeriod)
                                                val tvWashes: TextView = view.findViewById(R.id.tvInvoiceWashes)
                                                        val tvTotal: TextView = view.findViewById(R.id.tvInvoiceTotal)
                                                                val tvVat: TextView = view.findViewById(R.id.tvInvoiceVat)
                                                                        val tvDate: TextView = view.findViewById(R.id.tvInvoiceDate)
                                                                                val btnPrint: Button = view.findViewById(R.id.btnPrintInvoice)
          }

              override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
                            val view = LayoutInflater.from(parent.context)
                                        .inflate(R.layout.item_invoice, parent, false)
                                                return ViewHolder(view)
              }

                  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
                                val inv = invoices[position]

                                holder.tvNumber.text = "#${inv.invoiceNumber}"

                                val statusLabel = when (inv.status) {
                                                  "paid" -> "\u0645\u062f\u0641\u0648\u0639"
                                                  "issued" -> "\u0645\u0635\u062f\u0631"
                                                  "draft" -> "\u0645\u0633\u0648\u062f\u0629"
                                                  else -> inv.status ?: "draft"
                                }
                                        holder.tvStatus.text = statusLabel

                                when (inv.status) {
                                                  "paid" -> {
                                                                        holder.tvStatus.setBackgroundColor(Color.parseColor("#E8F5EC"))
                                                                                        holder.tvStatus.setTextColor(Color.parseColor("#1B6B35"))
                                                  }
                                                              "issued" -> {
                                                                                    holder.tvStatus.setBackgroundColor(Color.parseColor("#E3F2FD"))
                                                                                                    holder.tvStatus.setTextColor(Color.parseColor("#0D47A1"))
                                                              }
                                                                          else -> {
                                                                                                holder.tvStatus.setBackgroundColor(Color.parseColor("#F5F5F5"))
                                                                                                                holder.tvStatus.setTextColor(Color.parseColor("#757575"))
                                                                          }
                                }

                                        val start = inv.periodStart ?: ""
                                val end = inv.periodEnd ?: ""
                                holder.tvPeriod.text = if (start.isNotEmpty() && end.isNotEmpty()) "$start - $end" else ""

                                holder.tvWashes.text = "${inv.totalWashes ?: 0}"

                                val total = inv.grandTotal ?: inv.totalAmount ?: 0.0
                                holder.tvTotal.text = String.format("%.2f SAR", total)

                                        val vat = inv.vatAmount ?: 0.0
                                holder.tvVat.text = String.format("%.2f SAR", vat)

                                        holder.tvDate.text = inv.invoiceDate ?: inv.createdAt ?: ""

                                holder.btnPrint.setOnClickListener { onPrint(inv) }
                                        holder.itemView.setOnClickListener { onClick(inv) }
                  }

                      override fun getItemCount(): Int = invoices.size

          fun updateList(newInvoices: List<Invoice>) {
                        invoices.clear()
                                invoices.addAll(newInvoices)
                                        notifyDataSetChanged()
          }
}
