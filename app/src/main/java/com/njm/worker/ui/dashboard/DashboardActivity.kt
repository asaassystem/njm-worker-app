package com.njm.worker.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.njm.worker.R
import com.njm.worker.data.repository.WorkerRepository
import com.njm.worker.ui.login.PinLoginActivity
import com.njm.worker.ui.search.SearchActivity
import com.njm.worker.utils.SessionManager
import kotlinx.coroutines.launch

class DashboardActivity : AppCompatActivity() {
    private val repo = WorkerRepository()
    private lateinit var tvWorkerName: TextView
    private lateinit var tvTodayCount: TextView
    private lateinit var rvTodayWashes: RecyclerView
    private lateinit var tvNoWashes: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!SessionManager.isLoggedIn(this)) {
            goToLogin()
            return
        }
        setContentView(R.layout.activity_dashboard)
        setupViews()
        loadData()
    }

    private fun setupViews() {
        tvWorkerName = findViewById(R.id.tvWorkerName)
        tvTodayCount = findViewById(R.id.tvTodayCount)
        rvTodayWashes = findViewById(R.id.rvTodayWashes)
        tvNoWashes = findViewById(R.id.tvNoWashes)

        tvWorkerName.text = SessionManager.getWorkerName(this)
        rvTodayWashes.layoutManager = LinearLayoutManager(this)

        findViewById<View>(R.id.btnSearchCar).setOnClickListener {
            startActivity(Intent(this, SearchActivity::class.java))
        }

        findViewById<View>(R.id.btnLogout).setOnClickListener {
            doLogout()
        }
    }

    override fun onResume() {
        super.onResume()
        loadData()
    }

    private fun loadData() {
        lifecycleScope.launch {
            val result = repo.getTodayWashes()
            result.onSuccess { data ->
                val washes = data.washes ?: emptyList()
                tvTodayCount.text = washes.size.toString()
                if (washes.isEmpty()) {
                    tvNoWashes.visibility = View.VISIBLE
                    rvTodayWashes.visibility = View.GONE
                } else {
                    tvNoWashes.visibility = View.GONE
                    rvTodayWashes.visibility = View.VISIBLE
                    rvTodayWashes.adapter = WashRecordAdapter(washes)
                }
            }
            result.onFailure {
                tvNoWashes.visibility = View.VISIBLE
                rvTodayWashes.visibility = View.GONE
                tvTodayCount.text = "0"
            }
        }
    }

    private fun doLogout() {
        lifecycleScope.launch {
            repo.logout()
            SessionManager.logout(this@DashboardActivity)
            goToLogin()
        }
    }

    private fun goToLogin() {
        startActivity(Intent(this, PinLoginActivity::class.java))
        finish()
    }
}