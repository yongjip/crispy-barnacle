package com.example.warehousewrangler

import android.app.Presentation
import android.content.Context
import android.os.Bundle
import android.view.Display
import android.widget.TextView
import com.example.warehousewrangler.models.WarehouseIssue

class HudPresentation(context: Context, display: Display) : Presentation(context, display) {
    private var tvLocation: TextView? = null
    private var tvSku: TextView? = null
    private var tvProgress: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.hud_layout)

        tvLocation = findViewById(R.id.tv_location)
        tvSku = findViewById(R.id.tv_sku)
        tvProgress = findViewById(R.id.tv_progress)
    }

    fun updateIssue(issue: WarehouseIssue?) {
        if (issue != null) {
            tvLocation?.text = "Loc: ${issue.locationCode}"
            tvSku?.text = "SKU: ${issue.skuId}"
            tvProgress?.text = "Qty: ${issue.currentQty}/${issue.targetQty}"
        } else {
            tvLocation?.text = "No Active Issue"
            tvSku?.text = ""
            tvProgress?.text = ""
        }
    }
}
