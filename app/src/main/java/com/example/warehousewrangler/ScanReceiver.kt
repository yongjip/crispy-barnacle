package com.example.warehousewrangler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ScanReceiver(private val onScan: (String) -> Unit) : BroadcastReceiver() {
    companion object {
        // This action must be configured in the Zebra DataWedge Profile under Intent Output
        const val ACTION_SCAN = "com.example.warehousewrangler.SCAN"
        const val EXTRA_DATA_STRING = "com.symbol.datawedge.data_string"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_SCAN) {
            val scannedData = intent.getStringExtra(EXTRA_DATA_STRING)
            if (scannedData != null) {
                onScan(scannedData)
            }
        }
    }
}
