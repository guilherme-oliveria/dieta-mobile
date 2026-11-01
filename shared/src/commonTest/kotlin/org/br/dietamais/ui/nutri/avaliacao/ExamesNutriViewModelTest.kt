package org.br.dietamais.ui.nutri.avaliacao

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.br.dietamais.data.remoto.FalhaComunicacao
import org.br.dietamais.data.remoto.dto.*
import org.br.dietamais.data.repositorio.PortaAvaliacoes
import org.br.dietamais.data.repositorio.PortaExames
import org.br.dietamais.dominio.avaliacao.*
import kotlin.test.*
import kotlin.time.Instant
import kotlin.uuid.Uuid

@OptIn(ExperimentalCoroutinesApi::class)
class ExamesNutriViewModelTest {
    private val agora = Instant.parse("2026-11-01T12:00:00Z")
    private val catalogo = listOf(
        ExameCatalogoDto("glicemia-de-jejum", "Rotina", "Glicemia de jejum", "mg/dL"),
        ExameCatalogoDto("ferritina", "Rotina", "Ferritina", "ng/ml", condicoes = listOf("menarca", "homem")),
        ExameCatalogoDto("hiv", "Imunológicos", "Anti-HIV", null, qualitativo = true))
    private fun avaliacao(dias: Long = 5, faixa: FaixaEtariaAvaliacao? = FaixaEtariaAvaliacao.ADULTO, exames: List<ExameResultadoDto>? = null) =
        AvaliacaoResposta(Uuid.random(), Uuid.random(), Uuid.random(), agora - kotlin.time.Duration.parse("${dias * 24}h"), 80.0, 1.8, 24.69, ClassificacaoImc.NORMAL,
            versao = 3, criadoEm = agora - kotlin.time.Duration.parse("${dias * 24}h"), atualizadoEm = agora, faixaEtaria = faixa, exames = exames)

    private class Exames(val lista: List<ExameCatalogoDto>) : PortaExames {
        var faixas = mutableListOf<String>(); var falha: Exception? = null
        override suspend fun catalogo(faixaEtaria: String): List<ExameCatalogoDto> { faixas += faixaEtaria; falha?.let { throw it }; return lista }
    }
    private class Avaliacoes : PortaAvaliacoes {
        var salva: Triple<Uuid, Long, SalvarAvaliacaoRequisicao>? = null; var falha: Exception? = null
        override fun observar(pacienteId: Uuid?) = MutableStateFlow(emptyList<AvaliacaoResposta>())
        override suspend fun atualizar(pacienteId: Uuid) {}
        override suspend fun criar(pacienteId: Uuid, requisicao: SalvarAvaliacaoRequisicao): AvaliacaoResposta = error("não usado")
        override suspend fun salvar(id: Uuid, versao: Long, requisicao: SalvarAvaliacaoRequisicao): AvaliacaoResposta {
            falha?.let { throw it }; salva = Triple(id, versao, requisicao); error("resposta não usada")
        }
    }
    private fun TestScope.vm(e: Exames, a: Avaliacoes) = ExamesNutriViewModel(e, a, StandardTestDispatcher(testScheduler), agora = { agora })

