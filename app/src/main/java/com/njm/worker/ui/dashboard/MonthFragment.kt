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

class MonthFragment : Fragment() {
        private val repo = WorkerRepository()
            private var isLoading = false
        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
                    return inflater.inflate(R.layout.fragment_month, container, false)
        }
            override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
                        super.onViewCreated(view, savedInstanceState)
                                loadWashes(view)
            }
                override fun onResume() {
                            super.onResume()
                                    if (!isLoading) { view?.let { loadWashes(it) } }
                }
                    private fun loadWashes(view: View) {
                                if (isLoading) return
                                isLoading = true
                                val rv = view.findViewById<RecyclerView>(R.id.rvMonthWashes)
                                        val tvEmpty = view.findViewById<TextView>(R.id.tvNoMonthWashes)
                                                val tvTotal = view.findViewById<TextView>(R.id.tvMonthTotal)
                                                        val tvCount = view.findViewById<TextView>(R.id.tvMonthCount)
                                                                val tvPaid = view.findViewById<TextView>(R.id.tvMonthPaid)
                                                                        val tvUnpaid = view.findViewById<TextView>(R.id.tvMonthUnpaid)
                                                                                val progress = view.findViewById<ProgressBar>(R.id.progressMonth)
                                                                                        progress?.visibility = View.VISIBLE
                                rv?.visibility = View.GONE
                                tvEmpty?.visibility = View.GONE
                                rv?.layoutManager = LinearLayoutManager(requireContext())
                                        val workerName = SessionManager.getWorkerName(requireContext()) ?: ""
                                lifecycleScope.launch {
                                                try {
                                                                    repo.getMonthWashes().onSuccess { data ->
                                                                                            isLoading = false; progress?.visibility = View.GONE
                                                                                            val washes = data.washes ?: emptyList()
                                                                                                                val total = data.total ?: washes.sumOf { it.cost ?: 0.0 }
                                                                                                                                    val paidAmt = data.paid ?: washes.filter { (it.isPaid ?: 1) == 1 }.sumOf { it.cost ?: 0.0 }
                                                                                                                                                        val unpaidAmt = data.unpaid ?: washes.filter { (it.isPaid ?: 1) == 0 }.sumOf { it.cost ?: 0.0 }
                                                                                                                                                                            if (washes.isEmpty()) {
                                                                                                                                                                                                        tvEmpty?.visibility = View.VISIBLE; rv?.visibility = View.GONE
                                                                                                                                                                            } else {
                                                                                                                                                                                                        tvEmpty?.visibility = View.GONE; rv?.visibility = View.VISIBLE
                                                                                                                                                                                                        rv?.adapter = WashRecordAdapter(washes,
                                                                                                                                                                                                                                                                    onPrint = { wash -> (activity as? DashboardActivity)?.let { PrintManager.printWashReceipt(it, wash, workerName) } },
                                                                                                                                                                                                                                                                    onTogglePaid = { wash -> togglePayment(wash) })
                                                                                                                                                                                                                            }
                                                                    }.onFailure {
                                                                                            isLoading = false; progress?.visibility = View.GONE; tvEmpty?.visibility = View.VISIBLE
                                                                                            if (isAdded) Toast.makeText(requireContext(), "Connection error", Toast.LENGTH_SHORT).show()
                                                                    }
                                                } catch (e: Exception) { isLoading = false; progress?.visibility = View.GONE; tvEmpty?.visibility = View.VISIBLE }
                                }
                    }
                        private fun togglePayment(wash: WashRecord) {
                                    val newPaid = if ((wash.isPaid ?: 1) == 1) 0 else 1
                                    AlertDialog.Builder(requireContext())
                                                .setTitle("Update Payment")
                                                            .setMessage(if (newPaid == 1) "Mark as paid?" else "Mark as unpaid?")
                                                                        .setPositiveButton("Yes") { _, _ -> lifecycleScope.launch { repo.updatePayment(wash.id, newPaid); isLoading = false; view?.let { loadWashes(it) } } }
                                                                                    .setNegativeButton("Cancel", null).show()
                        }
}
