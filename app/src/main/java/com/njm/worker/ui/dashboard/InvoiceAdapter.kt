package com.njm.worker.ui.dashboard

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.njm.worker.R
import com.njm.worker.data.models.Invoice

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
                        val invoice = invoices[position]

                        holder.tvNumber.text = "#${invoice.invoice_number ?: invoice.id}"

                        val statusLabel = when (invoice.status) {
                                      "paid" -> "\u0645\u062f\u0641\u0648\u0639"
                                      "issued" -> "\u0645\u0635\u062f\u0631"
                                      "draft" -> "\u0645\u0633\u0648\u062f\u0629"
                                      else -> invoice.status ?: "unknown"
                        }
                                holder.tvStatus.text = statusLabel

                        when (invoice.status) {
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

                                val start = invoice.period_start ?: ""
                        val end = invoice.period_end ?: ""
                        holder.tvPeriod.text = if (start.isNotEmpty() && end.isNotEmpty()) "$start - $end" else ""

                        val washCount = invoice.wash_count ?: invoice.washes_count ?: 0
                        holder.tvWashes.text = "$washCount"

                        val total = invoice.total_amount ?: invoice.total ?: 0.0
                        holder.tvTotal.text = String.format("%.2f SAR", total)

                                val vat = invoice.vat_amount ?: invoice.vat ?: 0.0
                        holder.tvVat.text = String.format("%.2f SAR", vat)

                                holder.tvDate.text = invoice.created_at ?: invoice.invoice_date ?: ""

                        holder.btnPrint.setOnClickListener { onPrint(invoice) }
                                holder.itemView.setOnClickListener { onClick(invoice) }
              }

                  override fun getItemCount(): Int = invoices.size

      fun updateList(newInvoices: List<Invoice>) {
                invoices.clear()
                        invoices.addAll(newInvoices)
                                notifyDataSetChanged()
      }
}
