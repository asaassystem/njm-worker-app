package com.njm.worker.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.njm.worker.R
import com.njm.worker.data.api.AppCookieJar
import com.njm.worker.data.model.WashRecord
import com.njm.worker.data.repository.WorkerRepository
import com.njm.worker.ui.login.PinLoginActivity
import com.njm.worker.ui.search.SearchActivity
import com.njm.worker.utils.SessionManager
import kotlinx.coroutines.launch

class DashboardActivity : AppCompatActivity() {
    private val repo = WorkerRepository()
    private lateinit var adapter: WashRecordAdapter
    private val washes = mutableListOf<WashRecord>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)
        setupViews()
        loadData()
    }

    private fun setupViews() {
        val tvWorker = findViewById<TextView>(R.id.tvWorkerName)
        tvWorker.text = SessionManager.getWorkerName()
        val tvOrg = findViewById<TextView>(R.id.tvOrgName)
        tvOrg.text = SessionManager.getOrgName()

        val rvWashes = findViewById<RecyclerView>(R.id.rvTodayWashes)
        adapter = WashRecordAdapter(washes)
        rvWashes.layoutManager = LinearLayoutManager(this)
        rvWashes.adapter = adapter

        val btnSearch = findViewById<Button>(R.id.btnSearchCar)
        btnSearch.setOnClickListener {
            startActivity(Intent(this, SearchActivity::class.java))
        }

        val btnLogout = findViewById<Button>(R.id.btnLogout)
        btnLogout.setOnClickListener { doLogout() }
    }

    private fun loadData() {
        lifecycleScope.launch {
            val result = repo.getTodayWashes()
            result.onSuccess { resp ->
                washes.clear()
                washes.addAll(resp.washes ?: emptyList())
                adapter.notifyDataSetChanged()
                val tvCount = findViewById<TextView>(R.id.tvTodayCount)
                tvCount.text = washes.size.toString()
                val tvNoWashes = findViewById<TextView>(R.id.tvNoWashes)
                tvNoWashes.visibility = if (washes.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    private fun doLogout() {
        lifecycleScope.launch {
            repo.logout()
            SessionManager.clearSession()
            AppCookieJar.clear()
            val intent = Intent(this@DashboardActivity, PinLoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        loadData()
    }
}