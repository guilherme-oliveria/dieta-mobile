package org.br.dietamais.ui.nutri.listapaciente

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.br.dietamais.data.remoto.*
import org.br.dietamais.data.remoto.dto.*
import org.br.dietamais.data.repositorio.*
import kotlin.test.*
import kotlin.time.Instant
import kotlin.uuid.Uuid

@OptIn(ExperimentalCoroutinesApi::class)
class ListaPacienteViewModelTest {
    private val grupo = GrupoAlimentoResposta(Uuid.parse("019a0f32-5000-7000-8000-000000000001"), "Grupo fictício", 150.0, 25.0, 5.0, 3.0)
    private val paciente = Uuid.parse("019a0f32-5000-7000-8000-0000000000aa")
    private fun alimento(n: Int) = AlimentoResposta(Uuid.parse("019a0f32-5000-7000-8000-0000000001${n.toString().padStart(2, '0')}"), null, grupo.id, "Alimento $n",
        "1 unidade", 50.0, true, 1, Instant.parse("2026-10-26T12:00:00Z"))
    private val a1 = alimento(1); private val a2 = alimento(2)

    private class Lista(var servidor: ListaSubstituicaoResposta, val extras: List<AlimentoResposta> = emptyList()) : PortaListaPaciente {
        val salvos = mutableListOf<AtualizarListaSubstituicaoRequisicao>(); var falha: Exception? = null
        override suspend fun obter(pacienteId: Uuid) = servidor
        override suspend fun salvar(pacienteId: Uuid, versao: Long, requisicao: AtualizarListaSubstituicaoRequisicao): ListaSubstituicaoResposta {
            falha?.let { throw it }
            salvos += requisicao
            servidor = servidor.copy(versao = versao + 1, removidos = requisicao.removidos, adicionados = requisicao.adicionados,
                alimentos = servidor.alimentos.filter { it.id !in requisicao.removidos } + extras.filter { it.id in requisicao.adicionados })
            return servidor
        }
        val copias = mutableListOf<Triple<Uuid, Uuid, Long>>(); var origemLista: ListaSubstituicaoResposta? = null
        override suspend fun copiarDe(pacienteId: Uuid, origemId: Uuid, versao: Long): ListaSubstituicaoResposta {
            falha?.let { throw it }
            copias += Triple(pacienteId, origemId, versao)
            servidor = servidor.copy(versao = versao + 1, alimentos = origemLista?.alimentos ?: emptyList(), adicionados = origemLista?.adicionados ?: emptyList(), removidos = origemLista?.removidos ?: emptyList())
            return servidor
        }
    }
    private class Pacientes(val todos: List<PacienteResposta>) : PortaPacientes {
        override fun observar() = flowOf(todos)
        override suspend fun atualizar() {}
        override suspend fun criar(requisicao: CriarPacienteRequisicao, chave: Uuid) = error("não usado")
        override suspend fun convidar(id: Uuid, chave: Uuid) = error("não usado")
        override suspend fun assinatura() = null
    }
    private class Catalogo(val a: List<AlimentoResposta>, val g: GrupoAlimentoResposta) : PortaAlimentos {
        override fun observar() = flowOf(CatalogoAlimentos(listOf(g), a))
        override suspend fun atualizar() {}
        override suspend fun criar(requisicao: CriarAlimentoRequisicao, chave: Uuid) = error("não usado")
        override suspend fun alterar(id: Uuid, versao: Long, requisicao: AtualizarAlimentoRequisicao) = error("não usado")
    }
    private fun vm(lista: Lista, escopo: TestScope) = ListaPacienteViewModel(lista, Catalogo(listOf(a1, a2), grupo), StandardTestDispatcher(escopo.testScheduler))

