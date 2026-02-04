import UIKit
import ObjectiveC.runtime

final class ImageLoader {
    static let shared = ImageLoader()

    private let cache = NSCache<NSURL, UIImage>()
    private let session: URLSession

    private init() {
        let config = URLSessionConfiguration.default
        config.requestCachePolicy = .returnCacheDataElseLoad
        session = URLSession(configuration: config)
    }

    func load(_ url: URL, into imageView: UIImageView) {
        imageView.ww_currentImageURL = url

        let key = url as NSURL
        if let cached = cache.object(forKey: key) {
            imageView.image = cached
            return
        }

        imageView.image = nil

        session.dataTask(with: url) { [weak self, weak imageView] data, _, _ in
            guard let self, let imageView else { return }
            guard let data, let image = UIImage(data: data) else { return }

            self.cache.setObject(image, forKey: key)
            DispatchQueue.main.async {
                // Avoid setting an image into a reused imageView that moved on to another URL.
                guard imageView.ww_currentImageURL == url else { return }
                imageView.image = image
            }
        }.resume()
    }
}

private var wwImageURLKey: UInt8 = 0

extension UIImageView {
    fileprivate var ww_currentImageURL: URL? {
        get { objc_getAssociatedObject(self, &wwImageURLKey) as? URL }
        set { objc_setAssociatedObject(self, &wwImageURLKey, newValue, .OBJC_ASSOCIATION_RETAIN_NONATOMIC) }
    }
}

