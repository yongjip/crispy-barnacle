protocol WarehouseRepository {
    func fetchAssignedIssue(userId: String, completion: @escaping (WarehouseIssue?) -> Void)
    func updateIssue(_ issue: WarehouseIssue, completion: @escaping (Bool) -> Void)
}