    @Test fun removerEAdicionarSoSalvamAoConfirmarEEnviamDiferencaCompleta() = runTest {
        val lista = Lista(ListaSubstituicaoResposta(paciente, 3, listOf(a1), emptyList(), emptyList()), listOf(a2))
        val vm = vm(lista, this); vm.abrir(paciente); runCurrent()
        assertEquals(listOf(a1.id), vm.estado.value.efetiva.map { it.id }); assertEquals(listOf(a2.id), vm.estado.value.opcoes.map { it.id })
        vm.remover(a1.id); vm.adicionar(a2.id); runCurrent()
        assertTrue(vm.estado.value.pendente); assertEquals(listOf(a2.id), vm.estado.value.efetiva.map { it.id }); assertTrue(lista.salvos.isEmpty())
        vm.salvar(); runCurrent()
        assertEquals(listOf(a1.id), lista.salvos.single().removidos); assertEquals(listOf(a2.id), lista.salvos.single().adicionados)
        assertFalse(vm.estado.value.pendente); assertEquals(4, vm.estado.value.versao); assertEquals(MensagemLista.SALVO, vm.estado.value.aviso)
    }
    @Test fun desfazerAjusteLimpaPendencia() = runTest {
        val vm = vm(Lista(ListaSubstituicaoResposta(paciente, 1, listOf(a1), emptyList(), emptyList())), this); vm.abrir(paciente); runCurrent()
        vm.remover(a1.id); vm.adicionar(a1.id); assertFalse(vm.estado.value.pendente)
    }
    @Test fun conflitoMostraMensagemSemPerderTelaEPermiteRecarregar() = runTest {
        val lista = Lista(ListaSubstituicaoResposta(paciente, 1, listOf(a1), emptyList(), emptyList()))
        val vm = vm(lista, this); vm.abrir(paciente); runCurrent()
        lista.falha = ErroApi("CONFLITO_VERSAO", "RN-SYN-02", emptyList(), 409, null, null)
        vm.remover(a1.id); vm.salvar(); runCurrent()
        assertEquals(MensagemLista.CONFLITO, vm.estado.value.erro)
        lista.falha = null; vm.recarregar(); runCurrent()
        assertNull(vm.estado.value.erro); assertFalse(vm.estado.value.pendente)
    }

    @Test fun copiaAListaDeOutroPacienteSoDepoisDeConfirmarEUsaAVersaoDoServidor() = runTest {
        val agora = Instant.parse("2026-10-26T12:00:00Z")
        val outra = PacienteResposta(Uuid.random(), Uuid.random(), "Bia", status = StatusVinculo.ATIVO, versao = 1, criadoEm = agora, atualizadoEm = agora)
        val propria = PacienteResposta(paciente, Uuid.random(), "Ana", status = StatusVinculo.ATIVO, versao = 1, criadoEm = agora, atualizadoEm = agora)
        val l = Lista(ListaSubstituicaoResposta(paciente, 4, listOf(a1), emptyList(), emptyList())); l.origemLista = ListaSubstituicaoResposta(outra.id, 2, listOf(a2), listOf(a2.id), listOf(a1.id))
        val vm = ListaPacienteViewModel(l, Catalogo(listOf(a1, a2), grupo), StandardTestDispatcher(testScheduler), pacientes = Pacientes(listOf(propria, outra)))
        vm.abrir(paciente); advanceUntilIdle()
        assertEquals(listOf(outra.id), vm.estado.value.origens.map { it.id }) // o próprio paciente não é origem
        vm.remover(a1.id) // edição não salva: a cópia a descarta
        vm.pedirCopia(outra.id); assertEquals(outra.id, vm.estado.value.copiandoDe); assertTrue(l.copias.isEmpty())
        vm.desistirDaCopia(); assertNull(vm.estado.value.copiandoDe); assertTrue(l.copias.isEmpty())
        vm.pedirCopia(outra.id); vm.confirmarCopia(); advanceUntilIdle()
        assertEquals(listOf(Triple(paciente, outra.id, 4L)), l.copias)
        val e = vm.estado.value
        assertEquals(MensagemLista.COPIADA, e.aviso); assertEquals(5, e.versao); assertEquals(listOf(a2.id), e.efetiva.map { it.id }); assertFalse(e.pendente)
    }

    @Test fun copiaMostraVinculoEncerradoQuandoOServidorNega() = runTest {
        val l = Lista(ListaSubstituicaoResposta(paciente, 1, emptyList(), emptyList(), emptyList())); l.falha = ErroApi("ACESSO_NEGADO", null, emptyList(), 403, null, null)
        val vm = ListaPacienteViewModel(l, Catalogo(listOf(a1), grupo), StandardTestDispatcher(testScheduler))
        vm.abrir(paciente); advanceUntilIdle(); vm.pedirCopia(Uuid.random()); vm.confirmarCopia(); advanceUntilIdle()
        assertEquals(MensagemLista.VINCULO_ENCERRADO, vm.estado.value.erro)
    }
}
