import UIKit
import Darwin
import CryptoKit
import CoreText
import UniformTypeIdentifiers
import LocalAuthentication
import UserNotifications
import GoogleSignIn
import FirebaseCore
import FirebaseAuth
import FirebaseStorage
import PhotosUI
import ImageIO
import KasivaShared

final class IOSNativeServices: NSObject, NativeServices, UIDocumentPickerDelegate, UNUserNotificationCenterDelegate, PHPickerViewControllerDelegate {
    func localFinancePersistence() -> any LocalFinancePersistence { IOSLocalFinancePersistence.shared }
    func closeApp() -> Bool { false }
    func appVersion() -> String { Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? "1.0.0" }
    func deleteLocalProfilePhoto() -> String? {
        do {
            let support = try FileManager.default.url(for: .applicationSupportDirectory, in: .userDomainMask, appropriateFor: nil, create: false)
            let file = support.appendingPathComponent("KasivaLocal/avatar.jpg")
            if FileManager.default.fileExists(atPath: file.path) { try FileManager.default.removeItem(at: file) }
            return nil
        } catch { return "Data keuangan dihapus, tetapi foto lokal belum dapat dibersihkan." }
    }
    func composeSupport(subject: String, body: String) -> String? {
        var parts = URLComponents()
        parts.scheme = "mailto"
        parts.path = "muzafinarifin.dev@gmail.com"
        parts.queryItems = [URLQueryItem(name: "subject", value: subject), URLQueryItem(name: "body", value: body)]
        guard let url = parts.url, UIApplication.shared.canOpenURL(url) else { return "Aplikasi email belum tersedia" }
        UIApplication.shared.open(url)
        return nil
    }
    func shareApp() -> String? { "Kasiva belum memiliki tautan App Store. Fitur berbagi instalasi iOS tersedia setelah aplikasi diterbitkan." }
    private var photoCallback: ((KotlinByteArray?, String?) -> KotlinUnit)?
    private var photoStorage: Storage { Storage.storage(url: "gs://finflow-logikawan.firebasestorage.app") }
    private func kotlinBytes(_ data: Data) -> KotlinByteArray {
        let bytes = KotlinByteArray(size: Int32(data.count))
        for (index, value) in data.enumerated() { bytes.set(index: Int32(index), value: Int8(bitPattern: value)) }
        return bytes
    }
    func pickProfilePhoto(result: @escaping (KotlinByteArray?, String?) -> KotlinUnit) {
        guard photoCallback == nil, let presenter = presenter else { _ = result(nil, "Pemilih foto belum tersedia"); return }
        photoCallback = result
        var config = PHPickerConfiguration()
        config.selectionLimit = 1
        config.filter = .images
        let picker = PHPickerViewController(configuration: config)
        picker.delegate = self
        presenter.present(picker, animated: true)
    }
    func picker(_ picker: PHPickerViewController, didFinishPicking results: [PHPickerResult]) {
        picker.dismiss(animated: true)
        let callback = photoCallback
        photoCallback = nil
        guard let item = results.first else { _ = callback?(nil, nil); return }
        item.itemProvider.loadFileRepresentation(forTypeIdentifier: UTType.image.identifier) { url, error in
            var output: Data?
            var failure: String?
            if let url = url,
               let size = try? url.resourceValues(forKeys: [.fileSizeKey]).fileSize, size <= 5 * 1024 * 1024,
               let source = CGImageSourceCreateWithURL(url as CFURL, nil),
               let image = CGImageSourceCreateThumbnailAtIndex(source, 0, [
                kCGImageSourceCreateThumbnailFromImageAlways: true,
                kCGImageSourceCreateThumbnailWithTransform: true,
                kCGImageSourceThumbnailMaxPixelSize: 1024,
               ] as CFDictionary), let data = UIImage(cgImage: image).jpegData(compressionQuality: 0.85), data.count <= 2 * 1024 * 1024 {
                output = data
            } else { failure = error?.localizedDescription ?? "Pilih foto yang valid, maksimal 5 MB" }
            DispatchQueue.main.async { _ = callback?(output.map { self.kotlinBytes($0) }, failure) }
        }
    }
    func uploadProfilePhoto(userId: String, bytes: KotlinByteArray, result: @escaping (String?, String?) -> KotlinUnit) {
        if userId == "local" {
            do {
                guard bytes.size > 0, bytes.size <= 2 * 1024 * 1024 else { _ = result(nil, "Ukuran foto tidak valid"); return }
                let support = try FileManager.default.url(for: .applicationSupportDirectory, in: .userDomainMask, appropriateFor: nil, create: true)
                let directory = support.appendingPathComponent("KasivaLocal", isDirectory: true)
                try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
                let data = Data((0..<Int(bytes.size)).map { UInt8(bitPattern: bytes.get(index: Int32($0))) })
                try data.write(to: directory.appendingPathComponent("avatar.jpg"), options: [.atomic, .completeFileProtectionUntilFirstUserAuthentication])
                _ = result("local/profile/avatar.jpg", nil)
            } catch { _ = result(nil, "Foto lokal gagal disimpan") }
            return
        }
        guard Auth.auth().currentUser?.uid == userId, bytes.size > 0, bytes.size <= 2 * 1024 * 1024 else {
            _ = result(nil, "Akun atau ukuran foto tidak valid"); return
        }
        let data = Data((0..<Int(bytes.size)).map { UInt8(bitPattern: bytes.get(index: Int32($0))) })
        let path = "users/\(userId)/profile/avatar.jpg"
        let metadata = StorageMetadata()
        metadata.contentType = "image/jpeg"
        photoStorage.reference(withPath: path).putData(data, metadata: metadata) { _, error in
            DispatchQueue.main.async { _ = result(error == nil ? path : nil, error?.localizedDescription) }
        }
    }
    func loadProfilePhoto(path: String, result: @escaping (KotlinByteArray?, String?) -> KotlinUnit) {
        if path == "local/profile/avatar.jpg" {
            do {
                let support = try FileManager.default.url(for: .applicationSupportDirectory, in: .userDomainMask, appropriateFor: nil, create: false)
                let file = support.appendingPathComponent("KasivaLocal/avatar.jpg")
                let size = try file.resourceValues(forKeys: [.fileSizeKey]).fileSize ?? 0
                guard size > 0, size <= 2 * 1024 * 1024 else { _ = result(nil, "Foto lokal belum dapat dimuat"); return }
                _ = result(kotlinBytes(try Data(contentsOf: file)), nil)
            } catch { _ = result(nil, "Foto lokal belum dapat dimuat") }
            return
        }
        guard let uid = Auth.auth().currentUser?.uid, path == "users/\(uid)/profile/avatar.jpg" else {
            _ = result(nil, "Akses foto ditolak"); return
        }
        photoStorage.reference(withPath: path).getData(maxSize: 2 * 1024 * 1024) { data, error in
            DispatchQueue.main.async { _ = result(data.map { self.kotlinBytes($0) }, error?.localizedDescription) }
        }
    }
    private var csvCallback: ((String?, String?) -> KotlinUnit)?
    private let preferences = UserDefaults.standard
    private var pendingBudgetAlerts = Set<String>()
    override init() {
        super.init()
        UNUserNotificationCenter.current().delegate = self
    }
    func onboardingComplete() -> Bool { preferences.bool(forKey: "finflow.onboarding.complete") }
    func readSetting(key: String) -> String? { preferences.string(forKey: "finflow." + key) }
    func sha256(text: String) -> String { SHA256.hash(data: Data(text.utf8)).map { String(format: "%02x", $0) }.joined() }
    func writeSetting(key: String, value: String) {
        preferences.set(value, forKey: "finflow." + key)
        if key == "data.mode" || key.hasPrefix("local.sync.") || key.hasPrefix("restore.") || key.hasPrefix("reset.") { preferences.synchronize() }
    }
    func setOnboardingComplete() { preferences.set(true, forKey: "finflow.onboarding.complete") }
    func setNotificationImportUser(userId: String?) {}
    private var presenter: UIViewController? {
        let root = UIApplication.shared.connectedScenes.compactMap { $0 as? UIWindowScene }
            .flatMap { $0.windows }.first { $0.isKeyWindow }?.rootViewController
        var current = root
        while let next = current?.presentedViewController { current = next }
        return current
    }