    @Test fun carregaOCatalogoDaFaixaDaAvaliacaoEFiltraPorBuscaESecao() = runTest {
        val e = Exames(catalogo); val vm = vm(e, Avaliacoes()); vm.abrir(avaliacao(faixa = FaixaEtariaAvaliacao.IDOSO)); runCurrent()
        assertEquals(listOf("ADULTO"), e.faixas); assertEquals("Rotina", vm.estado.value.secao); assertEquals(listOf("Glicemia de jejum", "Ferritina"), vm.estado.value.visiveis.map { it.nome })
        vm.mudarBusca("GLICÊMIA".lowercase().replace("ê", "e")); assertEquals(listOf("Glicemia de jejum"), vm.estado.value.visiveis.map { it.nome })
        vm.mudarBusca(""); vm.escolherSecao("Imunológicos"); assertEquals(listOf("Anti-HIV"), vm.estado.value.visiveis.map { it.nome })
    }
    @Test fun semFaixaEtariaNaoCarregaEExplica() = runTest {
        val e = Exames(catalogo); val vm = vm(e, Avaliacoes()); vm.abrir(avaliacao(faixa = null)); runCurrent()
        assertTrue(e.faixas.isEmpty()); assertEquals(MensagemExames.SEM_NASCIMENTO, vm.estado.value.erro)
    }
    @Test fun salvarEnviaSoOPreenchidoComCondicaoEMostrarAoPaciente() = runTest {
        val a = Avaliacoes(); val av = avaliacao(); val vm = vm(Exames(catalogo), a); vm.abrir(av); runCurrent()
        vm.mudarValor("glicemia-de-jejum", "92,5"); vm.mudarValor("ferritina", "50"); vm.mudarCondicao("ferritina", "menarca"); vm.mudarValor("hiv", "não reagente")
        vm.mudarCondicao("hiv", "inventada"); vm.mudarMostrarAoPaciente(true); vm.salvar(); runCurrent()
        val (id, versao, req) = checkNotNull(a.salva)
        assertEquals(av.id, id); assertEquals(3L, versao)
        assertEquals(listOf(ExameLancadoDto("glicemia-de-jejum", valor = 92.5), ExameLancadoDto("ferritina", valor = 50.0, condicao = "menarca"), ExameLancadoDto("hiv", texto = "não reagente")), req.exames)
        assertEquals(true, req.compartilhamento?.exames); assertNull(req.justificativa)
    }
    @Test fun numeroInvalidoNaoEnviaEMarcaOCampo() = runTest {
        val a = Avaliacoes(); val vm = vm(Exames(catalogo), a); vm.abrir(avaliacao()); runCurrent()
        vm.mudarValor("glicemia-de-jejum", "abc"); vm.salvar(); runCurrent()
        assertNull(a.salva); assertEquals(setOf("glicemia-de-jejum"), vm.estado.value.invalidos); assertEquals(MensagemExames.NUMERO_INVALIDO, vm.estado.value.erro)
    }
    @Test fun depoisDe30DiasExigeJustificativa() = runTest {
        val a = Avaliacoes(); val vm = vm(Exames(catalogo), a); vm.abrir(avaliacao(dias = 40)); runCurrent()
        vm.mudarValor("glicemia-de-jejum", "90"); vm.salvar(); runCurrent()
        assertNull(a.salva); assertEquals(MensagemExames.JUSTIFICATIVA, vm.estado.value.erro)
        vm.mudarJustificativa("Resultado chegou depois"); vm.salvar(); runCurrent(); assertEquals("Resultado chegou depois", a.salva?.third?.justificativa)
    }
    @Test fun editaOsGravadosELimpandoTudoMandaListaVazia() = runTest {
        val a = Avaliacoes(); val gravado = ExameResultadoDto("glicemia-de-jejum", valor = 92.0, rotulo = "normal")
        val vm = vm(Exames(catalogo), a); vm.abrir(avaliacao(exames = listOf(gravado))); runCurrent()
        assertEquals("92", vm.estado.value.valores["glicemia-de-jejum"]); vm.mudarValor("glicemia-de-jejum", ""); vm.salvar(); runCurrent()
        assertEquals(emptyList(), a.salva?.third?.exames)
    }
    @Test fun semConexaoNoCatalogoViraMensagem() = runTest {
        val e = Exames(catalogo).also { it.falha = FalhaComunicacao() }; val vm = vm(e, Avaliacoes()); vm.abrir(avaliacao()); runCurrent()
        assertEquals(MensagemExames.SEM_CONEXAO, vm.estado.value.erro); assertTrue(vm.estado.value.semConexao); assertFalse(vm.estado.value.carregando)
    }
}
