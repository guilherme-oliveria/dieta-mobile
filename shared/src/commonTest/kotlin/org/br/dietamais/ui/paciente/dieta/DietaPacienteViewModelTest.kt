package org.br.dietamais.ui.paciente.dieta

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.viewmodel.initializer
import kotlinx.coroutines.test.*
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import org.br.dietamais.data.remoto.ErroApi
import org.br.dietamais.data.remoto.dto.*
import org.br.dietamais.data.repositorio.*
import kotlin.test.*
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.Uuid

@OptIn(ExperimentalCoroutinesApi::class)
class DietaPacienteViewModelTest {
    private val zero = TotaisNutricionaisResposta(0.0, 0.0, 0.0, 0.0)
    private val grupo = Uuid.random()
    private val arroz = AlimentoResposta(Uuid.random(), null, grupo, "Arroz", "4 colheres", 25.0, true, 0, Instant.parse("2026-01-01T00:00:00Z"))
    private val macarrao = AlimentoResposta(Uuid.random(), null, grupo, "Macarrão", "1 prato raso", 40.0, true, 0, Instant.parse("2026-01-01T00:00:00Z"))
    private val inativo = macarrao.copy(id = Uuid.random(), nome = "Cuscuz", ativo = false)
    private val relogio = object : Clock { override fun now() = Instant.parse("2026-10-28T09:00:00Z") }

    private fun snap(nome: String, publicado: String): DietaSnapshotResposta {
        val item = ItemRefeicaoResposta(Uuid.random(), arroz.id, grupo, "Arroz", 2.0, "4 colheres", 50.0, null, zero)
        return DietaSnapshotResposta(Uuid.random(), Uuid.random(), Uuid.random(), Uuid.random(),
            NutricionistaResumoResposta(Uuid.random(), nome, "CRN"), "Dieta $nome", null,
            listOf(RefeicaoResposta(Uuid.random(), "Café", LocalTime(8, 0), 0, listOf(item), zero), RefeicaoResposta(Uuid.random(), "Almoço", LocalTime(12, 0), 1, listOf(item.copy(id = Uuid.random())), zero)),
            ListaSubstituicaoResposta(Uuid.random(), 0, listOf(arroz, macarrao, inativo), emptyList(), emptyList()), zero, Instant.parse(publicado))
    }

    private class Porta(val dados: MutableStateFlow<DadosDietaPaciente>) : PortaDietaPaciente {
        var falha: Exception? = null; var chamadas = 0
        override fun observar() = dados
        override suspend fun selecionar(dietaId: Uuid, versao: Long): DietaEmUsoResposta { chamadas++; falha?.let { throw it }; return DietaEmUsoResposta(dietaId, versao + 1) }
    }

    private class Rotina : PortaRotina {
        var falha = false
        val recusas = MutableSharedFlow<Unit>(extraBufferCapacity = 4)
        override val falhas: Flow<Unit> = recusas
        val dados = MutableStateFlow(DadosRotina())
        val checkins = mutableListOf<CheckinResposta>()
        override fun observar() = dados
        override fun registrarCheckin(dietaId: Uuid, refeicaoId: Uuid, data: LocalDate, status: StatusCheckin, trocas: List<TrocaCheckin>, agora: Instant) {
            if (falha) throw IllegalStateException("falha simulada")
            val c = CheckinResposta(Uuid.random(), Uuid.random(), dietaId, refeicaoId, data, status, trocas, agora)
            checkins += c; dados.value = dados.value.copy(checkins = dados.value.checkins + c)
        }
        override fun registrarAgua(ml: Int, agora: Instant) { dados.value = dados.value.copy(agua = dados.value.agua + AguaResposta(Uuid.random(), Uuid.random(), ml, agora)) }
        override fun registrarPeso(pesoKg: Double, agora: Instant) { dados.value = dados.value.copy(pesos = dados.value.pesos + PesoResposta(Uuid.random(), Uuid.random(), pesoKg, agora)) }
    }

    private fun TestScope.vm(porta: Porta, rotina: Rotina = Rotina(), conectividade: Flow<Boolean>? = null, gravou: () -> Unit = {}) =
        DietaPacienteViewModel(porta, rotina, relogio, TimeZone.UTC, StandardTestDispatcher(testScheduler), conectividade, flowOf(Unit), gravou)

