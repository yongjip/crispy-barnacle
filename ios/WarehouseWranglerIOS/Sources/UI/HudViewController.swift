import UIKit

enum HudMode {
    case preview
    case externalDisplay
}

final class HudViewController: UIViewController {

    private let viewModel: WarehouseViewModel
    private let mode: HudMode

    private let locationLabel = UILabel()
    private let skuLabel = UILabel()
    private let barcodeLabel = UILabel()
    private let lotLabel = UILabel()
    private let progressLabel = UILabel()
    private let unitsLabel = UILabel()
    private let stageLabel = UILabel()
    private let skuImageView = UIImageView()

    init(viewModel: WarehouseViewModel, mode: HudMode) {
        self.viewModel = viewModel
        self.mode = mode
        super.init(nibName: nil, bundle: nil)
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    override func viewDidLoad() {
        super.viewDidLoad()

        view.backgroundColor = .black

        if mode == .preview {
            title = "HUD Preview"
            navigationItem.rightBarButtonItem = UIBarButtonItem(
                systemItem: .close,
                primaryAction: UIAction { [weak self] _ in self?.dismiss(animated: true) }
            )
        }

        configureUi()

        NotificationCenter.default.addObserver(
            self,
            selector: #selector(onStateChanged),
            name: .warehouseStateDidChange,
            object: viewModel
        )

        render()
    }

    deinit {
        NotificationCenter.default.removeObserver(self)
    }

    override var prefersStatusBarHidden: Bool { true }

    private func configureUi() {
        locationLabel.text = "LOC: --"
        locationLabel.textColor = UIColor(red: 0.0, green: 1.0, blue: 0.3, alpha: 1.0)
        locationLabel.font = .systemFont(ofSize: 56, weight: .bold)
        locationLabel.textAlignment = .center
        locationLabel.numberOfLines = 1
        locationLabel.adjustsFontSizeToFitWidth = true
        locationLabel.minimumScaleFactor = 0.6

        skuImageView.backgroundColor = UIColor(white: 0.15, alpha: 1.0)
        skuImageView.contentMode = .scaleAspectFill
        skuImageView.clipsToBounds = true
        skuImageView.layer.cornerRadius = 18
        skuImageView.translatesAutoresizingMaskIntoConstraints = false

        skuLabel.text = "SKU: --"
        skuLabel.textColor = .white
        skuLabel.font = .systemFont(ofSize: 38, weight: .bold)
        skuLabel.numberOfLines = 1
        skuLabel.adjustsFontSizeToFitWidth = true
        skuLabel.minimumScaleFactor = 0.6

        barcodeLabel.text = "BAR: --"
        barcodeLabel.textColor = UIColor(white: 0.85, alpha: 1.0)
        barcodeLabel.font = .systemFont(ofSize: 22, weight: .medium)

        lotLabel.text = "LOT: --"
        lotLabel.textColor = UIColor(white: 0.85, alpha: 1.0)
        lotLabel.font = .systemFont(ofSize: 22, weight: .medium)

        progressLabel.text = "--/--"
        progressLabel.textColor = UIColor(red: 0.0, green: 1.0, blue: 1.0, alpha: 1.0)
        progressLabel.font = .monospacedDigitSystemFont(ofSize: 84, weight: .bold)
        progressLabel.textAlignment = .center

        unitsLabel.text = "UNITS"
        unitsLabel.textColor = UIColor(white: 0.65, alpha: 1.0)
        unitsLabel.font = .systemFont(ofSize: 22, weight: .semibold)
        unitsLabel.textAlignment = .center

        stageLabel.text = ""
        stageLabel.textColor = UIColor(red: 0.0, green: 1.0, blue: 1.0, alpha: 1.0)
        stageLabel.font = .systemFont(ofSize: 24, weight: .bold)
        stageLabel.textAlignment = .center

        let separator = UIView()
        separator.backgroundColor = .white
        separator.translatesAutoresizingMaskIntoConstraints = false
        NSLayoutConstraint.activate([
            separator.heightAnchor.constraint(equalToConstant: 4),
            separator.widthAnchor.constraint(equalToConstant: 220),
        ])

        let detailsTextStack = UIStackView(arrangedSubviews: [skuLabel, barcodeLabel, lotLabel])
        detailsTextStack.axis = .vertical
        detailsTextStack.spacing = 8

        let detailsStack = UIStackView(arrangedSubviews: [skuImageView, detailsTextStack])
        detailsStack.axis = .horizontal
        detailsStack.alignment = .center
        detailsStack.spacing = 18

        NSLayoutConstraint.activate([
            skuImageView.widthAnchor.constraint(equalToConstant: 220),
            skuImageView.heightAnchor.constraint(equalToConstant: 220),
        ])

        let rootStack = UIStackView(arrangedSubviews: [
            locationLabel,
            separator,
            detailsStack,
            progressLabel,
            unitsLabel,
            stageLabel,
        ])

        rootStack.axis = .vertical
        rootStack.alignment = .center
        rootStack.spacing = 22
        rootStack.translatesAutoresizingMaskIntoConstraints = false

        view.addSubview(rootStack)
        NSLayoutConstraint.activate([
            rootStack.leadingAnchor.constraint(equalTo: view.safeAreaLayoutGuide.leadingAnchor, constant: 24),
            rootStack.trailingAnchor.constraint(equalTo: view.safeAreaLayoutGuide.trailingAnchor, constant: -24),
            rootStack.centerYAnchor.constraint(equalTo: view.safeAreaLayoutGuide.centerYAnchor),
        ])
    }

    @objc private func onStateChanged() {
        render()
    }

    private func render() {
        guard let issue = viewModel.currentIssue else {
            locationLabel.text = "No Active Issue"
            skuLabel.text = ""
            barcodeLabel.text = ""
            lotLabel.text = ""
            progressLabel.text = ""
            stageLabel.text = ""
            skuImageView.image = nil
            return
        }

        locationLabel.text = "LOC: \(issue.targetLocation)"
        skuLabel.text = "SKU: \(issue.skuId)"
        barcodeLabel.text = "BAR: \(issue.barcode)"
        lotLabel.text = "LOT: \(issue.lotNumber)"
        progressLabel.text = issue.progressText
        stageLabel.text = viewModel.stage.rawValue

        if let url = issue.skuImageURL {
            ImageLoader.shared.load(url, into: skuImageView)
        } else {
            skuImageView.image = nil
        }
    }
}

