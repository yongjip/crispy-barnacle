import Foundation

final class AppState {
    static let shared = AppState()

    let viewModel: WarehouseViewModel

    private init() {
        viewModel = WarehouseViewModel(
            repository: MockWarehouseRepository(),
            userId: "USER_001"
        )
        viewModel.loadNextTask()
    }
}

