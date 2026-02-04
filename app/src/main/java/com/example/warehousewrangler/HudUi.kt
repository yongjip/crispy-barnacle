package com.example.warehousewrangler

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import coil.load
import com.example.warehousewrangler.models.WarehouseIssue

class HudUi(root: View) {

    private val tvTargetLocation: TextView = root.findViewById(R.id.tv_target_location)
    private val tvSku: TextView = root.findViewById(R.id.tv_sku)
    private val tvBarcode: TextView = root.findViewById(R.id.tv_barcode)
    private val tvLot: TextView = root.findViewById(R.id.tv_lot)
    private val tvProgress: TextView = root.findViewById(R.id.tv_progress)
    private val tvHudStatus: TextView = root.findViewById(R.id.tv_hud_status)
    private val ivSkuImage: ImageView = root.findViewById(R.id.iv_sku_image)

    fun render(issue: WarehouseIssue?, stage: PickScanStage?) {
        if (issue != null) {
            val targetLoc = issue.targetLocation ?: issue.locationCode
            tvTargetLocation.text = "LOC: ${targetLoc ?: "--"}"
            tvSku.text = "SKU: ${issue.skuId ?: "--"}"
            tvBarcode.text = "BAR: ${issue.barcode ?: "--"}"
            tvLot.text = "LOT: ${issue.lotNumber ?: "--"}"
            tvProgress.text = "${issue.currentQty}/${issue.targetQty}"

            tvHudStatus.text = when (stage) {
                PickScanStage.SCAN_ITEM -> "SCAN ITEM"
                PickScanStage.CONFIRM_LOCATION -> "SCAN LOCATION"
                null -> ""
            }

            val imageUrl = issue.skuImageUrl
            if (!imageUrl.isNullOrBlank()) {
                ivSkuImage.load(imageUrl) {
                    crossfade(true)
                }
            } else {
                ivSkuImage.setImageDrawable(null)
            }
        } else {
            tvTargetLocation.text = "No Active Issue"
            tvSku.text = ""
            tvBarcode.text = ""
            tvLot.text = ""
            tvProgress.text = ""
            tvHudStatus.text = ""
            ivSkuImage.setImageDrawable(null)
        }
    }
}

