package com.example.warehousewrangler

import android.content.Context
import com.amazonaws.auth.CognitoCachingCredentialsProvider
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapper
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBScanExpression
import com.amazonaws.regions.Regions
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient
import com.amazonaws.services.dynamodbv2.model.AttributeValue
import com.example.warehousewrangler.models.WarehouseIssue
import java.util.concurrent.Executors

class DynamoDBManager(context: Context) {
    private val dbMapper: DynamoDBMapper

    init {
        // Placeholder for credentials. In a real app, use valid Identity Pool ID.
        val credentialsProvider = CognitoCachingCredentialsProvider(
            context,
            "us-east-1:00000000-0000-0000-0000-000000000000", // Example ID
            Regions.US_EAST_1
        )
        val dbClient = AmazonDynamoDBClient(credentialsProvider)
        dbMapper = DynamoDBMapper(dbClient)
    }

    // Run network operations on a background thread
    private val executor = Executors.newSingleThreadExecutor()

    fun fetchAssignedIssue(userId: String, callback: (WarehouseIssue?) -> Unit) {
        executor.execute {
            retryOperation(
                block = {
                    // Using scan for simplicity. In production, use Query with Index.
                    val expression = DynamoDBScanExpression()
                    val attributeValues = HashMap<String, AttributeValue>()
                    attributeValues[":user"] = AttributeValue().withS(userId)
                    attributeValues[":status"] = AttributeValue().withS("ASSIGNED")

                    expression.filterExpression = "assigned_user = :user AND status = :status"
                    expression.expressionAttributeValues = attributeValues

                    val result = dbMapper.scan(WarehouseIssue::class.java, expression)
                    // Return the first one found
                    val issue = if (result.isNotEmpty()) result[0] else null
                    callback(issue)
                },
                onError = {
                    callback(null)
                }
            )
        }
    }

    fun updateIssue(issue: WarehouseIssue, callback: (Boolean) -> Unit) {
        executor.execute {
            retryOperation(
                block = {
                    dbMapper.save(issue)
                    callback(true)
                },
                onError = {
                     callback(false)
                }
            )
        }
    }

    private fun <T> retryOperation(retries: Int = 3, delayMs: Long = 1000, onError: (() -> Unit)? = null, block: () -> T) {
        var attempt = 0
        while (attempt < retries) {
            try {
                block()
                return
            } catch (e: Exception) {
                attempt++
                e.printStackTrace()
                if (attempt >= retries) {
                    if (onError != null) {
                        onError()
                    } else {
                        // For fetch, we might just want to return null via callback if passed, but here we handled it inside block mostly.
                        // Ideally, refactor so callback is called here on failure.
                        // However, strictly following the existing pattern:
                        // If it fails completely, we swallow if no onError provided?
                        // The original code passed callback(null) in catch.
                        // Let's assume the caller handles the 'null' via callback inside 'block' if it was structured that way,
                        // but 'block' here includes the logic that MIGHT fail.
                        // Actually, better design:
                    }
                }
                try {
                    Thread.sleep(delayMs * attempt)
                } catch (ie: InterruptedException) {
                    Thread.currentThread().interrupt()
                }
            }
        }
    }
}
