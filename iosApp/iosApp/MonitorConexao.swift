import Foundation
import Network
import Shared

final class MonitorConexao {
    private let monitor = NWPathMonitor()
    init() {
        monitor.pathUpdateHandler = { caminho in
            ConexaoIosKt.atualizarConectividadeIos(disponivel: caminho.status == .satisfied)
        }
        monitor.start(queue: DispatchQueue(label: "org.br.dietamais.conectividade"))
    }
    deinit { monitor.cancel() }
}
