import SwiftUI
import FirebaseCore
import GoogleSignIn
import FinFlowShared

@main
struct FinFlowApp: App {
    @Environment(\.scenePhase) private var scenePhase
    private let lifecycle = AppLifecycle()
    private let native = IOSNativeServices()

    init() { FirebaseApp.configure() }

    var body: some Scene {
        WindowGroup {
            FinFlowView(native: native, lifecycle: lifecycle)
                .ignoresSafeArea()
                .onOpenURL { url in GIDSignIn.sharedInstance.handle(url) }
                .onChange(of: scenePhase) { phase in
                    if phase == .background { lifecycle.background() }
                }
        }
    }
}

struct FinFlowView: UIViewControllerRepresentable {
    let native: IOSNativeServices
    let lifecycle: AppLifecycle
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController(nativeServices: native, lifecycle: lifecycle)
    }
    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