    @Test fun semDietaMostraEstadoVazio() = runTest {
        val vm = vm(Porta(MutableStateFlow(DadosDietaPaciente()))); advanceUntilIdle()
        assertTrue(vm.estado.value.carregado); assertNull(vm.estado.value.dieta)
    }

    @Test fun listaFuncionaOfflineEReiniciaFiltrosQuandoOSnapshotMuda() = runTest {
        val s = snap("Ana", "2026-01-01T10:00:00Z")
        val porta = Porta(MutableStateFlow(DadosDietaPaciente(listOf(s))))
        val vm = vm(porta, conectividade = flowOf(false)); advanceUntilIdle()
        vm.abrirLista(); vm.buscarNaLista("macarrao"); vm.filtrarGrupoLista(grupo)
        vm.expandirGrupoLista(grupo); assertNull(vm.estado.value.grupoListaAberto); vm.expandirGrupoLista(grupo)
        assertTrue(vm.estado.value.listaAberta)
        assertEquals(listOf(macarrao), vm.estado.value.gruposListaVisiveis.single().alimentos)
        assertEquals(grupo, vm.estado.value.grupoListaAberto)
        assertEquals(0, porta.chamadas)
        porta.dados.value = DadosDietaPaciente(listOf(s.copy(id = Uuid.random(), listaSubstituicao = s.listaSubstituicao.copy(alimentos = emptyList()))))
        advanceUntilIdle()
        assertEquals("", vm.estado.value.buscaLista)
        assertNull(vm.estado.value.grupoListaFiltro)
        assertNull(vm.estado.value.grupoListaAberto)
        assertTrue(vm.estado.value.gruposListaVisiveis.isEmpty())
        vm.fecharLista(); assertFalse(vm.estado.value.listaAberta)
    }

    @Test fun botaoDoItemAbreSomenteATrocaDaqueleItem() = runTest {
        val s = snap("Ana", "2026-01-01T10:00:00Z")
        val rotina = Rotina(); val vm = vm(Porta(MutableStateFlow(DadosDietaPaciente(listOf(s)))), rotina)
        advanceUntilIdle()
        val r = s.refeicoes.first(); val item = r.itens.single()
        vm.abrirTroca(r.id, Uuid.random()); assertNull(vm.estado.value.troca)
        vm.abrirTroca(r.id, item.id); assertEquals(item.id, vm.estado.value.troca?.itemId)
        vm.escolherSubstituto(item.id, macarrao.id); vm.confirmarTroca(); advanceUntilIdle()
        assertEquals(listOf(TrocaCheckin(item.id, macarrao.id)), rotina.checkins.single().trocas)
    }

    @Test fun falhaAoConfirmarTrocaMantemEscolhasParaTentarNovamente() = runTest {
        val s = snap("Ana", "2026-01-01T10:00:00Z"); val rotina = Rotina().apply { falha = true }
        val vm = vm(Porta(MutableStateFlow(DadosDietaPaciente(listOf(s)))), rotina); advanceUntilIdle()
        val r = s.refeicoes.first(); val item = r.itens.single()
        vm.abrirRefeicao(r.id); vm.abrirTroca(r.id, item.id); vm.escolherSubstituto(item.id, macarrao.id); vm.confirmarTroca()
        assertEquals(MensagemDietaPaciente.ERRO, vm.estado.value.erro)
        assertEquals(macarrao.id, vm.estado.value.troca?.escolhas?.get(item.id)); assertTrue(rotina.checkins.isEmpty())
        rotina.falha = false; vm.confirmarTroca(); advanceUntilIdle()
        assertNull(vm.estado.value.erro); assertNull(vm.estado.value.troca)
        assertEquals(StatusCheckin.TROCADO, vm.estado.value.checkins[r.id])
    }

    @Test fun destacaAProximaERespeitaAEscolha() = runTest {
        val a = snap("Ana", "2026-01-01T10:00:00Z"); val b = snap("Bia", "2026-02-01T10:00:00Z")
        val vm = vm(Porta(MutableStateFlow(DadosDietaPaciente(listOf(a, b))))); advanceUntilIdle()
        assertEquals(b.dietaId, vm.estado.value.dieta?.dietaId); assertEquals("Almoço", vm.estado.value.proxima?.nome); assertEquals(2, vm.estado.value.alternativas.size)
        vm.escolher(a.dietaId); advanceUntilIdle()
        assertEquals(a.dietaId, vm.estado.value.dieta?.dietaId); assertEquals(1, vm.estado.value.versaoSelecao)
    }

