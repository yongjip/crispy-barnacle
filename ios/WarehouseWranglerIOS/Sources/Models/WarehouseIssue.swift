import Foundation

struct WarehouseIssue: Equatable {
    var issueId: String
    var assignedUser: String
    var status: String

    // Picking location (where the picker should go).
    var targetLocation: String

    var skuId: String
    var barcode: String
    var lotNumber: String

    var targetQty: Int
    var currentQty: Int

    var skuImageURL: URL?
}

extension WarehouseIssue {
    var progressText: String { "\(currentQty)/\(targetQty)" }
}

