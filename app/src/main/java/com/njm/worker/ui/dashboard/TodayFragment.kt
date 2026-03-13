package com.njm.worker.ui.dashboard

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

    override fun onResume() {
        super.onResume()
        view?.let { loadWashes(it) }
    }

    private fun loadWashes(view: View) {
        val rvTodayWashes = view.findViewById<RecyclerView>(R.id.rvTodayWashes)
        val tvNoWashes = view.findViewById<TextView>(R.id.tvNoWashes)

        rvTodayWashes.layoutManager = LinearLayoutManager(requireContext())

        lifecycleScope.launch {
            val result = repo.getTodayWashes()
            result.onSuccess { data ->
                val washes = data.washes ?: emptyList()
                if (washes.isEmpty()) {
                    tvNoWashes.visibility = View.VISIBLE
                    rvTodayWashes.visibility = View.GONE
                } else {
                    tvNoWashes.visibility = View.GONE
                    rvTodayWashes.visibility = View.VISIBLE
                    rvTodayWashes.adapter = WashRecordAdapter(washes) { wash ->
                        // Print receipt
                        (activity as? DashboardActivity)?.let {
                            PrintManager.printWashReceipt(it, wash, it)
                        }
                    }
                }
                (activity as? DashboardActivity)?.loadStats()
            }
            result.onFailure {
                tvNoWashes.visibility = View.VISIBLE
                rvTodayWashes.visibility = View.GONE
            }
        }
    }
}