    @Test fun escolherSemRedeNaoChamaAApi() = runTest {
        val a = snap("Ana", "2026-01-01T10:00:00Z"); val b = snap("Bia", "2026-02-01T10:00:00Z")
        val porta = Porta(MutableStateFlow(DadosDietaPaciente(listOf(a, b))))
        val vm = vm(porta, conectividade = flowOf(false)); advanceUntilIdle()
        vm.escolher(a.dietaId); advanceUntilIdle()
        assertEquals(0, porta.chamadas); assertEquals(MensagemDietaPaciente.SEM_CONEXAO, vm.estado.value.erro)
    }

    @Test fun conflitoDeVersaoViraMensagem() = runTest {
        val a = snap("Ana", "2026-01-01T10:00:00Z"); val b = snap("Bia", "2026-02-01T10:00:00Z")
        val porta = Porta(MutableStateFlow(DadosDietaPaciente(listOf(a, b)))).apply { falha = ErroApi("CONFLITO_VERSAO", null, emptyList(), 409, null, null) }
        val vm = vm(porta); advanceUntilIdle()
        vm.escolher(a.dietaId); advanceUntilIdle()
        assertEquals(MensagemDietaPaciente.CONFLITO, vm.estado.value.erro); assertFalse(vm.estado.value.enviando)
    }

    @Test fun checkinMarcaARefeicaoEAvancaAProxima() = runTest {
        val s = snap("Ana", "2026-01-01T10:00:00Z"); var gravacoes = 0
        val rotina = Rotina(); val vm = vm(Porta(MutableStateFlow(DadosDietaPaciente(listOf(s)))), rotina) { gravacoes++ }; advanceUntilIdle()
        val almoco = vm.estado.value.proxima!!
        vm.marcar(almoco.id, StatusCheckin.FEITO); advanceUntilIdle()
        assertEquals(StatusCheckin.FEITO, vm.estado.value.checkins[almoco.id]); assertNull(vm.estado.value.proxima); assertEquals(1, gravacoes)
        assertEquals(LocalDate(2026, 10, 28), rotina.checkins.single().data)
        vm.marcar(almoco.id, StatusCheckin.TROCADO); assertEquals(1, rotina.checkins.size) // TROCADO só pelo fluxo de troca
    }

    @Test fun desfazerDentroDoPrazoNaoGravaOCheckin() = runTest {
        val s = snap("Ana", "2026-01-01T10:00:00Z"); val rotina = Rotina()
        val vm = vm(Porta(MutableStateFlow(DadosDietaPaciente(listOf(s)))), rotina); advanceUntilIdle()
        val cafe = vm.estado.value.proxima!!
        vm.marcar(cafe.id, StatusCheckin.PULADO); runCurrent()
        assertEquals(StatusCheckin.PULADO, vm.estado.value.checkins[cafe.id]); assertTrue(vm.estado.value.podeDesfazer)
        assertTrue(rotina.checkins.isEmpty())
        vm.desfazerCheckin(); advanceUntilIdle()
        assertNull(vm.estado.value.checkins[cafe.id]); assertFalse(vm.estado.value.podeDesfazer)
        assertEquals(cafe.id, vm.estado.value.proxima?.id); assertTrue(rotina.checkins.isEmpty())
    }

    @Test fun semDesfazerOCheckinEGravadoAposOPrazo() = runTest {
        val s = snap("Ana", "2026-01-01T10:00:00Z"); val rotina = Rotina()
        val vm = vm(Porta(MutableStateFlow(DadosDietaPaciente(listOf(s)))), rotina); advanceUntilIdle()
        val cafe = vm.estado.value.proxima!!
        vm.marcar(cafe.id, StatusCheckin.FEITO); advanceTimeBy(PRAZO_DESFAZER_MS - 1); runCurrent()
        assertTrue(rotina.checkins.isEmpty()); assertTrue(vm.estado.value.podeDesfazer)
        advanceTimeBy(2); runCurrent()
        assertEquals(StatusCheckin.FEITO, rotina.checkins.single().status); assertFalse(vm.estado.value.podeDesfazer)
    }