    func pickCsv(result: @escaping (String?, String?) -> KotlinUnit) {
        guard let presenter = presenter else { _ = result(nil, "Tidak dapat membuka pemilih file"); return }
        csvCallback = result
        let picker = UIDocumentPickerViewController(forOpeningContentTypes: [.commaSeparatedText, .plainText, .json], asCopy: true)
        picker.delegate = self
        picker.allowsMultipleSelection = false
        presenter.present(picker, animated: true)
    }
    func documentPickerWasCancelled(_ controller: UIDocumentPickerViewController) {
        _ = csvCallback?(nil, nil); csvCallback = nil
    }
    func documentPicker(_ controller: UIDocumentPickerViewController, didPickDocumentsAt urls: [URL]) {
        let callback = csvCallback; csvCallback = nil
        guard let url = urls.first else { _ = callback?(nil, nil); return }
        let scoped = url.startAccessingSecurityScopedResource()
        defer { if scoped { url.stopAccessingSecurityScopedResource() } }
        do {
            let attributes = try FileManager.default.attributesOfItem(atPath: url.path)
            guard let size = attributes[.size] as? NSNumber, size.intValue <= 5 * 1024 * 1024 else {
                _ = callback?(nil, "File maksimal 5 MB"); return
            }
            let data = try Data(contentsOf: url)
            guard let text = String(data: data, encoding: .utf8) else { _ = callback?(nil, "Gunakan file UTF-8"); return }
            _ = callback?(text, nil)
        } catch { _ = callback?(nil, error.localizedDescription) }
    }
    func shareCsv(name: String, content: String) {
        shareTextFile(name: name, content: content, mimeType: "text/csv")
    }
    func shareTextFile(name: String, content: String, mimeType: String) {
        do {
            guard content.utf8.count <= 5 * 1024 * 1024 else {
                throw NSError(domain: "KasivaExport", code: 1, userInfo: [NSLocalizedDescriptionKey: "File ekspor maksimal 5 MB"])
            }
            let file = try exportFile(name: name)
            try content.write(to: file, atomically: true, encoding: .utf8)
            shareExport(file: file)
        } catch { exportError(error) }
    }
    func deviceDescription() -> String {
        let device = UIDevice.current
        return "\(device.model) · \(device.systemName) \(device.systemVersion)"
    }
    func deviceDiagnostics() -> String {
        let screen = UIScreen.main
        var system = utsname()
        uname(&system)
        let model = withUnsafeBytes(of: system.machine) { String(decoding: $0.prefix { $0 != 0 }, as: UTF8.self) }
        return "Perangkat: \(deviceDescription()) (\(model))\nLayar: \(screen.nativeBounds.width) × \(screen.nativeBounds.height) px, skala \(screen.scale)\nUkuran teks: \(UIApplication.shared.preferredContentSizeCategory.rawValue)\nBahasa sistem: \(Locale.preferredLanguages.joined(separator: ", "))\nZona waktu: \(TimeZone.current.identifier)\nBuild aplikasi: \(Bundle.main.object(forInfoDictionaryKey: "CFBundleVersion") as? String ?? "1")"
    }
    func sharePdf(name: String, title: String, lines: [String]) {
        do {
            guard lines.count <= 25_000, lines.reduce(0, { $0 + $1.utf16.count }) <= 5 * 1024 * 1024 else {
                throw NSError(domain: "KasivaExport", code: 1, userInfo: [NSLocalizedDescriptionKey: "PDF terlalu besar. Persempit periode ekspor."])
            }
            let file = try exportFile(name: name)
            let bounds = CGRect(x: 0, y: 0, width: 595, height: 842)
            let margin: CGFloat = 36
            let width = bounds.width - margin * 2
            let titleAttributes: [NSAttributedString.Key: Any] = [.font: UIFont.boldSystemFont(ofSize: 18), .foregroundColor: UIColor.black]
            let titleText = NSAttributedString(string: title, attributes: titleAttributes)
            let titleHeight = min(50, titleText.boundingRect(with: CGSize(width: width, height: 50), options: [.usesLineFragmentOrigin, .usesFontLeading], context: nil).height)
            let paragraph = NSMutableParagraphStyle()
            paragraph.lineSpacing = 3
            paragraph.lineBreakMode = .byWordWrapping
            let text = lines.joined(separator: "\n")
            let body = NSAttributedString(string: text.isEmpty ? "Tidak ada catatan pada periode ini." : text, attributes: [
                .font: UIFont.systemFont(ofSize: 11), .foregroundColor: UIColor.darkGray, .paragraphStyle: paragraph,
            ])
            let framesetter = CTFramesetterCreateWithAttributedString(body as CFAttributedString)
            var incomplete = false
            let data = UIGraphicsPDFRenderer(bounds: bounds).pdfData { context in
                var offset = 0
                var page = 0
                repeat {
                    guard page < 500 else { incomplete = true; break }
                    page += 1
                    context.beginPage()
                    titleText.draw(with: CGRect(x: margin, y: margin, width: width, height: 50), options: [.usesLineFragmentOrigin, .usesFontLeading], context: nil)
                    let footer = "Kasiva · Halaman \(page)" as NSString
                    footer.draw(at: CGPoint(x: margin, y: bounds.height - margin - 9), withAttributes: [.font: UIFont.systemFont(ofSize: 9), .foregroundColor: UIColor.gray])
                    let top = margin + titleHeight + 20
                    let bodyRect = CGRect(x: margin, y: margin + 20, width: width, height: bounds.height - top - margin - 20)
                    let frame = CTFramesetterCreateFrame(framesetter, CFRange(location: offset, length: 0), CGPath(rect: bodyRect, transform: nil), nil)
                    let visible = CTFrameGetVisibleStringRange(frame)
                    guard visible.length > 0 else { incomplete = true; break }
                    let canvas = context.cgContext
                    canvas.saveGState()
                    canvas.textMatrix = .identity
                    canvas.translateBy(x: 0, y: bounds.height)
                    canvas.scaleBy(x: 1, y: -1)
                    CTFrameDraw(frame, canvas)
                    canvas.restoreGState()
                    offset += visible.length
                } while offset < body.length
            }
            guard !incomplete else {
                throw NSError(domain: "KasivaExport", code: 2, userInfo: [NSLocalizedDescriptionKey: "PDF belum dapat diselesaikan. Persempit periode ekspor."])
            }
            try data.write(to: file, options: .atomic)
            shareExport(file: file)
        } catch { exportError(error) }
    }
    private func exportFile(name: String) throws -> URL {
        let directory = FileManager.default.temporaryDirectory.appendingPathComponent("exports", isDirectory: true)
        try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        let allowed = CharacterSet(charactersIn: "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789._-")
        let clean = String(name.unicodeScalars.map { allowed.contains($0) ? Character(String($0)) : "_" }.suffix(120))
        let filename = clean.isEmpty || clean == "." || clean == ".." ? "kasiva-export" : clean
        return directory.appendingPathComponent(filename)
    }
    private func shareExport(file: URL) {
        guard let presenter = presenter else { return }
        let sheet = UIActivityViewController(activityItems: [file], applicationActivities: nil)
        sheet.popoverPresentationController?.sourceView = presenter.view
        sheet.popoverPresentationController?.sourceRect = CGRect(x: presenter.view.bounds.midX, y: presenter.view.bounds.midY, width: 1, height: 1)
        presenter.present(sheet, animated: true)
    }
    private func exportError(_ error: Error) {
        guard let presenter = presenter else { return }
        let alert = UIAlertController(title: "Ekspor gagal", message: error.localizedDescription, preferredStyle: .alert)
        alert.addAction(UIAlertAction(title: "Tutup", style: .default))
        presenter.present(alert, animated: true)
    }
    func googleSignIn(result: @escaping (String?, String?) -> KotlinUnit) {
        guard let clientID = FirebaseApp.app()?.options.clientID, let presenter = presenter else {
            _ = result(nil, "Konfigurasi Google Sign-In iOS belum lengkap"); return
        }
        GIDSignIn.sharedInstance.configuration = GIDConfiguration(clientID: clientID)
        GIDSignIn.sharedInstance.signIn(withPresenting: presenter) { response, error in
            DispatchQueue.main.async { _ = result(response?.user.idToken?.tokenString, error?.localizedDescription) }
        }
    }
    func unlock(result: @escaping (KotlinBoolean) -> KotlinUnit) {
        let context = LAContext()
        var error: NSError?
        guard context.canEvaluatePolicy(.deviceOwnerAuthentication, error: &error) else {
            _ = result(KotlinBoolean(bool: false)); return
        }
        context.evaluatePolicy(.deviceOwnerAuthentication, localizedReason: "Buka Kasiva untuk melihat data keuangan") { success, _ in
            DispatchQueue.main.async { _ = result(KotlinBoolean(bool: success)) }
        }
    }
    func lockEnabled() -> Bool { preferences.bool(forKey: "finflow.lock") }
    func setLockEnabled(enabled: Bool) { preferences.set(enabled, forKey: "finflow.lock") }
    func reminderEnabled() -> Bool { preferences.bool(forKey: "finflow.reminder") }
    func budgetAlertsEnabled() -> Bool { preferences.bool(forKey: "finflow.budget.enabled") }
    func setBudgetAlerts(enabled: Bool, result: @escaping (String?) -> KotlinUnit) {
        if !enabled {
            preferences.set(false, forKey: "finflow.budget.enabled")
            _ = result(nil)
            return
        }
        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound]) { granted, error in
            DispatchQueue.main.async {
                if granted { self.preferences.set(true, forKey: "finflow.budget.enabled"); _ = result(nil) }
                else { _ = result(error?.localizedDescription ?? "Izin notifikasi ditolak; peringatan anggaran belum diaktifkan") }
            }
        }
    }
    func showBudgetAlert(userId: String, alertId: String, title: String, message: String) {
        let key = "finflow.budget.sent.\(userId).\(alertId)"
        guard budgetAlertsEnabled(), !preferences.bool(forKey: key), !pendingBudgetAlerts.contains(key) else { return }
        pendingBudgetAlerts.insert(key)
        let content = UNMutableNotificationContent()
        content.title = title
        content.body = message
        content.sound = .default
        let request = UNNotificationRequest(identifier: key, content: content, trigger: nil)
        let center = UNUserNotificationCenter.current()
        center.getNotificationSettings { settings in
            guard self.budgetAlertsEnabled(), settings.authorizationStatus == .authorized || settings.authorizationStatus == .provisional else {
                DispatchQueue.main.async { self.pendingBudgetAlerts.remove(key) }
                return
            }
            center.add(request) { error in
                DispatchQueue.main.async {
                    if error == nil { self.preferences.set(true, forKey: key) }
                    self.pendingBudgetAlerts.remove(key)
                }
            }
        }
    }
    func userNotificationCenter(_ center: UNUserNotificationCenter, willPresent notification: UNNotification,
                                withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void) {
        completionHandler([.banner, .sound])
    }
    func reminderHour() -> Int32 { preferences.object(forKey: "finflow.hour") == nil ? 20 : Int32(preferences.integer(forKey: "finflow.hour")) }
    func reminderMinute() -> Int32 { Int32(preferences.integer(forKey: "finflow.minute")) }
    func setReminder(enabled: Bool, hour: Int32, minute: Int32, result: @escaping (String?) -> KotlinUnit) {
        let center = UNUserNotificationCenter.current()
        if !enabled {
            center.removePendingNotificationRequests(withIdentifiers: ["finflow.daily"])
            preferences.set(false, forKey: "finflow.reminder")
            _ = result(nil); return
        }
        center.requestAuthorization(options: [.alert, .sound]) { granted, error in
            guard granted else {
                DispatchQueue.main.async { _ = result(error?.localizedDescription ?? "Izin notifikasi ditolak") }; return
            }
            let content = UNMutableNotificationContent()
            content.title = "Waktunya mencatat keuangan"
            content.body = "Catat pemasukan dan pengeluaran hari ini di Kasiva."
            content.sound = .default
            var date = DateComponents(); date.hour = Int(hour); date.minute = Int(minute)
            let trigger = UNCalendarNotificationTrigger(dateMatching: date, repeats: true)
            center.add(UNNotificationRequest(identifier: "finflow.daily", content: content, trigger: trigger)) { error in
                DispatchQueue.main.async {
                    if error == nil {
                        self.preferences.set(true, forKey: "finflow.reminder")
                        self.preferences.set(Int(hour), forKey: "finflow.hour")
                        self.preferences.set(Int(minute), forKey: "finflow.minute")
                    }
                    _ = result(error?.localizedDescription)
                }
            }
        }
    }
}
