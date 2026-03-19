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
 * MonthFragment v6.4
 * Fixed: stats (tvMonthTotal, tvMonthCount, tvMonthPaid, tvMonthUnpaid) now correctly updated from API
 * Fixed: worker name passed to receipt printing
 * Fixed: Arabic error messages
 */
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
                    isLoading = false
                    progress?.visibility = View.GONE

                    val washes = data.washes ?: emptyList()
                    val total = data.total ?: washes.sumOf { it.cost ?: 0.0 }
                    val paidAmt = data.paid ?: washes.filter { (it.isPaid ?: 1) == 1 }.sumOf { it.cost ?: 0.0 }
                    val unpaidAmt = data.unpaid ?: washes.filter { (it.isPaid ?: 1) == 0 }.sumOf { it.cost ?: 0.0 }
                    val count = data.count ?: washes.size

                    // Update stats card - THIS was missing before
                    tvTotal?.text = String.format("%.0f \u0631.\u0633", total)
                    tvCount?.text = count.toString()
                    tvPaid?.text = String.format("%.0f", paidAmt)
                    tvUnpaid?.text = String.format("%.0f", unpaidAmt)

                    if (washes.isEmpty()) {
                        tvEmpty?.text = "\u0644\u0627 \u062a\u0648\u062c\u062f \u063a\u0633\u064a\u0644\u0627\u062a \u0647\u0630\u0627 \u0627\u0644\u0634\u0647\u0631"
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
                    tvEmpty?.text = "\u062e\u0637\u0623 \u0641\u064a \u0627\u0644\u0627\u062a\u0635\u0627\u0644 \u0628\u0627\u0644\u062e\u0627\u062f\u0645"
                    tvEmpty?.visibility = View.VISIBLE
                    if (isAdded) Toast.makeText(requireContext(), "\u062e\u0637\u0623 \u0641\u064a \u062a\u062d\u0645\u064a\u0644 \u0628\u064a\u0627\u0646\u0627\u062a \u0627\u0644\u0634\u0647\u0631", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                isLoading = false
                progress?.visibility = View.GONE
                tvEmpty?.text = "\u062e\u0637\u0623 \u0641\u064a \u0627\u0644\u0627\u062a\u0635\u0627\u0644"
                tvEmpty?.visibility = View.VISIBLE
            }
        }
    }

    private fun togglePayment(wash: WashRecord) {
        val newPaid = if ((wash.isPaid ?: 1) == 1) 0 else 1
        val msg = if (newPaid == 1) "\u062a\u062d\u062f\u064a\u062f \u0643\u0645\u062f\u0641\u0648\u0639\u061f" else "\u062a\u062d\u062f\u064a\u062f \u0643\u063a\u064a\u0631 \u0645\u062f\u0641\u0648\u0639\u061f"
        AlertDialog.Builder(requireContext())
            .setTitle("\u062a\u062d\u062f\u064a\u062b \u062d\u0627\u0644\u0629 \u0627\u0644\u062f\u0641\u0639")
            .setMessage(msg)
            .setPositiveButton("\u0646\u0639\u0645") { _, _ ->
                lifecycleScope.launch {
                    repo.updatePayment(wash.id, newPaid)
                    isLoading = false
                    view?.let { loadWashes(it) }
                }
            }
            .setNegativeButton("\u0625\u0644\u063a\u0627\u0621", null).show()
    }
}
