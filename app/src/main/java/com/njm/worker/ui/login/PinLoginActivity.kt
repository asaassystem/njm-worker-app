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
import com.njm.worker.data.api.ApiClient
import com.njm.worker.data.model.LoginRequest
import com.njm.worker.ui.dashboard.DashboardActivity
import com.njm.worker.utils.SessionManager
import kotlinx.coroutines.launch

class PinLoginActivity : AppCompatActivity() {
    private var pin = StringBuilder()
    private lateinit var dots: Array<TextView>
    private lateinit var tvError: TextView
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (SessionManager.isLoggedIn(this)) {
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
        if (pin.isNotEmpty()) {
            pin.deleteCharAt(pin.length - 1)
            updateDots()
        }
    }

    private fun updateDots() {
        dots.forEachIndexed { i, dot ->
            dot.text = if (i < pin.length) "x" else "o"
        }
    }

    private fun doLogin() {
        val pinStr = pin.toString()
        if (pinStr.length != 4) {
            tvError.text = getString(R.string.error_pin_length)
            tvError.visibility = View.VISIBLE
            return
        }
        tvError.visibility = View.GONE
        progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val api = ApiClient.service
                val response = api.loginWithPin(LoginRequest(pin = pinStr))
                progressBar.visibility = View.GONE

                if (response.isSuccessful && response.body()?.success == true) {
                    val body = response.body()!!
                    SessionManager.saveWorker(
                        this@PinLoginActivity,
                        body.workerId ?: 0,
                        body.workerName ?: "",
                        body.orgId ?: 0,
                        pinStr
                    )
                    startDashboard()
                } else {
                    val msg = response.body()?.message ?: getString(R.string.error_invalid_pin)
                    tvError.text = msg
                    tvError.visibility = View.VISIBLE
                    pin.clear()
                    updateDots()
                }
            } catch (e: Exception) {
                progressBar.visibility = View.GONE
                tvError.text = getString(R.string.error_network) + ": " + (e.message ?: "")
                tvError.visibility = View.VISIBLE
                pin.clear()
                updateDots()
            }
        }
    }

    private fun startDashboard() {
        startActivity(Intent(this, DashboardActivity::class.java))
        finish()
    }
}