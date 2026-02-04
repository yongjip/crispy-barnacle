package com.example.warehousewrangler

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.example.warehousewrangler.models.WarehouseIssue
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations

class WarehouseViewModelTest {

    @get:Rule
    val rule = InstantTaskExecutorRule()

    @Mock
    lateinit var mockRepo: DynamoDBManager

    @Mock
    lateinit var statusObserver: Observer<String>

    @Mock
    lateinit var issueObserver: Observer<WarehouseIssue?>

    @Mock
    lateinit var toastObserver: Observer<String>

    private lateinit var viewModel: WarehouseViewModel

    // Helper for non-nullable Mockito matchers in Kotlin
    private fun <T> any(type: Class<T>): T = Mockito.any(type)
    private fun <T> any(): T {
        Mockito.any<T>()
        return null as T
    }

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        viewModel = WarehouseViewModel(mockRepo)
        viewModel.statusMessage.observeForever(statusObserver)
        viewModel.currentIssue.observeForever(issueObserver)
        viewModel.toastMessage.observeForever(toastObserver)
    }

    @Test
    fun `loadNextTask updates status and issue when found`() {
        val issue = WarehouseIssue().apply {
            issueId = "123"
            skuId = "SKU-ABC"
            status = "ASSIGNED"
        }

        // Use argument matchers safely
        doAnswer {
            val callback = it.arguments[1] as (WarehouseIssue?) -> Unit
            callback(issue)
            null
        }.`when`(mockRepo).fetchAssignedIssue(ArgumentMatchers.anyString(), any())

        viewModel.loadNextTask()

        verify(statusObserver).onChanged("Status: Searching")
        verify(issueObserver).onChanged(issue)
        assertEquals("SEARCHING", issue.status)
    }

    @Test
    fun `processScan increments qty on correct SKU`() {
        val issue = WarehouseIssue().apply {
            skuId = "TARGET-SKU"
            targetQty = 5
            currentQty = 0
            status = "SEARCHING"
        }

        doAnswer {
            val callback = it.arguments[1] as (WarehouseIssue?) -> Unit
            callback(issue)
            null
        }.`when`(mockRepo).fetchAssignedIssue(ArgumentMatchers.anyString(), any())

        viewModel.loadNextTask()

        viewModel.processScan("TARGET-SKU")

        assertEquals(1, issue.currentQty)
    }

    @Test
    fun `processScan ignores wrong SKU`() {
        val issue = WarehouseIssue().apply {
            skuId = "TARGET-SKU"
            targetQty = 5
            currentQty = 0
        }

        doAnswer {
             val callback = it.arguments[1] as (WarehouseIssue?) -> Unit
             callback(issue)
             null
        }.`when`(mockRepo).fetchAssignedIssue(ArgumentMatchers.anyString(), any())

        viewModel.loadNextTask()

        viewModel.processScan("WRONG-SKU")

        assertEquals(0, issue.currentQty)
        verify(toastObserver).onChanged("Wrong SKU!")
    }

    @Test
    fun `processScan completes task when target reached`() {
        val issue = WarehouseIssue().apply {
            skuId = "TARGET-SKU"
            targetQty = 1
            currentQty = 0
            status = "SEARCHING"
        }

        doAnswer {
             val callback = it.arguments[1] as (WarehouseIssue?) -> Unit
             callback(issue)
             null
        }.`when`(mockRepo).fetchAssignedIssue(ArgumentMatchers.anyString(), any())

        viewModel.loadNextTask()

        // Mock update success
        doAnswer {
            val callback = it.arguments[1] as (Boolean) -> Unit
            callback(true)
            null
        }.`when`(mockRepo).updateIssue(any(), any())

        viewModel.processScan("TARGET-SKU")

        assertEquals(1, issue.currentQty)
        assertEquals("FOUND", issue.status)
        verify(statusObserver).onChanged("Status: Item Found! Updating...")
        verify(toastObserver).onChanged("Task Completed!")
    }
}
