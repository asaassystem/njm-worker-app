package com.njm.worker.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
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
    private lateinit var tvTotalToday: TextView
    private lateinit var rvWashes: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmpty: TextView

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
        tvTotalToday = findViewById(R.id.tvTotalToday)
        rvWashes = findViewById(R.id.rvWashes)
        progressBar = findViewById(R.id.progressBar)
        tvEmpty = findViewById(R.id.tvEmpty)

        tvWorkerName.text = SessionManager.getWorkerName(this)

        rvWashes.layoutManager = LinearLayoutManager(this)

        findViewById<View>(R.id.btnSearch).setOnClickListener {
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
        progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            val result = repo.getTodayWashes()
            progressBar.visibility = View.GONE
            result.onSuccess { data ->
                val washes = data.washes ?: emptyList()
                tvTotalToday.text = washes.size.toString()
                if (washes.isEmpty()) {
                    tvEmpty.visibility = View.VISIBLE
                    rvWashes.visibility = View.GONE
                } else {
                    tvEmpty.visibility = View.GONE
                    rvWashes.visibility = View.VISIBLE
                    rvWashes.adapter = WashRecordAdapter(washes)
                }
            }
            result.onFailure {
                tvEmpty.visibility = View.VISIBLE
                rvWashes.visibility = View.GONE
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