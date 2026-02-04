import UIKit
import AVFoundation

protocol ScanViewControllerDelegate: AnyObject {
    func scanViewController(_ controller: ScanViewController, didScan code: String)
    func scanViewControllerDidCancel(_ controller: ScanViewController)
}

final class ScanViewController: UIViewController {

    weak var delegate: ScanViewControllerDelegate?

    private let session = AVCaptureSession()
    private var previewLayer: AVCaptureVideoPreviewLayer?
    private var isHandlingScan = false

    private let instructionLabel = UILabel()

    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = .black

        configureUi()
        setupCamera()
    }

    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        previewLayer?.frame = view.bounds
    }

    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        if session.isRunning {
            session.stopRunning()
        }
    }

    private func configureUi() {
        let close = UIButton(type: .system)
        close.configuration = .plain()
        close.configuration?.image = UIImage(systemName: "xmark.circle.fill")
        close.configuration?.baseForegroundColor = .white
        close.addAction(UIAction { [weak self] _ in
            guard let self else { return }
            self.delegate?.scanViewControllerDidCancel(self)
        }, for: .touchUpInside)
        close.translatesAutoresizingMaskIntoConstraints = false

        instructionLabel.text = "Point the camera at a barcode"
        instructionLabel.textColor = .white
        instructionLabel.font = .systemFont(ofSize: 18, weight: .semibold)
        instructionLabel.textAlignment = .center
        instructionLabel.translatesAutoresizingMaskIntoConstraints = false

        view.addSubview(close)
        view.addSubview(instructionLabel)

        NSLayoutConstraint.activate([
            close.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor, constant: 16),
            close.trailingAnchor.constraint(equalTo: view.safeAreaLayoutGuide.trailingAnchor, constant: -16),

            instructionLabel.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 16),
            instructionLabel.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -16),
            instructionLabel.bottomAnchor.constraint(equalTo: view.safeAreaLayoutGuide.bottomAnchor, constant: -24),
        ])
    }

    private func setupCamera() {
        switch AVCaptureDevice.authorizationStatus(for: .video) {
        case .authorized:
            configureSession()
        case .notDetermined:
            AVCaptureDevice.requestAccess(for: .video) { [weak self] granted in
                DispatchQueue.main.async {
                    guard let self else { return }
                    if granted {
                        self.configureSession()
                    } else {
                        self.showCameraDenied()
                    }
                }
            }
        default:
            showCameraDenied()
        }
    }

    private func showCameraDenied() {
        instructionLabel.text = "Camera permission is required to scan.\nEnable it in Settings."
        instructionLabel.numberOfLines = 0
        instructionLabel.textAlignment = .center
    }

    private func configureSession() {
        guard let device = AVCaptureDevice.default(for: .video) else { return }
        guard let input = try? AVCaptureDeviceInput(device: device) else { return }

        if session.canAddInput(input) {
            session.addInput(input)
        }

        let metadataOutput = AVCaptureMetadataOutput()
        if session.canAddOutput(metadataOutput) {
            session.addOutput(metadataOutput)
            metadataOutput.setMetadataObjectsDelegate(self, queue: DispatchQueue.main)
            metadataOutput.metadataObjectTypes = [
                .ean8,
                .ean13,
                .code128,
                .qr,
                .dataMatrix,
                .pdf417,
            ]
        }

        let preview = AVCaptureVideoPreviewLayer(session: session)
        preview.videoGravity = .resizeAspectFill
        preview.frame = view.bounds
        view.layer.insertSublayer(preview, at: 0)
        previewLayer = preview

        session.startRunning()
    }
}

extension ScanViewController: AVCaptureMetadataOutputObjectsDelegate {
    func metadataOutput(
        _ output: AVCaptureMetadataOutput,
        didOutput metadataObjects: [AVMetadataObject],
        from connection: AVCaptureConnection
    ) {
        guard !isHandlingScan else { return }
        guard let object = metadataObjects.first as? AVMetadataMachineReadableCodeObject else { return }
        guard let code = object.stringValue, !code.isEmpty else { return }

        isHandlingScan = true
        UIImpactFeedbackGenerator(style: .medium).impactOccurred()
        delegate?.scanViewController(self, didScan: code)
    }
}

