package com.njm.worker.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.RadioGroup
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

        btnPrintReport.setOnClickListener {
            val isDaily = rgReportType.checkedRadioButtonId == R.id.rbToday
            if (isDaily) printDailyReport() else printMonthlyReport()
        }
    }

    private fun printDailyReport() {
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
                PrintManager.printDailyReport(act, washes, total, paid, unpaid, "اليوم")
            }.also {
                repo.getTodayWashes().onFailure {
                    Toast.makeText(requireContext(), "خطأ في تحميل البيانات", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun printMonthlyReport() {
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
                PrintManager.printDailyReport(act, washes, total, paid, unpaid, "الشهر")
            }
        }
    }
}
