import Foundation

extension Notification.Name {
    static let warehouseStateDidChange = Notification.Name("WarehouseViewModel.warehouseStateDidChange")
    static let warehouseToast = Notification.Name("WarehouseViewModel.warehouseToast")
}

final class WarehouseViewModel {

    private let repository: WarehouseRepository
    private let userId: String

    private(set) var currentIssue: WarehouseIssue?
    private(set) var stage: PickScanStage = .confirmLocation
    private(set) var statusMessage: String = "Status: Idle"
    private(set) var lastScan: String = "None"

    init(repository: WarehouseRepository, userId: String) {
        self.repository = repository
        self.userId = userId
    }

    func loadNextTask() {
        statusMessage = "Status: Loading..."
        notifyStateChanged()

        repository.fetchAssignedIssue(userId: userId) { [weak self] issue in
            guard let self else { return }
            DispatchQueue.main.async {
                if var issue = issue {
                    if issue.status == "ASSIGNED" {
                        issue.status = "SEARCHING"
                        self.repository.updateIssue(issue) { _ in }
                    }

                    self.currentIssue = issue
                    self.stage = .confirmLocation
                    self.statusMessage = "Status: Scan Location"
                } else {
                    self.currentIssue = nil
                    self.stage = .confirmLocation
                    self.statusMessage = "Status: No Tasks Assigned"
                }

                self.lastScan = "None"
                self.notifyStateChanged()
            }
        }
    }

    func processScan(_ data: String) {
        let scanned = data.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !scanned.isEmpty else { return }

        lastScan = scanned

        guard var issue = currentIssue else {
            notifyStateChanged()
            return
        }

        let targetLocation = issue.targetLocation.trimmingCharacters(in: .whitespacesAndNewlines)

        if stage == .confirmLocation {
            if scanned.caseInsensitiveCompare(targetLocation) == .orderedSame {
                stage = .scanItem
                statusMessage = "Status: Scan Item"
                postToast("Location confirmed: \(targetLocation)")
            } else {
                postToast("Wrong Location!")
            }
            notifyStateChanged()
            return
        }

        if scanned.caseInsensitiveCompare(targetLocation) == .orderedSame {
            postToast("Location confirmed: \(targetLocation)")
            notifyStateChanged()
            return
        }

        let matchesSku = scanned.caseInsensitiveCompare(issue.skuId) == .orderedSame ||
            scanned.caseInsensitiveCompare(issue.barcode) == .orderedSame

        if matchesSku {
            issue.currentQty += 1
            currentIssue = issue
            notifyStateChanged()

            if issue.currentQty >= issue.targetQty {
                completeIssue(issue)
            } else {
                postToast("Item scanned. \(issue.progressText)")
            }
        } else {
            postToast("Wrong SKU!")
            notifyStateChanged()
        }
    }

    func markMissing() {
        guard var issue = currentIssue else { return }
        issue.status = "MISSING"
        currentIssue = issue
        statusMessage = "Status: Marking Missing..."
        notifyStateChanged()

        repository.updateIssue(issue) { [weak self] success in
            DispatchQueue.main.async {
                if success {
                    self?.postToast("Marked Missing")
                    self?.loadNextTask()
                } else {
                    self?.postToast("Error updating task")
                }
            }
        }
    }

    private func completeIssue(_ issue: WarehouseIssue) {
        var issue = issue
        issue.status = "FOUND"
        currentIssue = issue
        statusMessage = "Status: Task Found! Updating..."
        notifyStateChanged()

        repository.updateIssue(issue) { [weak self] success in
            DispatchQueue.main.async {
                if success {
                    self?.postToast("Task Completed!")
                    self?.loadNextTask()
                } else {
                    self?.postToast("Error updating task")
                }
            }
        }
    }

    private func notifyStateChanged() {
        if Thread.isMainThread {
            NotificationCenter.default.post(name: .warehouseStateDidChange, object: self)
        } else {
            DispatchQueue.main.async {
                NotificationCenter.default.post(name: .warehouseStateDidChange, object: self)
            }
        }
    }

    private func postToast(_ message: String) {
        if Thread.isMainThread {
            NotificationCenter.default.post(
                name: .warehouseToast,
                object: self,
                userInfo: ["message": message]
            )
        } else {
            DispatchQueue.main.async {
                NotificationCenter.default.post(
                    name: .warehouseToast,
                    object: self,
                    userInfo: ["message": message]
                )
            }
        }
    }
}

