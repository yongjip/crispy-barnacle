import Foundation

final class MockWarehouseRepository: WarehouseRepository {

    private let lock = NSLock()
    private var issues: [WarehouseIssue]

    init(seedUserId: String = "USER_001") {
        issues = [
            WarehouseIssue(
                issueId: UUID().uuidString,
                assignedUser: seedUserId,
                status: "ASSIGNED",
                targetLocation: "A-01-01",
                skuId: "SKU-APPLE-001",
                barcode: "8800000000001",
                lotNumber: "LOT-240204-A",
                targetQty: 3,
                currentQty: 0,
                skuImageURL: URL(string: "https://picsum.photos/seed/sku-apple-001/512/512")
            ),
            WarehouseIssue(
                issueId: UUID().uuidString,
                assignedUser: seedUserId,
                status: "ASSIGNED",
                targetLocation: "B-12-07",
                skuId: "SKU-BANANA-002",
                barcode: "8800000000002",
                lotNumber: "LOT-240204-B",
                targetQty: 2,
                currentQty: 0,
                skuImageURL: URL(string: "https://picsum.photos/seed/sku-banana-002/512/512")
            ),
            WarehouseIssue(
                issueId: UUID().uuidString,
                assignedUser: seedUserId,
                status: "ASSIGNED",
                targetLocation: "C-03-15",
                skuId: "SKU-CARROT-003",
                barcode: "8800000000003",
                lotNumber: "LOT-240204-C",
                targetQty: 1,
                currentQty: 0,
                skuImageURL: URL(string: "https://picsum.photos/seed/sku-carrot-003/512/512")
            ),
        ]
    }

    func fetchAssignedIssue(userId: String, completion: @escaping (WarehouseIssue?) -> Void) {
        lock.lock()
        let issue = issues.first(where: { $0.assignedUser == userId && $0.status == "ASSIGNED" })
        lock.unlock()
        completion(issue)
    }

    func updateIssue(_ issue: WarehouseIssue, completion: @escaping (Bool) -> Void) {
        lock.lock()
        if let idx = issues.firstIndex(where: { $0.issueId == issue.issueId }) {
            issues[idx] = issue
        }
        lock.unlock()
        completion(true)
    }
}

