package com.example.warehousewrangler

import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDialog
import com.example.warehousewrangler.models.WarehouseIssue

class HudPreviewDialog(context: Context) : AppCompatDialog(context) {

    private var hudUi: HudUi? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.hud_layout)
        window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        hudUi = window?.decorView?.let { HudUi(it) }
    }

    fun render(issue: WarehouseIssue?, stage: PickScanStage?) {
        hudUi?.render(issue, stage)
    }
}

