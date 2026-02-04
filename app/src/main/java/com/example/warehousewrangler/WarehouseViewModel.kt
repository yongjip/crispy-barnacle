package com.example.warehousewrangler

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.warehousewrangler.models.WarehouseIssue

enum class PickScanStage {
    CONFIRM_LOCATION,
    SCAN_ITEM
}

class WarehouseViewModel(private val repository: WarehouseRepository) : ViewModel() {

    private val _currentIssue = MutableLiveData<WarehouseIssue?>()
    val currentIssue: LiveData<WarehouseIssue?> = _currentIssue

    private val _statusMessage = MutableLiveData<String>()
    val statusMessage: LiveData<String> = _statusMessage

    private val _toastMessage = MutableLiveData<String>()
    val toastMessage: LiveData<String> = _toastMessage

    private val _pickScanStage = MutableLiveData<PickScanStage>()
    val pickScanStage: LiveData<PickScanStage> = _pickScanStage

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
                // Default picking flow: confirm location first, then scan item(s).
                val needsLocationConfirm = !issue.targetLocation.isNullOrBlank() || !issue.locationCode.isNullOrBlank()
                _pickScanStage.postValue(if (needsLocationConfirm) PickScanStage.CONFIRM_LOCATION else PickScanStage.SCAN_ITEM)
                _statusMessage.postValue(if (needsLocationConfirm) "Status: Scan Location" else "Status: Scan Item")
            } else {
                _currentIssue.postValue(null)
                _pickScanStage.postValue(PickScanStage.CONFIRM_LOCATION)
                _statusMessage.postValue("Status: No Tasks Assigned")
            }
        }
    }

    fun processScan(data: String) {
        val issue = _currentIssue.value ?: return
        val scanned = data.trim()

        val targetLocation = (issue.targetLocation ?: issue.locationCode)?.trim()
        val skuId = issue.skuId?.trim()
        val barcode = issue.barcode?.trim()

        val stage = _pickScanStage.value ?: PickScanStage.CONFIRM_LOCATION

        if (stage == PickScanStage.CONFIRM_LOCATION) {
            if (!targetLocation.isNullOrBlank() && scanned.equals(targetLocation, ignoreCase = true)) {
                _pickScanStage.postValue(PickScanStage.SCAN_ITEM)
                _statusMessage.postValue("Status: Scan Item")
                _toastMessage.postValue("Location confirmed: $targetLocation")
            } else {
                _toastMessage.postValue("Wrong Location!")
            }
            return
        }

        if (!targetLocation.isNullOrBlank() && scanned.equals(targetLocation, ignoreCase = true)) {
            // Allow re-scanning location while in item stage without showing "Wrong SKU".
            _toastMessage.postValue("Location confirmed: $targetLocation")
            return
        }

        val matchesSku = (!skuId.isNullOrBlank() && scanned.equals(skuId, ignoreCase = true)) ||
            (!barcode.isNullOrBlank() && scanned.equals(barcode, ignoreCase = true))

        if (matchesSku) {
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

class WarehouseViewModelFactory(private val repository: WarehouseRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WarehouseViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WarehouseViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
