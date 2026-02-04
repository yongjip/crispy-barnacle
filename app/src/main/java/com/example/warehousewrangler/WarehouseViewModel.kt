package com.example.warehousewrangler

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.warehousewrangler.models.WarehouseIssue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WarehouseViewModel(private val repository: DynamoDBManager) : ViewModel() {

    private val _currentIssue = MutableLiveData<WarehouseIssue?>()
    val currentIssue: LiveData<WarehouseIssue?> = _currentIssue

    private val _statusMessage = MutableLiveData<String>()
    val statusMessage: LiveData<String> = _statusMessage

    private val _toastMessage = MutableLiveData<String>()
    val toastMessage: LiveData<String> = _toastMessage

    private val _userId = "USER_001" // Hardcoded for now

    fun loadNextTask() {
        _statusMessage.postValue("Status: Loading...")
        repository.fetchAssignedIssue(_userId) { issue ->
            if (issue != null) {
                if (issue.status == "ASSIGNED") {
                    issue.status = "SEARCHING"
                    repository.updateIssue(issue) {
                        // Silent update
                    }
                }
                _currentIssue.postValue(issue)
                _statusMessage.postValue("Status: Searching")
            } else {
                _currentIssue.postValue(null)
                _statusMessage.postValue("Status: No Tasks Assigned")
            }
        }
    }

    fun processScan(data: String) {
        val issue = _currentIssue.value ?: return

        if (data == issue.skuId) {
            issue.currentQty += 1
            _currentIssue.postValue(issue) // Trigger update

            if (issue.currentQty >= issue.targetQty) {
                completeIssue(issue)
            } else {
                _toastMessage.postValue("Item Scanned. ${issue.currentQty}/${issue.targetQty}")
            }
        } else {
            _toastMessage.postValue("Wrong SKU!")
        }
    }

    private fun completeIssue(issue: WarehouseIssue) {
        issue.status = "FOUND"
        _statusMessage.postValue("Status: Item Found! Updating...")

        repository.updateIssue(issue) { success ->
            if (success) {
                _toastMessage.postValue("Task Completed!")
                loadNextTask()
            } else {
                _toastMessage.postValue("Error Updating DB")
            }
        }
    }

    fun markAsMissing() {
        val issue = _currentIssue.value ?: return
        issue.status = "MISSING"
        _statusMessage.postValue("Status: Marking Missing...")

        repository.updateIssue(issue) { success ->
            if (success) {
                _toastMessage.postValue("Marked Missing")
                loadNextTask()
            } else {
                _toastMessage.postValue("Error Updating DB")
            }
        }
    }
}

class WarehouseViewModelFactory(private val repository: DynamoDBManager) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WarehouseViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WarehouseViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
