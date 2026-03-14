package com.njm.worker.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.njm.worker.R
import com.njm.worker.data.ApiClient
import com.njm.worker.data.model.WashRecord
import com.njm.worker.printer.PrinterManager
import com.njm.worker.utils.SessionManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * TodayFragment - Today's wash records
 * Navy/Gold design with print support
 * Developer: meshari.tech
 */
class TodayFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmpty: TextView
    private lateinit var tvTotalCount: TextView
    private lateinit var tvTotalAmount: TextView
    private lateinit var btnPrintSummary: Button
    private lateinit var adapter: WashRecordAdapter

    private val washRecords = mutableListOf<WashRecord>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_today, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)
        setupRecyclerView()
        loadTodayRecords()
    }

    private fun initViews(view: View) {
        recyclerView = view.findViewById(R.id.recycler_view)
        progressBar = view.findViewById(R.id.progress_bar)
        tvEmpty = view.findViewById(R.id.tv_empty)
        tvTotalCount = view.findViewById(R.id.tv_total_count)
        tvTotalAmount = view.findViewById(R.id.tv_total_amount)
        btnPrintSummary = view.findViewById(R.id.btn_print_summary)

        btnPrintSummary.setOnClickListener { printDailySummary() }
    }

    private fun setupRecyclerView() {
        adapter = WashRecordAdapter(washRecords) { record ->
            // On item click - reprint receipt
            context?.let { ctx ->
                PrinterManager.printWashReceipt(ctx, record)
                Toast.makeText(ctx, R.string.printing, Toast.LENGTH_SHORT).show()
            }
        }
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
    }

    private fun loadTodayRecords() {
        showLoading(true)
        val session = SessionManager(requireContext())
        val workerId = session.getWorkerId()

        ApiClient.apiService.getTodayWashes(workerId).enqueue(object : Callback<List<WashRecord>> {
            override fun onResponse(call: Call<List<WashRecord>>, response: Response<List<WashRecord>>) {
                if (!isAdded) return
                showLoading(false)
                val records = response.body() ?: emptyList()
                washRecords.clear()
                washRecords.addAll(records)
                adapter.notifyDataSetChanged()
                updateStats()

                if (records.isEmpty()) {
                    tvEmpty.visibility = View.VISIBLE
                    recyclerView.visibility = View.GONE
                } else {
                    tvEmpty.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE
                }
            }

            override fun onFailure(call: Call<List<WashRecord>>, t: Throwable) {
                if (!isAdded) return
                showLoading(false)
                Toast.makeText(requireContext(), R.string.connection_error, Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun updateStats() {
        tvTotalCount.text = getString(R.string.total_washes_count, washRecords.size)
        val total = washRecords.sumOf { it.price ?: 0.0 }
        tvTotalAmount.text = getString(R.string.total_amount, total)
    }

    private fun printDailySummary() {
        context?.let { ctx ->
            PrinterManager.printDailySummary(ctx, washRecords)
            Toast.makeText(ctx, R.string.printing_summary, Toast.LENGTH_SHORT).show()
        }
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    override fun onResume() {
        super.onResume()
        loadTodayRecords()
    }
}
