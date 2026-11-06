import Foundation
import FinFlowShared

final class IOSLocalFinancePersistence: NSObject, FinFlowShared.LocalFinancePersistence {
    static let shared = IOSLocalFinancePersistence()
    let id = "finflow-local-v1"
    private let lock = NSLock()
    private let maxBytes = 20 * 1024 * 1024
    private let manager = FileManager.default

    private override init() { super.init() }

    func load() -> LocalFinanceLoad {
        lock.lock()
        defer { lock.unlock() }
        do {
            let file = try fileURL()
            let attributes: [FileAttributeKey: Any]
            do { attributes = try manager.attributesOfItem(atPath: file.path) }
            catch let error as NSError {
                if error.domain == NSCocoaErrorDomain &&
                    (error.code == NSFileReadNoSuchFileError || error.code == NSFileNoSuchFileError) {
                    return LocalFinanceLoad(content: nil, error: nil)
                }
                throw error
            }
            guard attributes[.type] as? FileAttributeType == .typeRegular,
                  let size = attributes[.size] as? NSNumber, size.int64Value > 0, size.int64Value <= Int64(maxBytes) else {
                return LocalFinanceLoad(content: nil, error: "Ukuran atau format data lokal tidak valid. Data lama dipertahankan.")
            }
            let data = try Data(contentsOf: file, options: .mappedIfSafe)
            guard data.count <= maxBytes, let content = String(data: data, encoding: .utf8),
                  (try JSONSerialization.jsonObject(with: data)) is [String: Any] else {
                return LocalFinanceLoad(content: nil, error: "Data lokal rusak. Data lama dipertahankan; jangan hapus data aplikasi.")
            }
            return LocalFinanceLoad(content: content, error: nil)
        } catch {
            return LocalFinanceLoad(content: nil, error: "Data lokal tidak dapat dibuka. Buka kunci perangkat dan coba lagi; jangan hapus data aplikasi.")
        }
    }

    func save(content: String) -> String? {
        if UserDefaults.standard.string(forKey: "finflow.local.sync.frozen") == "true" {
            return "Pencatatan lokal dijeda selama sinkronisasi. Lanjutkan melalui Manajemen Akun."
        }
        lock.lock()
        defer { lock.unlock() }
        do {
            guard content.utf8.count <= maxBytes, let data = content.data(using: .utf8), !data.isEmpty,
                  (try JSONSerialization.jsonObject(with: data)) is [String: Any] else {
                return "Format data lokal tidak valid atau melebihi 20 MB. Data lama tidak ditimpa."
            }
            let file = try fileURL()
            try data.write(to: file, options: [.atomic, .completeFileProtectionUntilFirstUserAuthentication])
            return nil
        } catch {
            return "Data lokal gagal disimpan. Pastikan penyimpanan perangkat tersedia, lalu coba lagi."
        }
    }

    private func fileURL() throws -> URL {
        let support = try manager.url(for: .applicationSupportDirectory, in: .userDomainMask, appropriateFor: nil, create: true)
        var directory = support.appendingPathComponent("FinFlowLocal", isDirectory: true)
        try manager.createDirectory(at: directory, withIntermediateDirectories: true,
            attributes: [.protectionKey: FileProtectionType.completeUntilFirstUserAuthentication])
        try manager.setAttributes([.protectionKey: FileProtectionType.completeUntilFirstUserAuthentication], ofItemAtPath: directory.path)
        var resources = URLResourceValues()
        resources.isExcludedFromBackup = true
        try directory.setResourceValues(resources)
        return directory.appendingPathComponent("finance-v1.json", isDirectory: false)
    }
}
