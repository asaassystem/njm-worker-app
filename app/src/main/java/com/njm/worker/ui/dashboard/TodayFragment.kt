package com.njm.worker.ui.dashboard

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.njm.worker.R
import com.njm.worker.data.model.WashRecord
import com.njm.worker.data.repository.WorkerRepository
import com.njm.worker.printer.PrintManager
import com.njm.worker.utils.SessionManager
import kotlinx.coroutines.launch

/**
 * TodayFragment v6.0
  * Fixes: double-load bug, worker name on receipts, error feedback, loading state
   * Developer: meshari.tech
    */
class TodayFragment : Fragment() {

        private val repo = WorkerRepository()
            private var isLoading = false

        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
                    return inflater.inflate(R.layout.fragment_today, container, false)
        }

            override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
                        super.onViewCreated(view, savedInstanceState)
                                loadWashes(view)
            }

                override fun onResume() {
                            super.onResume()
                                    if (!isLoading) {
                                                    view?.let { loadWashes(it) }
                                    }
                }

                    fun forceRefresh() {
                                isLoading = false
                                view?.let { loadWashes(it) }
                    }

                        private fun loadWashes(view: View) {
                                    if (isLoading) return
                                    isLoading = true

                                    val rv = view.findViewById<RecyclerView>(R.id.rvTodayWashes)
                                            val tvEmpty = view.findViewById<TextView>(R.id.tvNoWashes)
                                                    val tvTotal = view.findViewById<TextView>(R.id.tvTodayTotal)
                                                            val tvPaid = view.findViewById<TextView>(R.id.tvTodayPaid)
                                                                    val tvUnpaid = view.findViewById<TextView>(R.id.tvTodayUnpaid)
                                                                            val progress = view.findViewById<ProgressBar>(R.id.progressToday)

                                                                                    progress?.visibility = View.VISIBLE
                                    rv?.visibility = View.GONE
                                    tvEmpty?.visibility = View.GONE
                                    rv?.layoutManager = LinearLayoutManager(requireContext())

                                            val workerName = SessionManager.getWorkerName(requireContext()) ?: ""

                                    lifecycleScope.launch {
                                                    try {
                                                                        repo.getTodayWashes().onSuccess { data ->
                                                                                                isLoading = false
                                                                                                progress?.visibility = View.GONE
                                                                                                val washes = data.washes ?: emptyList()
                                                                                                                    val total = data.total ?: washes.sumOf { it.cost ?: 0.0 }
                                                                                                                                        val paid = data.paid ?: washes.filter { (it.isPaid ?: 1) == 1 }.sumOf { it.cost ?: 0.0 }
                                                                                                                                                            val unpaid = data.unpaid ?: washes.filter { (it.isPaid ?: 1) == 0 }.sumOf { it.cost ?: 0.0 }
                                                                                                                                                                                tvTotal?.text = String.format("%.0f ر.س", total)
                                                                                                                                                                                                    tvPaid?.text = String.format("%.0f ر.س", paid)
                                                                                                                                                                                                                        tvUnpaid?.text = String.format("%.0f ر.س", unpaid)
                                                                                                                                                                                                                                            if (washes.isEmpty()) {
                                                                                                                                                                                                                                                                        tvEmpty?.text = "لا توجد غسيلات اليوم"
                                                                                                                                                                                                                                                                        tvEmpty?.visibility = View.VISIBLE
                                                                                                                                                                                                                                                                        rv?.visibility = View.GONE
                                                                                                                                                                                                                                                                    } else {
                                                                                                                                                                                                                                                                        tvEmpty?.visibility = View.GONE
                                                                                                                                                                                                                                                                        rv?.visibility = View.VISIBLE
                                                                                                                                                                                                                                                                        rv?.adapter = WashRecordAdapter(
                                                                                                                                                                                                                                                                                                        washes,
                                                                                                                                                                                                                                                                                                        onPrint = { wash ->
                                                                                                                                                                                                                                                                                                                                            (activity as? DashboardActivity)?.let {
                                                                                                                                                                                                                                                                                                                                                                                    PrintManager.printWashReceipt(it, wash, workerName)
                                                                                                                                                                                                                                                                                                                                                                                                                    }
                                                                                                                                                                                                                                                                                                                                                                        },
                                                                                                                                                                                                                                                                                                        onTogglePaid = { wash -> togglePayment(wash) }
                                                                                                                                                                                                                                                                                                                                )
                                                                                                                                                                                                                                                                                            }
                                                                                                                                                                                                                                                            }.onFailure {
                                                                                                isLoading = false
                                                                                                progress?.visibility = View.GONE
                                                                                                tvEmpty?.text = "خطأ في الاتصال بالخادم"
                                                                                                tvEmpty?.visibility = View.VISIBLE
                                                                                                if (isAdded) Toast.makeText(requireContext(), "خطأ في تحميل بيانات اليوم", Toast.LENGTH_SHORT).show()
                                                                        }
                                                    } catch (e: Exception) {
                                                                        isLoading = false
                                                                        progress?.visibility = View.GONE
                                                                        tvEmpty?.text = "خطأ في الاتصال"
                                                                        tvEmpty?.visibility = View.VISIBLE
                                                    }
                                    }
                        }

                            private fun togglePayment(wash: WashRecord) {
                                        val newPaid = if ((wash.isPaid ?: 1) == 1) 0 else 1
                                        val msg = if (newPaid == 1) "تحديد كمدفوع؟" else "تحديد كغير مدفوع؟"
                                        AlertDialog.Builder(requireContext())
                                                    .setTitle("تحديث حالة الدفع")
                                                                .setMessage(msg)
                                                                            .setPositiveButton("نعم") { _, _ ->
                                                                                                lifecycleScope.launch {
                                                                                                                        repo.updatePayment(wash.id, newPaid)
                                                                                                                                            isLoading = false
                                                                                                                        view?.let { loadWashes(it) }
                                                                                                }
                                                                            }
                                                                                        .setNegativeButton("إلغاء", null).show()
                            }
}
