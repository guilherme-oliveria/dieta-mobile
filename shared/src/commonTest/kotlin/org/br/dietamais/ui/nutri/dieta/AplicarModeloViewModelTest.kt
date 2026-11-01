package org.br.dietamais.ui.nutri.dieta

import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import kotlinx.datetime.LocalTime
import org.br.dietamais.data.remoto.*
import org.br.dietamais.data.remoto.dto.*
import org.br.dietamais.data.repositorio.*
import kotlin.test.*
import kotlin.time.Instant
import kotlin.uuid.Uuid

@OptIn(ExperimentalCoroutinesApi::class)
class AplicarModeloViewModelTest {
    private val agora = Instant.parse("2026-11-01T12:00:00Z")
    private val arroz = Uuid.random(); private val feijao = Uuid.random()
    private val paciente = Uuid.random()
    private fun item(g: Uuid, p: Double) = ItemRefeicaoRequisicao(grupoId = g, porcoes = p)
    private fun modelo(kcal: Double?) = ModeloDietaResposta(Uuid.random(), "SISTEMA", "Hipertensão", kcalReferencia = kcal, restricoes = listOf("ALTO_SODIO"), versao = 1, criadoEm = agora, atualizadoEm = agora,
        refeicoes = listOf(
            RefeicaoRequisicao(nome = "Almoço", horario = LocalTime(12, 0), ordem = 1, itens = listOf(item(arroz, 2.0), item(feijao, 1.0))),
            RefeicaoRequisicao(nome = "Jantar", horario = LocalTime(19, 0), ordem = 2, itens = listOf(item(arroz, 2.0))),
            RefeicaoRequisicao(nome = "Alt", horario = LocalTime(12, 0), ordem = 3, tipo = "ALTERNATIVA", itens = listOf(item(arroz, 9.0)))))
    private val referencia = PorcoesGruposResposta("t", grupos = listOf(GrupoPorcoesResposta(arroz, "Arroz", emptyList()), GrupoPorcoesResposta(feijao, "Feijão", emptyList())))
    private val dieta = DietaResposta(Uuid.random(), paciente, Uuid.random(), "Hipertensão", StatusDieta.RASCUNHO, null, emptyList(), TotaisNutricionaisResposta(0.0, 0.0, 0.0, 0.0), 1, agora, agora)

    private class Modelos(var vet: Double?) : PortaModelosDieta {
        val pedidos = mutableListOf<AplicarModeloRequisicao>(); val chaves = mutableListOf<Uuid>(); var avisos = emptyList<AvisoModelo>(); var falha: Exception? = null; var dieta: DietaResposta? = null
        override suspend fun listar() = emptyList<ModeloDietaResposta>()
        override suspend fun duplicar(id: Uuid, chave: Uuid): ModeloDietaResposta = error("não usado")
        override suspend fun excluir(id: Uuid, versao: Long) {}
        override suspend fun salvarComoModelo(dietaId: Uuid, requisicao: SalvarComoModeloRequisicao, chave: Uuid): ModeloDietaResposta = error("não usado")
        override suspend fun referenciaAvaliacao(pacienteId: Uuid) = vet?.let { ReferenciaAvaliacaoDietaResposta(metas = MetasDieta(it, 50.0, 20.0, 30.0)) }
        override suspend fun aplicar(pacienteId: Uuid, modeloId: Uuid, requisicao: AplicarModeloRequisicao, chave: Uuid): DietaDeModeloResposta {
            falha?.let { throw it }; pedidos += requisicao; chaves += chave; return DietaDeModeloResposta(dieta!!, 1.0, avisos)
        }
    }
    private fun TestScope.vm(m: Modelos) = AplicarModeloViewModel(m, object : PortaPorcoes { override suspend fun obter() = referencia }, StandardTestDispatcher(testScheduler))

    @Test fun mostraEscalaEPorcoesPorGrupoSemContarAlternativas() = runTest {
        val vm = vm(Modelos(2000.0)); vm.abrir(paciente, modelo(1600.0)); runCurrent()
        val e = vm.estado.value
        assertEquals(SituacaoEscala.CALCULADA, e.situacao); assertEquals(1.25, e.fator); assertEquals(2000.0, e.vetKcal)
        assertEquals(listOf(LinhaEscala("Arroz", 4.0, 5.0), LinhaEscala("Feijão", 1.0, 1.5)), e.linhas)
    }
    @Test fun semAvaliacaoOuForaDaFaixaMantemPorcoesDoModelo() = runTest {
        val m = Modelos(null); val vm = vm(m); vm.abrir(paciente, modelo(1600.0)); runCurrent()
        assertEquals(SituacaoEscala.SEM_AVALIACAO, vm.estado.value.situacao); assertEquals(4.0, vm.estado.value.linhas.first().paciente)
        m.vet = 9000.0; vm.abrir(paciente, modelo(1600.0)); runCurrent(); assertEquals(SituacaoEscala.FORA_DA_FAIXA, vm.estado.value.situacao)
        m.vet = 2000.0; vm.abrir(paciente, modelo(null)); runCurrent(); assertEquals(SituacaoEscala.SEM_BASE, vm.estado.value.situacao)
    }
    @Test fun criarEnviaOVetSoComEscalaViavelEMantemAChaveNoReenvio() = runTest {
        val m = Modelos(2000.0); m.dieta = dieta; val vm = vm(m); vm.abrir(paciente, modelo(1600.0)); runCurrent()
        vm.criar(); runCurrent(); assertEquals(dieta, vm.estado.value.criada); assertEquals(2000.0, m.pedidos.single().vetKcal)
        vm.criar(); runCurrent(); assertEquals(1, m.pedidos.size)
        m.vet = 9000.0; vm.abrir(paciente, modelo(1600.0)); runCurrent(); vm.criar(); runCurrent(); assertNull(m.pedidos.last().vetKcal)
        assertNotEquals(m.chaves[0], m.chaves[1])
    }
    @Test fun avisosDoServidorFicamNaTelaEFalhasViramMensagem() = runTest {
        val m = Modelos(2000.0); m.dieta = dieta; m.avisos = listOf(AvisoModelo("ITEM_REMOVIDO_RESTRICAO", "Pão saiu"))
        val vm = vm(m); vm.abrir(paciente, modelo(1600.0)); runCurrent(); vm.criar(); runCurrent()
        assertEquals(listOf("Pão saiu"), vm.estado.value.avisos.map { it.mensagem })
        vm.abrir(paciente, modelo(1600.0)); runCurrent(); m.falha = FalhaComunicacao(); vm.criar(); runCurrent()
        assertEquals(MensagemModelos.SEM_CONEXAO, vm.estado.value.erro); assertNull(vm.estado.value.criada); assertFalse(vm.estado.value.enviando)
    }
}
