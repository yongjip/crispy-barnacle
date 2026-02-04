import UIKit

class ExternalSceneDelegate: UIResponder, UIWindowSceneDelegate {

    var window: UIWindow?

    func scene(
        _ scene: UIScene,
        willConnectTo session: UISceneSession,
        options connectionOptions: UIScene.ConnectionOptions
    ) {
        guard let windowScene = scene as? UIWindowScene else { return }

        let hud = HudViewController(viewModel: AppState.shared.viewModel, mode: .externalDisplay)

        let window = UIWindow(windowScene: windowScene)
        window.rootViewController = hud
        window.makeKeyAndVisible()
        self.window = window
    }
}

