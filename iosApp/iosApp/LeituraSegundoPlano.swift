import BackgroundTasks
import Foundation
import Shared

final class LeituraSegundoPlano {
    static let shared = LeituraSegundoPlano()
    private let identificador = "org.br.dietamais.sync-leitura"
    private init() {
        BGTaskScheduler.shared.register(forTaskWithIdentifier: identificador, using: nil) { [weak self] tarefa in
            guard let self, let leitura = tarefa as? BGAppRefreshTask else {
                tarefa.setTaskCompleted(success: false)
                return
            }
            self.executar(leitura)
        }
    }
    func agendar() {
        let pedido = BGAppRefreshTaskRequest(identifier: identificador)
        pedido.earliestBeginDate = Date(timeIntervalSinceNow: 15 * 60)
        // O sistema pode recusar/adiar; abertura e retorno continuam sendo gatilhos.
        try? BGTaskScheduler.shared.submit(pedido)
    }
    private func executar(_ tarefa: BGAppRefreshTask) {
        agendar()
        var finalizada = false
        let concluir: (Bool) -> Void = { sucesso in
            DispatchQueue.main.async {
                guard !finalizada else { return }
                finalizada = true
                tarefa.setTaskCompleted(success: sucesso)
            }
        }
        let cancelamento = LeituraIosKt.executarLeituraIos { resultado in
            concluir(resultado == "CONCLUIDO" || resultado == "SEM_SESSAO")
        }
        tarefa.expirationHandler = {
            cancelamento.cancelar()
            concluir(false)
        }
    }
}
