package com.example.warehousewrangler

import android.content.Context
import android.content.IntentFilter
import android.hardware.display.DisplayManager
import android.os.Bundle
import android.os.Build
import android.view.Display
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.warehousewrangler.models.WarehouseIssue
import com.example.warehousewrangler.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private val repository: WarehouseRepository by lazy {
        // Home testing: use mock data in debug builds until backend is ready.
        if (BuildConfig.DEBUG) MockWarehouseRepository() else DynamoDBManager(applicationContext)
    }

    private val viewModel: WarehouseViewModel by viewModels {
        WarehouseViewModelFactory(repository)
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var scanReceiver: ScanReceiver
    var hudPresentation: HudPresentation? = null
    private var hudPreviewDialog: HudPreviewDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
            binding.tvStatusMain.text = message
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
        binding.btnMissing.setOnClickListener { showMissingConfirmationDialog() }
        binding.btnNext.setOnClickListener { viewModel.loadNextTask() }
        binding.btnManualScan.setOnClickListener { submitManualScan() }
        binding.etManualScan.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                submitManualScan()
                true
            } else {
                false
            }
        }
        binding.btnArMode.setOnClickListener {
            val intent = android.content.Intent(this, ArActivity::class.java)
            startActivity(intent)
        }
        binding.btnHudPreview.setOnClickListener { toggleHudPreview() }

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
            binding.tvIssueInfo.text = info
        } else {
            binding.tvIssueInfo.text = getString(R.string.no_issue_loaded)
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
        binding.tvScannedSku.text = "Last Scan: $data"
        viewModel.processScan(data)
    }

    private fun submitManualScan() {
        val text = binding.etManualScan.text?.toString().orEmpty().trim()
        if (text.isBlank()) return
        onScanReceived(text)
        binding.etManualScan.setText("")
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
            .setTitle(getString(R.string.confirm_title))
            .setMessage(getString(R.string.confirm_missing_msg))
            .setPositiveButton(getString(R.string.btn_confirm_missing)) { _, _ ->
                viewModel.markAsMissing()
            }
            .setNegativeButton(getString(R.string.btn_cancel), null)
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
