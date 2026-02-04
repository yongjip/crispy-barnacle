package com.example.warehousewrangler

import android.content.Context
import android.content.IntentFilter
import android.hardware.display.DisplayManager
import android.os.Bundle
import android.view.Display
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.warehousewrangler.models.WarehouseIssue

class MainActivity : AppCompatActivity() {

    private val viewModel: WarehouseViewModel by viewModels {
        WarehouseViewModelFactory(DynamoDBManager(applicationContext))
    }

    private lateinit var scanReceiver: ScanReceiver
    var hudPresentation: HudPresentation? = null

    // UI Elements
    lateinit var tvStatus: TextView
    lateinit var tvIssueInfo: TextView
    lateinit var tvScannedSku: TextView
    lateinit var btnMissing: Button
    lateinit var btnNext: Button
    lateinit var btnArMode: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Init UI
        tvStatus = findViewById(R.id.tv_status_main)
        tvIssueInfo = findViewById(R.id.tv_issue_info)
        tvScannedSku = findViewById(R.id.tv_scanned_sku)
        btnMissing = findViewById(R.id.btn_missing)
        btnNext = findViewById(R.id.btn_next)
        btnArMode = findViewById(R.id.btn_ar_mode)

        // Observe ViewModel
        viewModel.currentIssue.observe(this) { issue ->
            updateUiState(issue)
        }

        viewModel.statusMessage.observe(this) { message ->
            tvStatus.text = message
        }

        viewModel.toastMessage.observe(this) { message ->
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }

        // Scan Receiver
        scanReceiver = ScanReceiver { data ->
            onScanReceived(data)
        }
        val filter = IntentFilter()
        filter.addAction(ScanReceiver.ACTION_SCAN)
        filter.addCategory("android.intent.category.DEFAULT")
        registerReceiver(scanReceiver, filter)

        // Display Manager (for XREAL)
        val displayManager = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        displayManager.registerDisplayListener(object : DisplayManager.DisplayListener {
            override fun onDisplayAdded(displayId: Int) {
                checkForExternalDisplay(displayManager)
            }
            override fun onDisplayRemoved(displayId: Int) {
                if (hudPresentation != null && hudPresentation!!.display.displayId == displayId) {
                    hudPresentation!!.dismiss()
                    hudPresentation = null
                }
            }
            override fun onDisplayChanged(displayId: Int) {}
        }, null)

        // Check initially
        checkForExternalDisplay(displayManager)

        // Setup Buttons
        btnMissing.setOnClickListener { viewModel.markAsMissing() }
        btnNext.setOnClickListener { viewModel.loadNextTask() }
        btnArMode.setOnClickListener {
            val intent = android.content.Intent(this, ArActivity::class.java)
            startActivity(intent)
        }

        // Initial Load
        viewModel.loadNextTask()
    }

    private fun updateUiState(issue: WarehouseIssue?) {
        if (issue != null) {
            val info = "Loc: ${issue.locationCode}\nSKU: ${issue.skuId}\nQty: ${issue.currentQty} / ${issue.targetQty}"
            tvIssueInfo.text = info
        } else {
            tvIssueInfo.text = "No active issue."
        }

        hudPresentation?.updateIssue(issue)
    }

    private fun checkForExternalDisplay(displayManager: DisplayManager) {
        val displays = displayManager.displays
        for (display in displays) {
            if (display.displayId != Display.DEFAULT_DISPLAY) {
                showHud(display)
                break
            }
        }
    }

    private fun showHud(display: Display) {
        if (hudPresentation == null) {
            hudPresentation = HudPresentation(this, display)
            hudPresentation!!.show()
            updateUiState(viewModel.currentIssue.value)
        }
    }

    fun onScanReceived(data: String) {
        tvScannedSku.text = "Last Scan: $data"
        viewModel.processScan(data)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(scanReceiver)
        hudPresentation?.dismiss()
    }
}
