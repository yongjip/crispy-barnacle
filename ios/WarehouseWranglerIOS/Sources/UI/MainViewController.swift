import UIKit

final class MainViewController: UIViewController {

    private let viewModel: WarehouseViewModel

    private let statusLabel = UILabel()
    private let stageLabel = UILabel()
    private let externalDisplayLabel = UILabel()
    private let issueLabel = UILabel()
    private let lastScanLabel = UILabel()

    private let manualScanField = UITextField()

    private let simulateScanButton = UIButton(type: .system)
    private let cameraScanButton = UIButton(type: .system)
    private let hudPreviewButton = UIButton(type: .system)
    private let loadNextButton = UIButton(type: .system)
    private let missingButton = UIButton(type: .system)

    init(viewModel: WarehouseViewModel) {
        self.viewModel = viewModel
        super.init(nibName: nil, bundle: nil)
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    override func viewDidLoad() {
        super.viewDidLoad()

        title = "Warehouse Wrangler"
        view.backgroundColor = .systemBackground

        configureUi()

        NotificationCenter.default.addObserver(
            self,
            selector: #selector(onStateChanged),
            name: .warehouseStateDidChange,
            object: viewModel
        )
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(onToast(_:)),
            name: .warehouseToast,
            object: viewModel
        )

        NotificationCenter.default.addObserver(
            self,
            selector: #selector(onScreensChanged),
            name: UIScreen.didConnectNotification,
            object: nil
        )
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(onScreensChanged),
            name: UIScreen.didDisconnectNotification,
            object: nil
        )

