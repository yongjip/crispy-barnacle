package com.example.warehousewrangler

import com.example.warehousewrangler.models.WarehouseIssue

interface WarehouseRepository {
    fun fetchAssignedIssue(userId: String, callback: (WarehouseIssue?) -> Unit)
    fun updateIssue(issue: WarehouseIssue, callback: (Boolean) -> Unit)
}

