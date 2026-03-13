package com.njm.worker.ui.login

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.njm.worker.R
import com.njm.worker.data.repository.WorkerRepository
import com.njm.worker.ui.dashboard.DashboardActivity
import com.njm.worker.utils.SessionManager
import kotlinx.coroutines.launch

class PinLoginActivity : AppCompatActivity() {
    private val repo = WorkerRepository()
    private var pin = StringBuilder()
    private lateinit var dots: Array<TextView>
    private lateinit var tvError: TextView
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (SessionManager.isLoggedIn()) {
            startDashboard()
            return
        }
        setContentView(R.layout.activity_pin_login)
        setupViews()
    }

    private fun setupViews() {
        dots = arrayOf(
            findViewById(R.id.dot1), findViewById(R.id.dot2),
            findViewById(R.id.dot3), findViewById(R.id.dot4)
        )
        tvError = findViewById(R.id.tvError)
        progressBar = findViewById(R.id.progressBar)

        val btnIds = listOf(
            R.id.btn0 to "0", R.id.btn1 to "1", R.id.btn2 to "2",
            R.id.btn3 to "3", R.id.btn4 to "4", R.id.btn5 to "5",
            R.id.btn6 to "6", R.id.btn7 to "7", R.id.btn8 to "8",
            R.id.btn9 to "9"
        )
        btnIds.forEach { (id, digit) ->
            findViewById<Button>(id).setOnClickListener { addDigit(digit) }
        }
        findViewById<Button>(R.id.btnDelete).setOnClickListener { deleteDigit() }
        findViewById<Button>(R.id.btnLogin).setOnClickListener { doLogin() }
    }

    private fun addDigit(d: String) {
        if (pin.length >= 4) return
        pin.append(d)
        updateDots()
        if (pin.length == 4) doLogin()
    }

    private fun deleteDigit() {
        if (pin.isNotEmpty()) { pin.deleteCharAt(pin.length - 1); updateDots() }
    }

    private fun updateDots() {
        dots.forEachIndexed { i, dot ->
            dot.text = if (i < pin.length) getString(R.string.pin_dot_filled)
            else getString(R.string.pin_dot_empty)
        }
    }

    private fun doLogin() {
        if (pin.length != 4) { showError("Please enter 4 digits"); return }
        setLoading(true)
        lifecycleScope.launch {
            val result = repo.loginWithPin(pin.toString())
            setLoading(false)
            result.onSuccess { resp ->
                SessionManager.saveSession(
                    resp.workerId ?: 0,
                    resp.workerName ?: "",
                    resp.orgId ?: 0
                )
                startDashboard()
            }.onFailure { showError(it.message ?: "Login failed") }
        }
    }

    private fun showError(msg: String) {
        tvError.text = msg
        tvError.visibility = View.VISIBLE
        pin.clear()
        updateDots()
    }

    private fun setLoading(loading: Boolean) {
        progressBar.visibility = if (loading) View.VISIBLE else View.GONE
    }

    private fun startDashboard() {
        startActivity(Intent(this, DashboardActivity::class.java))
        finish()
    }
}