        render()
        renderExternalDisplayStatus()
    }

    deinit {
        NotificationCenter.default.removeObserver(self)
    }

    private func configureUi() {
        statusLabel.font = .systemFont(ofSize: 18, weight: .semibold)
        statusLabel.textColor = .label
        statusLabel.numberOfLines = 0

        stageLabel.font = .systemFont(ofSize: 16, weight: .medium)
        stageLabel.textColor = .secondaryLabel

        externalDisplayLabel.font = .systemFont(ofSize: 14, weight: .medium)
        externalDisplayLabel.textColor = .secondaryLabel

        issueLabel.font = .monospacedSystemFont(ofSize: 16, weight: .regular)
        issueLabel.textColor = .label
        issueLabel.numberOfLines = 0

        lastScanLabel.font = .systemFont(ofSize: 14, weight: .medium)
        lastScanLabel.textColor = .secondaryLabel
        lastScanLabel.numberOfLines = 0

        manualScanField.placeholder = "Manual scan (location / barcode)"
        manualScanField.borderStyle = .roundedRect
        manualScanField.autocapitalizationType = .allCharacters
        manualScanField.returnKeyType = .done
        manualScanField.addTarget(self, action: #selector(onManualScanReturn), for: .editingDidEndOnExit)

        simulateScanButton.configuration = .filled()
        simulateScanButton.setTitle("Simulate Scan", for: .normal)
        simulateScanButton.addAction(UIAction { [weak self] _ in self?.submitManualScan() }, for: .touchUpInside)

        cameraScanButton.configuration = .tinted()
        cameraScanButton.setTitle("Camera Scan", for: .normal)
        cameraScanButton.addAction(UIAction { [weak self] _ in self?.startCameraScan() }, for: .touchUpInside)

        hudPreviewButton.configuration = .tinted()
        hudPreviewButton.setTitle("HUD Preview", for: .normal)
        hudPreviewButton.addAction(UIAction { [weak self] _ in self?.openHudPreview() }, for: .touchUpInside)

        loadNextButton.configuration = .filled()
        loadNextButton.setTitle("Load Next Task", for: .normal)
        loadNextButton.addAction(UIAction { [weak self] _ in self?.viewModel.loadNextTask() }, for: .touchUpInside)

        missingButton.configuration = .filled()
        missingButton.configuration?.baseBackgroundColor = .systemRed
        missingButton.setTitle("Mark Missing", for: .normal)
        missingButton.addAction(UIAction { [weak self] _ in self?.confirmMissing() }, for: .touchUpInside)

        let headerStack = UIStackView(arrangedSubviews: [statusLabel, stageLabel, externalDisplayLabel])
        headerStack.axis = .vertical
        headerStack.spacing = 6

        let manualStack = UIStackView(arrangedSubviews: [manualScanField, simulateScanButton])
        manualStack.axis = .vertical
        manualStack.spacing = 10

        let actionsRow1 = UIStackView(arrangedSubviews: [cameraScanButton, hudPreviewButton])
        actionsRow1.axis = .horizontal
        actionsRow1.spacing = 12
        actionsRow1.distribution = .fillEqually

        let actionsRow2 = UIStackView(arrangedSubviews: [missingButton, loadNextButton])
        actionsRow2.axis = .horizontal
        actionsRow2.spacing = 12
        actionsRow2.distribution = .fillEqually

        let rootStack = UIStackView(arrangedSubviews: [
            headerStack,
            issueLabel,
            lastScanLabel,
            manualStack,
            actionsRow1,
            actionsRow2,
        ])

        rootStack.axis = .vertical
        rootStack.spacing = 18

        let scrollView = UIScrollView()
        scrollView.alwaysBounceVertical = true
        scrollView.translatesAutoresizingMaskIntoConstraints = false

        let contentView = UIView()
        contentView.translatesAutoresizingMaskIntoConstraints = false

        view.addSubview(scrollView)
        scrollView.addSubview(contentView)
        contentView.addSubview(rootStack)

        rootStack.translatesAutoresizingMaskIntoConstraints = false

        NSLayoutConstraint.activate([
            scrollView.leadingAnchor.constraint(equalTo: view.safeAreaLayoutGuide.leadingAnchor),
            scrollView.trailingAnchor.constraint(equalTo: view.safeAreaLayoutGuide.trailingAnchor),
            scrollView.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor),
            scrollView.bottomAnchor.constraint(equalTo: view.bottomAnchor),

            contentView.leadingAnchor.constraint(equalTo: scrollView.contentLayoutGuide.leadingAnchor),
            contentView.trailingAnchor.constraint(equalTo: scrollView.contentLayoutGuide.trailingAnchor),
            contentView.topAnchor.constraint(equalTo: scrollView.contentLayoutGuide.topAnchor),
            contentView.bottomAnchor.constraint(equalTo: scrollView.contentLayoutGuide.bottomAnchor),
            contentView.widthAnchor.constraint(equalTo: scrollView.frameLayoutGuide.widthAnchor),

            rootStack.leadingAnchor.constraint(equalTo: contentView.leadingAnchor, constant: 16),
            rootStack.trailingAnchor.constraint(equalTo: contentView.trailingAnchor, constant: -16),
            rootStack.topAnchor.constraint(equalTo: contentView.topAnchor, constant: 16),
            rootStack.bottomAnchor.constraint(equalTo: contentView.bottomAnchor, constant: -16),
        ])
    }

    @objc private func onStateChanged() {
        render()
    }

    @objc private func onToast(_ note: Notification) {
        let message = (note.userInfo?["message"] as? String) ?? ""
        guard !message.isEmpty else { return }
        presentToast(message)
    }

    @objc private func onScreensChanged() {
        renderExternalDisplayStatus()
    }

    private func renderExternalDisplayStatus() {
        let connected = UIScreen.screens.count > 1
        externalDisplayLabel.text = "External display: \(connected ? "Connected" : "Not connected")"
    }

    private func render() {
        statusLabel.text = viewModel.statusMessage
        stageLabel.text = "Stage: \(viewModel.stage.rawValue)"
        lastScanLabel.text = "Last scan: \(viewModel.lastScan)"

        if let issue = viewModel.currentIssue {
            issueLabel.text = [
                "Target Loc: \(issue.targetLocation)",
                "SKU: \(issue.skuId)",
                "BAR: \(issue.barcode)",
                "LOT: \(issue.lotNumber)",
                "Qty: \(issue.progressText)",
            ].joined(separator: "\n")
        } else {
            issueLabel.text = "No active task."
        }
    }

    @objc private func onManualScanReturn() {
        submitManualScan()
    }

    private func submitManualScan() {
        let text = manualScanField.text?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        guard !text.isEmpty else { return }
        viewModel.processScan(text)
        manualScanField.text = ""
    }

    private func startCameraScan() {
        let scanVc = ScanViewController()
        scanVc.delegate = self
        present(scanVc, animated: true)
    }

    private func openHudPreview() {
        let hud = HudViewController(viewModel: viewModel, mode: .preview)
        let nav = UINavigationController(rootViewController: hud)
        nav.modalPresentationStyle = .fullScreen
        present(nav, animated: true)
    }

    private func confirmMissing() {
        let alert = UIAlertController(
            title: "Confirm",
            message: "Mark this item as MISSING?",
            preferredStyle: .alert
        )
        alert.addAction(UIAlertAction(title: "Cancel", style: .cancel))
        alert.addAction(UIAlertAction(title: "Mark Missing", style: .destructive) { [weak self] _ in
            self?.viewModel.markMissing()
        })
        present(alert, animated: true)
    }

    private func presentToast(_ message: String) {
        let alert = UIAlertController(title: nil, message: message, preferredStyle: .alert)
        present(alert, animated: true)
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.0) { [weak alert] in
            alert?.dismiss(animated: true)
        }
    }
}

extension MainViewController: ScanViewControllerDelegate {
    func scanViewController(_ controller: ScanViewController, didScan code: String) {
        controller.dismiss(animated: true)
        viewModel.processScan(code)
    }

    func scanViewControllerDidCancel(_ controller: ScanViewController) {
        controller.dismiss(animated: true)
    }
}

