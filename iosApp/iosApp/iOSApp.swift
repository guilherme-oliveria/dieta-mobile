import SwiftUI

@main
struct iOSApp: App {
    private let monitorConexao = MonitorConexao()
    @Environment(\.scenePhase) private var fase
    init() { _ = LeituraSegundoPlano.shared }
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
        .onChange(of: fase) { _, novaFase in
            if novaFase == .background { LeituraSegundoPlano.shared.agendar() }
        }
    }
}
