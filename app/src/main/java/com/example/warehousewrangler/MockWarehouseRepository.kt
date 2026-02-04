package com.example.warehousewrangler

import com.example.warehousewrangler.models.WarehouseIssue
import java.util.UUID

class MockWarehouseRepository(
    seedUserId: String = "USER_001"
) : WarehouseRepository {

    private val lock = Any()
    private val issues: MutableList<WarehouseIssue> = mutableListOf(
        WarehouseIssue().apply {
            issueId = UUID.randomUUID().toString()
            assignedUser = seedUserId
            status = "ASSIGNED"
            targetLocation = "A-01-01"
            skuId = "SKU-APPLE-001"
            barcode = "8800000000001"
            lotNumber = "LOT-240204-A"
            targetQty = 3
            currentQty = 0
            skuImageUrl = "https://picsum.photos/seed/sku-apple-001/512/512"
        },
        WarehouseIssue().apply {
            issueId = UUID.randomUUID().toString()
            assignedUser = seedUserId
            status = "ASSIGNED"
            targetLocation = "B-12-07"
            skuId = "SKU-BANANA-002"
            barcode = "8800000000002"
            lotNumber = "LOT-240204-B"
            targetQty = 2
            currentQty = 0
            skuImageUrl = "https://picsum.photos/seed/sku-banana-002/512/512"
        },
        WarehouseIssue().apply {
            issueId = UUID.randomUUID().toString()
            assignedUser = seedUserId
            status = "ASSIGNED"
            targetLocation = "C-03-15"
            skuId = "SKU-CARROT-003"
            barcode = "8800000000003"
            lotNumber = "LOT-240204-C"
            targetQty = 1
            currentQty = 0
            skuImageUrl = "https://picsum.photos/seed/sku-carrot-003/512/512"
        }
    )

    override fun fetchAssignedIssue(userId: String, callback: (WarehouseIssue?) -> Unit) {
        val issue = synchronized(lock) {
            issues.firstOrNull { it.assignedUser == userId && it.status == "ASSIGNED" }
        }
        callback(issue)
    }

    override fun updateIssue(issue: WarehouseIssue, callback: (Boolean) -> Unit) {
        synchronized(lock) {
            val idx = issues.indexOfFirst { it.issueId == issue.issueId }
            if (idx >= 0) {
                issues[idx] = issue
            }
        }
        callback(true)
    }
}

