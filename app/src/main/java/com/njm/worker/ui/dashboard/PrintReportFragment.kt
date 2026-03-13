package com.njm.worker.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.njm.worker.R
import com.njm.worker.data.repository.WorkerRepository
import kotlinx.coroutines.launch

class PrintReportFragment : Fragment() {
    private val repo = WorkerRepository()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_print_report, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rgReportType = view.findViewById<RadioGroup>(R.id.rgReportType)
        val btnPrintReport = view.findViewById<Button>(R.id.btnPrintReport)
        val tvCount = view.findViewById<TextView>(R.id.tvReportCount)
        val tvTotal = view.findViewById<TextView>(R.id.tvReportTotal)
        val tvPaid = view.findViewById<TextView>(R.id.tvReportPaid)
        val tvUnpaid = view.findViewById<TextView>(R.id.tvReportUnpaid)
        val cardSummary = view.findViewById<View>(R.id.cardSummary)

        btnPrintReport.setOnClickListener {
            val isDaily = rgReportType.checkedRadioButtonId == R.id.rbToday
            if (isDaily) printDailyReport(tvCount, tvTotal, tvPaid, tvUnpaid, cardSummary)
            else printMonthlyReport(tvCount, tvTotal, tvPaid, tvUnpaid, cardSummary)
        }
    }

    private fun printDailyReport(tvCount: TextView?, tvTotal: TextView?, tvPaid: TextView?, tvUnpaid: TextView?, cardSummary: View?) {
        lifecycleScope.launch {
            repo.getTodayWashes().onSuccess { data ->
                val washes = data.washes ?: emptyList()
                if (washes.isEmpty()) {
                    Toast.makeText(requireContext(), "لا توجد غسيلات اليوم", Toast.LENGTH_SHORT).show()
                    return@onSuccess
                }
                val act = activity ?: return@onSuccess
                val total = washes.sumOf { it.cost ?: 0.0 }
                val paid = washes.filter { (it.isPaid ?: 1) == 1 }.sumOf { it.cost ?: 0.0 }
                val unpaid = washes.filter { (it.isPaid ?: 1) == 0 }.sumOf { it.cost ?: 0.0 }
                // Update summary card
                tvCount?.text = washes.size.toString()
                tvTotal?.text = String.format("%.2f", total)
                tvPaid?.text = String.format("%.2f", paid)
                tvUnpaid?.text = String.format("%.2f", unpaid)
                cardSummary?.visibility = View.VISIBLE
                PrintManager.printDailyReport(act, washes, total, paid, unpaid)
            }.also {
                repo.getTodayWashes().onFailure {
                    Toast.makeText(requireContext(), "خطأ في تحميل البيانات", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun printMonthlyReport(tvCount: TextView?, tvTotal: TextView?, tvPaid: TextView?, tvUnpaid: TextView?, cardSummary: View?) {
        lifecycleScope.launch {
            repo.getMonthWashes().onSuccess { data ->
                val washes = data.washes ?: emptyList()
                if (washes.isEmpty()) {
                    Toast.makeText(requireContext(), "لا توجد غسيلات هذا الشهر", Toast.LENGTH_SHORT).show()
                    return@onSuccess
                }
                val act = activity ?: return@onSuccess
                val total = washes.sumOf { it.cost ?: 0.0 }
                val paid = washes.filter { (it.isPaid ?: 1) == 1 }.sumOf { it.cost ?: 0.0 }
                val unpaid = washes.filter { (it.isPaid ?: 1) == 0 }.sumOf { it.cost ?: 0.0 }
                // Update summary card
                tvCount?.text = washes.size.toString()
                tvTotal?.text = String.format("%.2f", total)
                tvPaid?.text = String.format("%.2f", paid)
                tvUnpaid?.text = String.format("%.2f", unpaid)
                cardSummary?.visibility = View.VISIBLE
                PrintManager.printMonthlyReport(act, washes, total, paid, unpaid)
            }
        }
    }
}
