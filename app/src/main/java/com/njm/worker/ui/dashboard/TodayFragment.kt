package com.njm.worker.ui.dashboard

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.njm.worker.R
import com.njm.worker.data.model.WashRecord
import com.njm.worker.data.repository.WorkerRepository
import kotlinx.coroutines.launch

class TodayFragment : Fragment() {
    private val repo = WorkerRepository()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_today, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadWashes(view)
    }

    override fun onResume() { super.onResume(); view?.let { loadWashes(it) } }

    private fun loadWashes(view: View) {
        val rv = view.findViewById<RecyclerView>(R.id.rvTodayWashes)
        val tvEmpty = view.findViewById<TextView>(R.id.tvNoWashes)
        val tvTotal = view.findViewById<TextView>(R.id.tvTodayTotal)
        val tvPaid = view.findViewById<TextView>(R.id.tvTodayPaid)
        val tvUnpaid = view.findViewById<TextView>(R.id.tvTodayUnpaid)
        rv.layoutManager = LinearLayoutManager(requireContext())
        lifecycleScope.launch {
            repo.getTodayWashes().onSuccess { data ->
                val washes = data.washes ?: emptyList()
                tvEmpty.visibility = if (washes.isEmpty()) View.VISIBLE else View.GONE
                rv.visibility = if (washes.isEmpty()) View.GONE else View.VISIBLE
                val total = washes.sumOf { it.cost ?: 0.0 }
                val paid = washes.filter { (it.isPaid ?: 1) == 1 }.sumOf { it.cost ?: 0.0 }
                val unpaid = washes.filter { (it.isPaid ?: 1) == 0 }.sumOf { it.cost ?: 0.0 }
                tvTotal?.text = String.format("%.0f ر.س", total)
                tvPaid?.text = String.format("%.0f ر.س", paid)
                tvUnpaid?.text = String.format("%.0f ر.س", unpaid)
                if (washes.isNotEmpty()) {
                    rv.adapter = WashRecordAdapter(washes,
                        onPrint = { wash ->
                            (activity as? DashboardActivity)?.let { PrintManager.printWashReceipt(it, wash, it) }
                        },
                        onTogglePaid = { wash -> togglePayment(wash) }
                    )
                }
                (activity as? DashboardActivity)?.loadStats()
            }.also {
                repo.getTodayWashes().onFailure { tvEmpty.visibility = View.VISIBLE }
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
                    view?.let { loadWashes(it) }
                }
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }
}
