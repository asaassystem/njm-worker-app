package com.njm.worker.ui.dashboard

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.njm.worker.data.model.WashRecord
import com.njm.worker.data.repository.WorkerRepository
import com.njm.worker.databinding.ActivityDashboardBinding
import com.njm.worker.printer.PrinterManager
import com.njm.worker.ui.login.PinLoginActivity
import com.njm.worker.ui.search.SearchActivity
import com.njm.worker.utils.SessionManager
import kotlinx.coroutines.launch

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private lateinit var printerManager: PrinterManager
    private val repo = WorkerRepository()
    private val washAdapter = WashRecordAdapter()

    private val printerStatusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            updatePrinterStatus()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupPrinter()
        loadTodayWashes()

        // Register printer status receiver
        registerReceiver(printerStatusReceiver, IntentFilter("com.njm.worker.PRINTER_STATUS"))
    }

    private fun setupUI() {
        binding.tvWorkerName.text = "مرحباً، " + SessionManager.workerName

        binding.btnSearch.setOnClickListener {
            startActivity(Intent(this, SearchActivity::class.java))
        }

        binding.btnLogout.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("تسجيل الخروج")
                .setMessage("هل تريد تسجيل الخروج؟")
                .setPositiveButton("نعم") { _, _ -> logout() }
                .setNegativeButton("إلغاء", null)
                .show()
        }

        binding.rvWashes.layoutManager = LinearLayoutManager(this)
        binding.rvWashes.adapter = washAdapter
    }

    private fun setupPrinter() {
        printerManager = PrinterManager(this)
        printerManager.initialize { ready, message ->
            runOnUiThread { updatePrinterStatus() }
        }
    }

    private fun updatePrinterStatus() {
        val statusText = printerManager.getStatusText()
        val isReady = printerManager.isReady()
        binding.tvPrinterStatus.text = "الطابعة: $statusText"
        binding.tvPrinterStatus.setTextColor(
            if (isReady) getColor(android.R.color.holo_green_dark)
            else getColor(android.R.color.holo_red_dark)
        )
    }

    private fun loadTodayWashes() {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            repo.getTodayWashes()
                .onSuccess { response ->
                    binding.progressBar.visibility = View.GONE
                    binding.tvTodayCount.text = "غسيل اليوم: " + response.total
                    binding.tvTodayAmount.text = String.format("%.2f ريال", response.total_amount)
                    washAdapter.submitList(response.washes ?: emptyList())
                    binding.tvNoWashes.visibility = if ((response.washes ?: emptyList()).isEmpty()) View.VISIBLE else View.GONE
                }
                .onFailure {
                    binding.progressBar.visibility = View.GONE
                    binding.tvNoWashes.visibility = View.VISIBLE
                    binding.tvNoWashes.text = "تعذر تحميل السجل"
                }
        }
    }

    override fun onResume() {
        super.onResume()
        loadTodayWashes()
        updatePrinterStatus()
    }

    private fun logout() {
        lifecycleScope.launch {
            repo.logout()
            printerManager.destroy()
            startActivity(Intent(this@DashboardActivity, PinLoginActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(printerStatusReceiver)
        printerManager.destroy()
    }
}