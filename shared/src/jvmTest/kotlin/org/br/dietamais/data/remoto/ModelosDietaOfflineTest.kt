package org.br.dietamais.data.remoto

import io.ktor.client.engine.mock.*
import kotlinx.coroutines.test.runTest
import org.br.dietamais.data.remoto.dto.*
import org.br.dietamais.data.repositorio.ModelosDietaRepositorio
import org.br.dietamais.plataforma.Config
import kotlin.test.*
import kotlin.time.Instant
import kotlin.uuid.Uuid

/** MOB-56c: sem rede, a lista de modelos cai nos modelos da nutri que o sync já trouxe. */
class ModelosDietaOfflineTest {
    private val sessao = """{"accessToken":"ACCESS_FICTICIO","expiraEm":"2026-10-24T12:15:00Z","usuario":{"id":"019a0f32-5000-7000-8000-000000000001",
        "nome":"Nutri","email":"nutri@example.invalid","emailConfirmado":false,"perfis":["NUTRICIONISTA"],"perfil":"NUTRICIONISTA",
        "nutricionistaId":"019a0f32-5000-7000-8000-000000000002","pacienteId":null,"fuso":"America/Sao_Paulo"}}"""
    private val t = Instant.parse("2026-11-05T12:00:00Z")
    private fun modelo(titulo: String) = ModeloDietaResposta(Uuid.random(), "NUTRI", titulo, refeicoes = emptyList(), versao = 1, criadoEm = t, atualizadoEm = t)
    private fun api(engine: MockEngine) = ApiDieta(Config("https://app.example.invalid"), "1.0", Plataforma.WEB, CofreMemoria(sessao), ArmazemSessaoMemoria, engine)

    @Test fun semRedeMostraOsModelosLocais() = runTest {
        val locais = listOf(modelo("Low carb"), modelo("Hipertrofia"))
        api(MockEngine { throw java.io.IOException("Rede simulada indisponível") }).use { api ->
            api.restaurarSessao()
            val lista = ModelosDietaRepositorio(api, { }, { locais }).listar()
            assertEquals(locais, lista)
        }
    }

    @Test fun semRedeESemModeloLocalMantemAFalha() = runTest {
        api(MockEngine { throw java.io.IOException("Rede simulada indisponível") }).use { api ->
            api.restaurarSessao()
            assertFailsWith<FalhaComunicacao> { ModelosDietaRepositorio(api, { }, { emptyList() }).listar() }
        }
    }

    @Test fun syncComModelosDeDietaContaNoTotalEIgnoraCampoDesconhecido() {
        val id = Uuid.random()
        val json = """{"pacientes":[],"vinculos":[],"nutris":[],"produtos":[],"campoNovo":1,
            "modelosDieta":[{"id":"$id","versao":3,"excluido":false,"dados":${JsonContrato.encodeToString(modelo("Teste").copy(id = id))}}]}"""
        val m = JsonContrato.decodeFromString<MudancasFase1Resposta>(json)
        assertEquals(1, m.modelosDieta.size)
        assertEquals(1, m.total)
    }
}
