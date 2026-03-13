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

class MonthFragment : Fragment() {
    private val repo = WorkerRepository()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_month, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadWashes(view)
    }

    override fun onResume() { super.onResume(); view?.let { loadWashes(it) } }

    private fun loadWashes(view: View) {
        val rv = view.findViewById<RecyclerView>(R.id.rvMonthWashes)
        val tvEmpty = view.findViewById<TextView>(R.id.tvNoMonthWashes)
        val tvTotal = view.findViewById<TextView>(R.id.tvMonthTotal)
        val tvCount = view.findViewById<TextView>(R.id.tvMonthCount)
        val tvPaid = view.findViewById<TextView>(R.id.tvMonthPaid)
        val tvUnpaid = view.findViewById<TextView>(R.id.tvMonthUnpaid)
        rv.layoutManager = LinearLayoutManager(requireContext())
        lifecycleScope.launch {
            repo.getMonthWashes().onSuccess { data ->
                val washes = data.washes ?: emptyList()
                tvEmpty.visibility = if (washes.isEmpty()) View.VISIBLE else View.GONE
                rv.visibility = if (washes.isEmpty()) View.GONE else View.VISIBLE
                val total = washes.sumOf { it.cost ?: 0.0 }
                val paidAmt = washes.filter { (it.isPaid ?: 1) == 1 }.sumOf { it.cost ?: 0.0 }
                val unpaidAmt = washes.filter { (it.isPaid ?: 1) == 0 }.sumOf { it.cost ?: 0.0 }
                tvTotal?.text = String.format("%.0f ر.س", total)
                tvCount?.text = washes.size.toString() + " غسيلة"
                tvPaid?.text = String.format("%.0f ر.س", paidAmt)
                tvUnpaid?.text = String.format("%.0f ر.س", unpaidAmt)
                if (washes.isNotEmpty()) {
                    rv.adapter = WashRecordAdapter(washes,
                        onPrint = { wash ->
                            (activity as? DashboardActivity)?.let { PrintManager.printWashReceipt(it, wash, it) }
                        },
                        onTogglePaid = { wash -> togglePayment(wash) }
                    )
                }
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
            .setNegativeButton("إلغاء", null).show()
    }
}