    @Test fun novoRegistroGravaOAnteriorEDesfazerSoAfetaOUltimo() = runTest {
        val s = snap("Ana", "2026-01-01T10:00:00Z"); val rotina = Rotina()
        val vm = vm(Porta(MutableStateFlow(DadosDietaPaciente(listOf(s)))), rotina); advanceUntilIdle()
        val a = s.refeicoes[0]; val b = s.refeicoes[1]
        vm.marcar(a.id, StatusCheckin.FEITO); runCurrent()
        vm.marcar(b.id, StatusCheckin.PULADO); runCurrent()
        assertEquals(a.id, rotina.checkins.single().refeicaoId)
        vm.desfazerCheckin(); advanceUntilIdle()
        assertEquals(1, rotina.checkins.size); assertNull(vm.estado.value.checkins[b.id]); assertEquals(StatusCheckin.FEITO, vm.estado.value.checkins[a.id])
    }

    @Test fun falhaAoGravarDepoisDoPrazoTiraOAvisoVoltaARefeicaoEMostraOErro() = runTest {
        val s = snap("Ana", "2026-01-01T10:00:00Z"); val rotina = Rotina().apply { falha = true }
        val vm = vm(Porta(MutableStateFlow(DadosDietaPaciente(listOf(s)))), rotina); advanceUntilIdle()
        val cafe = vm.estado.value.proxima!!
        vm.marcar(cafe.id, StatusCheckin.FEITO); runCurrent()
        assertTrue(vm.estado.value.podeDesfazer); assertNull(vm.estado.value.erro)
        advanceTimeBy(PRAZO_DESFAZER_MS + 1); runCurrent()
        assertFalse(vm.estado.value.podeDesfazer); assertNull(vm.estado.value.checkins[cafe.id])
        assertEquals(cafe.id, vm.estado.value.proxima?.id); assertEquals(MensagemDietaPaciente.ERRO, vm.estado.value.erro)
        rotina.falha = false; vm.marcar(cafe.id, StatusCheckin.FEITO); advanceUntilIdle()
        assertEquals(StatusCheckin.FEITO, rotina.checkins.single().status); assertNull(vm.estado.value.erro)
    }

    @Test fun fecharATelaGravaOCheckinPendente() = runTest {
        val s = snap("Ana", "2026-01-01T10:00:00Z"); val rotina = Rotina()
        val vm = vm(Porta(MutableStateFlow(DadosDietaPaciente(listOf(s)))), rotina); advanceUntilIdle()
        val cafe = vm.estado.value.proxima!!
        vm.marcar(cafe.id, StatusCheckin.FEITO); runCurrent()
        assertTrue(rotina.checkins.isEmpty())
        val loja = ViewModelStore()
        ViewModelProvider.create(loja, viewModelFactory { initializer { vm } })[DietaPacienteViewModel::class]
        loja.clear() // o mesmo caminho de quando a aba/tela sai: onCleared com o viewModelScope já cancelado
        assertEquals(StatusCheckin.FEITO, rotina.checkins.single().status)
    }

    @Test fun desfazerAguaTiraSoOUltimoLancamentoAindaNaoEnviado() = runTest {
        val s = snap("Ana", "2026-01-01T10:00:00Z"); val rotina = Rotina()
        val vm = vm(Porta(MutableStateFlow(DadosDietaPaciente(listOf(s)))), rotina); advanceUntilIdle()
        assertFalse(vm.estado.value.podeDesfazerAgua)
        vm.beberAgua(200); vm.beberAgua(250); runCurrent()
        assertEquals(450, vm.estado.value.aguaMl); assertTrue(vm.estado.value.podeDesfazerAgua)
        vm.desfazerAgua(); runCurrent()
        assertEquals(200, vm.estado.value.aguaMl); assertTrue(vm.estado.value.podeDesfazerAgua)
        advanceUntilIdle()
        assertEquals(200, vm.estado.value.aguaMl); assertFalse(vm.estado.value.podeDesfazerAgua)
        assertEquals(listOf(200), rotina.dados.value.agua.map { it.ml })
        vm.desfazerAgua(); advanceUntilIdle() // nada pendente: não faz nada
        assertEquals(200, vm.estado.value.aguaMl)
    }

