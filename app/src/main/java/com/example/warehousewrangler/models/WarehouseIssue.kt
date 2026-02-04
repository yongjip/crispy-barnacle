package com.example.warehousewrangler.models

import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBAttribute
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBHashKey
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBTable

@DynamoDBTable(tableName = "WarehouseIssues")
class WarehouseIssue {
    @get:DynamoDBHashKey(attributeName = "issue_id")
    var issueId: String? = null

    @get:DynamoDBAttribute(attributeName = "location_code")
    var locationCode: String? = null

    // Optional: destination/target location (when different from location_code).
    // Use this for the "go-to" location shown in the HUD during picking.
    @get:DynamoDBAttribute(attributeName = "target_location")
    var targetLocation: String? = null

    @get:DynamoDBAttribute(attributeName = "sku_id")
    var skuId: String? = null

    @get:DynamoDBAttribute(attributeName = "barcode")
    var barcode: String? = null

    @get:DynamoDBAttribute(attributeName = "lot_number")
    var lotNumber: String? = null

    // Optional product image URL (for HUD).
    @get:DynamoDBAttribute(attributeName = "sku_image_url")
    var skuImageUrl: String? = null

    @get:DynamoDBAttribute(attributeName = "target_qty")
    var targetQty: Int = 0

    @get:DynamoDBAttribute(attributeName = "current_qty")
    var currentQty: Int = 0

    @get:DynamoDBAttribute(attributeName = "status")
    var status: String? = "ASSIGNED"

    @get:DynamoDBAttribute(attributeName = "assigned_user")
    var assignedUser: String? = null
}
