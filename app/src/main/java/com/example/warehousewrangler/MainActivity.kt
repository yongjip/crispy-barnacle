package com.example.warehousewrangler

import android.content.Context
import android.content.IntentFilter
import android.hardware.display.DisplayManager
import android.os.Bundle
import android.os.Build
import android.view.Display
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import coil.load
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doOnTextChanged
import com.example.warehousewrangler.models.WarehouseIssue
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class MainActivity : AppCompatActivity() {

    private val repository: WarehouseRepository by lazy {
        // Home testing: use mock data in debug builds until backend is ready.
        if (BuildConfig.DEBUG) MockWarehouseRepository() else DynamoDBManager(applicationContext)
    }

    private val viewModel: WarehouseViewModel by viewModels {
        WarehouseViewModelFactory(repository)
    }

    private lateinit var scanReceiver: ScanReceiver
    var hudPresentation: HudPresentation? = null
    private var hudPreviewDialog: HudPreviewDialog? = null

    // UI Elements
    lateinit var tvStatus: TextView
    lateinit var tvIssueInfo: TextView
    lateinit var tvScannedSku: TextView
    lateinit var ivSkuPreview: ImageView
    lateinit var tilManualScan: TextInputLayout
    lateinit var etManualScan: TextInputEditText
    lateinit var btnMissing: Button
    lateinit var btnManualScan: Button
    lateinit var btnNext: Button
    lateinit var btnArMode: Button
    lateinit var btnHudPreview: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Init UI
        tvStatus = findViewById(R.id.tv_status_main)
        tvIssueInfo = findViewById(R.id.tv_issue_info)
        tvScannedSku = findViewById(R.id.tv_scanned_sku)
        ivSkuPreview = findViewById(R.id.iv_sku_preview)
        tilManualScan = findViewById(R.id.til_manual_scan)
        etManualScan = findViewById(R.id.et_manual_scan)
        btnMissing = findViewById(R.id.btn_missing)
        btnManualScan = findViewById(R.id.btn_manual_scan)
        btnNext = findViewById(R.id.btn_next)
        btnArMode = findViewById(R.id.btn_ar_mode)
        btnHudPreview = findViewById(R.id.btn_hud_preview)

        // Observe ViewModel
        viewModel.currentIssue.observe(this) { issue ->
            updateUiState(issue)
        }

        viewModel.pickScanStage.observe(this) { stage ->
            val issue = viewModel.currentIssue.value
            hudPresentation?.updateIssue(issue, stage)
            hudPreviewDialog?.render(issue, stage)
        }

        viewModel.statusMessage.observe(this) { message ->
            tvStatus.text = message
        }

        viewModel.toastMessage.observe(this) { message ->
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }

        viewModel.isLoading.observe(this) { isLoading ->
            btnNext.isEnabled = !isLoading
            btnMissing.isEnabled = !isLoading
            btnManualScan.isEnabled = !isLoading
            btnNext.text = if (isLoading) "Loading..." else "Load Next Task"
        }

        // Scan Receiver
        scanReceiver = ScanReceiver { data ->
            onScanReceived(data)
        }
        val filter = IntentFilter()
        filter.addAction(ScanReceiver.ACTION_SCAN)
        filter.addCategory("android.intent.category.DEFAULT")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // DataWedge (or other scanner apps) will send broadcasts from outside the app.
            registerReceiver(scanReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(scanReceiver, filter)
        }

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
        btnMissing.setOnClickListener { showMissingConfirmationDialog() }
        btnNext.setOnClickListener { viewModel.loadNextTask() }
        btnManualScan.setOnClickListener { submitManualScan() }

        etManualScan.doOnTextChanged { _, _, _, _ ->
            tilManualScan.isErrorEnabled = false
        }

        etManualScan.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                submitManualScan()
                true
            } else {
                false
            }
        }
        btnArMode.setOnClickListener {
            val intent = android.content.Intent(this, ArActivity::class.java)
            startActivity(intent)
        }
        btnHudPreview.setOnClickListener { toggleHudPreview() }

        // Initial Load
        viewModel.loadNextTask()
    }

    private fun updateUiState(issue: WarehouseIssue?) {
        if (issue != null) {
            val targetLoc = issue.targetLocation ?: issue.locationCode
            val info = buildString {
                append("Target Loc: ${targetLoc ?: "--"}\n")
                append("SKU: ${issue.skuId ?: "--"}\n")
                append("BAR: ${issue.barcode ?: "--"}\n")
                append("LOT: ${issue.lotNumber ?: "--"}\n")
                append("Qty: ${issue.currentQty} / ${issue.targetQty}")
            }
            tvIssueInfo.text = info

            if (!issue.skuImageUrl.isNullOrBlank()) {
                ivSkuPreview.load(issue.skuImageUrl) {
                    crossfade(true)
                }
            } else {
                ivSkuPreview.setImageDrawable(null)
            }
        } else {
            tvIssueInfo.text = "No active issue."
            ivSkuPreview.setImageDrawable(null)
        }

        val stage = viewModel.pickScanStage.value
        hudPresentation?.updateIssue(issue, stage)
        hudPreviewDialog?.render(issue, stage)
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

    private fun submitManualScan() {
        val text = etManualScan.text?.toString().orEmpty().trim()
        if (text.isBlank()) {
            tilManualScan.error = "Please enter a value"
            return
        }
        onScanReceived(text)
        etManualScan.setText("")
    }

    private fun toggleHudPreview() {
        val dialog = hudPreviewDialog
        if (dialog != null && dialog.isShowing) {
            dialog.dismiss()
            return
        }

        val newDialog = HudPreviewDialog(this)
        hudPreviewDialog = newDialog
        newDialog.show()
        newDialog.render(viewModel.currentIssue.value, viewModel.pickScanStage.value)
    }

    private fun showMissingConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Confirm")
            .setMessage("Are you sure you want to mark this item as MISSING?")
            .setPositiveButton("Mark Missing") { _, _ ->
                viewModel.markAsMissing()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(scanReceiver)
        hudPresentation?.dismiss()
        hudPreviewDialog?.dismiss()
        hudPreviewDialog = null
    }
}