    @Test fun fecharATelaGravaAAguaPendente() = runTest {
        val s = snap("Ana", "2026-01-01T10:00:00Z"); val rotina = Rotina()
        val vm = vm(Porta(MutableStateFlow(DadosDietaPaciente(listOf(s)))), rotina); advanceUntilIdle()
        vm.beberAgua(300); runCurrent()
        assertTrue(rotina.dados.value.agua.isEmpty())
        val loja = ViewModelStore()
        ViewModelProvider.create(loja, viewModelFactory { initializer { vm } })[DietaPacienteViewModel::class]
        loja.clear()
        assertEquals(listOf(300), rotina.dados.value.agua.map { it.ml })
    }

    @Test fun recusaDoServidorMostraErroNoHoje() = runTest {
        val rotina = Rotina(); val vm = vm(Porta(MutableStateFlow(DadosDietaPaciente(listOf(snap("Ana", "2026-01-01T10:00:00Z"))))), rotina); advanceUntilIdle()
        assertNull(vm.estado.value.erro)
        rotina.recusas.tryEmit(Unit); advanceUntilIdle()
        assertEquals(MensagemDietaPaciente.ERRO, vm.estado.value.erro)
    }

    @Test fun ultimoCheckinDaRefeicaoVale() = runTest {
        val s = snap("Ana", "2026-01-01T10:00:00Z"); val rotina = Rotina()
        val vm = vm(Porta(MutableStateFlow(DadosDietaPaciente(listOf(s)))), rotina); advanceUntilIdle()
        val cafe = s.refeicoes.first { it.nome == "Café" }
        rotina.registrarCheckin(s.dietaId, cafe.id, LocalDate(2026, 10, 28), StatusCheckin.PULADO, emptyList(), Instant.parse("2026-10-28T08:00:00Z"))
        rotina.registrarCheckin(s.dietaId, cafe.id, LocalDate(2026, 10, 28), StatusCheckin.FEITO, emptyList(), Instant.parse("2026-10-28T08:30:00Z"))
        advanceUntilIdle()
        assertEquals(StatusCheckin.FEITO, vm.estado.value.checkins[cafe.id])
    }

    @Test fun trocaSoAceitaSubstitutoDoMesmoGrupoNaLista() = runTest {
        val s = snap("Ana", "2026-01-01T10:00:00Z"); val rotina = Rotina()
        val vm = vm(Porta(MutableStateFlow(DadosDietaPaciente(listOf(s)))), rotina); advanceUntilIdle()
        val cafe = s.refeicoes.first { it.nome == "Café" }; val item = cafe.itens.single()
        vm.abrirTroca(cafe.id); vm.escolherSubstituto(item.id, inativo.id); vm.confirmarTroca()
        assertTrue(rotina.checkins.isEmpty()) // inativo não é opção
        vm.escolherSubstituto(item.id, macarrao.id); vm.confirmarTroca(); advanceUntilIdle()
        val c = rotina.checkins.single()
        assertEquals(StatusCheckin.TROCADO, c.status); assertEquals(listOf(TrocaCheckin(item.id, macarrao.id)), c.trocas); assertNull(vm.estado.value.troca)
        assertEquals(StatusCheckin.TROCADO, vm.estado.value.checkins[cafe.id])
    }

    @Test fun aguaSomaODiaEUsaOPesoMaisRecenteNaMeta() = runTest {
        val rotina = Rotina(); val s = snap("Ana", "2026-01-01T10:00:00Z")
        val vm = vm(Porta(MutableStateFlow(DadosDietaPaciente(listOf(s)))), rotina); advanceUntilIdle()
        assertEquals(2000, vm.estado.value.metaAguaMl)
        vm.beberAgua(300); vm.beberAgua(200); rotina.registrarPeso(70.0, relogio.now()); advanceUntilIdle()
        assertEquals(500, vm.estado.value.aguaMl); assertEquals(2450, vm.estado.value.metaAguaMl)
        rotina.registrarAgua(250, Instant.parse("2026-10-27T10:00:00Z")); advanceUntilIdle()
        assertEquals(500, vm.estado.value.aguaMl)
        vm.mudarDia(LocalDate(2026, 10, 27)); advanceUntilIdle()
        assertEquals(250, vm.estado.value.aguaMl); assertNull(vm.estado.value.proxima)
        vm.mudarDia(LocalDate(2026, 10, 20)); advanceUntilIdle()
        assertEquals(LocalDate(2026, 10, 28), vm.estado.value.dia) // fora da janela de 3 dias volta para hoje
    }
}
