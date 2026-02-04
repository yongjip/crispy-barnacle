package com.example.warehousewrangler

import android.content.Context
import android.content.IntentFilter
import android.hardware.display.DisplayManager
import android.os.Bundle
import android.view.Display
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.warehousewrangler.models.WarehouseIssue

class MainActivity : AppCompatActivity() {

    lateinit var dynamoDBManager: DynamoDBManager
    private lateinit var scanReceiver: ScanReceiver
    var hudPresentation: HudPresentation? = null

    // UI Elements
    lateinit var tvStatus: TextView
    lateinit var tvIssueInfo: TextView
    lateinit var tvScannedSku: TextView
    lateinit var btnMissing: Button
    lateinit var btnNext: Button

    // Data State
    var currentIssue: WarehouseIssue? = null
    var currentUserId: String = "USER_001" // Hardcoded for demo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Init UI
        tvStatus = findViewById(R.id.tv_status_main)
        tvIssueInfo = findViewById(R.id.tv_issue_info)
        tvScannedSku = findViewById(R.id.tv_scanned_sku)
        btnMissing = findViewById(R.id.btn_missing)
        btnNext = findViewById(R.id.btn_next)

        // Init Managers
        dynamoDBManager = DynamoDBManager(this)

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
        btnMissing.setOnClickListener { markAsMissing() }
        btnNext.setOnClickListener { loadNextTask() }

        // Initial Load
        loadNextTask()
    }

    private fun loadNextTask() {
        tvStatus.text = "Status: Loading..."
        dynamoDBManager.fetchAssignedIssue(currentUserId) { issue ->
            runOnUiThread {
                if (issue != null) {
                    currentIssue = issue
                    // Keep currentQty from DB or default to 0
                    updateUiState()
                    tvStatus.text = "Status: Searching"

                    if (currentIssue?.status == "ASSIGNED") {
                        currentIssue?.status = "SEARCHING"
                        dynamoDBManager.updateIssue(currentIssue!!) { /* error handling? */ }
                    }
                } else {
                    currentIssue = null
                    tvStatus.text = "Status: No Tasks Assigned"
                    updateUiState()
                }
            }
        }
    }

    private fun updateUiState() {
        val issue = currentIssue
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
            updateUiState()
        }
    }

    fun onScanReceived(data: String) {
        tvScannedSku.text = "Last Scan: $data"
        val issue = currentIssue ?: return

        if (data == issue.skuId) {
            issue.currentQty += 1
            updateUiState()

            if (issue.currentQty >= issue.targetQty) {
                // Completed
                issue.status = "FOUND"
                tvStatus.text = "Status: Item Found! Updating..."
                dynamoDBManager.updateIssue(issue) { success ->
                    runOnUiThread {
                        if (success) {
                            Toast.makeText(this, "Task Completed!", Toast.LENGTH_SHORT).show()
                            loadNextTask()
                        } else {
                            Toast.makeText(this, "Error Updating DB", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        } else {
            Toast.makeText(this, "Wrong SKU!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun markAsMissing() {
        val issue = currentIssue ?: return
        issue.status = "MISSING"
        tvStatus.text = "Status: Marking Missing..."
        dynamoDBManager.updateIssue(issue) { success ->
             runOnUiThread {
                if (success) {
                    Toast.makeText(this, "Marked Missing", Toast.LENGTH_SHORT).show()
                    loadNextTask()
                } else {
                    Toast.makeText(this, "Error Updating DB", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(scanReceiver)
        hudPresentation?.dismiss()
    }
}
