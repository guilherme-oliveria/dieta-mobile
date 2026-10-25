import XCTest
import Shared

@MainActor
final class CofreSessaoTests: XCTestCase {
    func testKeychainPersisteEntreInstanciasELimpaSomenteSeuServico() async throws {
        let servico = "org.br.dietamais.testes.\(UUID().uuidString)"
        let primeiro = ArmazenamentoSeguro(nomeServico: servico)
        let outro = ArmazenamentoSeguro(nomeServico: servico + ".outro")
        do {
            let inicial = try await primeiro.ler()
            XCTAssertNil(inicial)
            try await primeiro.gravar(conteudo: "TOKEN_FICTICIO_TESTE_HOST_IOS")
            let reaberto = try await ArmazenamentoSeguro(nomeServico: servico).ler()
            XCTAssertEqual(reaberto, "TOKEN_FICTICIO_TESTE_HOST_IOS")
            let isolado = try await outro.ler()
            XCTAssertNil(isolado)
            try await outro.gravar(conteudo: "TOKEN_FICTICIO_OUTRO_SERVICO_IOS")
            try await primeiro.limpar()
            let removido = try await primeiro.ler()
            XCTAssertNil(removido)
            let preservado = try await outro.ler()
            XCTAssertEqual(preservado, "TOKEN_FICTICIO_OUTRO_SERVICO_IOS")
        } catch {
            try? await primeiro.limpar()
            try? await outro.limpar()
            throw error
        }
        try await outro.limpar()
    }
}
