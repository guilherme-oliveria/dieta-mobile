package org.br.dietamais.ui.nutri.alimentos

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.br.dietamais.data.remoto.*
import org.br.dietamais.data.remoto.dto.*
import org.br.dietamais.data.repositorio.*
import kotlin.test.*
import kotlin.time.Instant
import kotlin.uuid.Uuid

@OptIn(ExperimentalCoroutinesApi::class)
class AlimentosViewModelTest {
    private val grupo = GrupoAlimentoResposta(Uuid.parse("019a0f32-5000-7000-8000-000000000001"), "Grupo fictício", 150.0, 25.0, 5.0, 3.0)
    private fun alimento() = AlimentoResposta(Uuid.parse("019a0f32-5000-7000-8000-000000000002"), grupoId = grupo.id,
        nome = "Alimento fictício", medidaCaseira = "1 unidade fictícia", gramasPorPorcao = 50.0, ativo = true, versao = 1,
        atualizadoEm = Instant.parse("2026-10-26T12:00:00Z"))

    @Test fun buscaGrupoEAtivoFiltramCacheSemHttpExtra() = runTest {
        val porta = PortaTeste(); val vm = AlimentosViewModel(porta, StandardTestDispatcher(testScheduler))
        runCurrent(); assertEquals(1, vm.estado.value.lista.size)
        vm.mudarBusca("inexistente"); assertTrue(vm.estado.value.lista.isEmpty())
        vm.mudarBusca("FICTÍCIO"); vm.mudarGrupo(Uuid.random()); assertTrue(vm.estado.value.lista.isEmpty())
        vm.mudarGrupo(null); vm.mudarAtivo(false); assertTrue(vm.estado.value.lista.isEmpty())
        vm.mudarAtivo(true); assertEquals(1, vm.estado.value.lista.size); assertEquals(1, porta.atualizacoes)
    }
    @Test fun formularioInvalidoNaoEnviaERejeitaNumeroNaoFinito() = runTest {
        val porta = PortaTeste(); val vm = AlimentosViewModel(porta, StandardTestDispatcher(testScheduler))
        runCurrent(); vm.novo(); vm.mudarCampo(CampoAlimento.GRAMAS, "NaN"); vm.salvar(); runCurrent()
        assertTrue(porta.criacoes.isEmpty())
        assertEquals(MensagemAlimentos.OBRIGATORIO, vm.estado.value.errosCampos["nome"])
        assertEquals(MensagemAlimentos.POSITIVO, vm.estado.value.errosCampos["gramasPorPorcao"])
    }
    @Test fun retentativaPreservaChaveEIdMasEditarFormularioTrocaChave() = runTest {
        val porta = PortaTeste().apply { falha = FalhaComunicacao() }
        val vm = AlimentosViewModel(porta, StandardTestDispatcher(testScheduler)); runCurrent()
        vm.novo(); vm.mudarCampo(CampoAlimento.NOME, "Alimento fictício"); vm.mudarCampo(CampoAlimento.MEDIDA, "1 unidade fictícia")
        vm.mudarCampo(CampoAlimento.GRAMAS, "50,5"); vm.salvar(); vm.salvar(); runCurrent()
        assertEquals(1, porta.criacoes.size)
        porta.falha = null; vm.atualizar(); runCurrent(); porta.falha = FalhaComunicacao()
        vm.salvar(); runCurrent(); assertEquals(porta.chaves[0], porta.chaves[1]); assertEquals(porta.criacoes[0], porta.criacoes[1])
        vm.mudarCampo(CampoAlimento.GRAMAS, "51"); assertNotEquals(porta.chaves[0], vm.estado.value.formulario!!.chave)
    }
    @Test fun redeDesabilitaEscritaMasConservaListaSalva() = runTest {
        val rede = MutableStateFlow(true); val porta = PortaTeste()
        val vm = AlimentosViewModel(porta, StandardTestDispatcher(testScheduler), conectividade = rede)
        runCurrent(); rede.value = false; runCurrent(); vm.novo()
        assertNull(vm.estado.value.formulario); assertEquals(1, vm.estado.value.lista.size)
        vm.editar(alimento()); assertNull(vm.estado.value.formulario)
    }
    @Test fun conflitoNaoReenviaAutomaticamenteNemTrocaVersaoDoFormulario() = runTest {
        val porta = PortaTeste().apply { falha = ErroApi("CONFLITO_VERSAO", null, emptyList(), 409, null, null) }
        val vm = AlimentosViewModel(porta, StandardTestDispatcher(testScheduler)); runCurrent()
        vm.editar(alimento()); vm.salvar(); runCurrent(); vm.salvar(); runCurrent()
        assertEquals(1, porta.alteracoes.size); assertTrue(vm.estado.value.conflito)
        assertEquals(1L, vm.estado.value.formulario!!.versao)
        assertEquals(MensagemAlimentos.CONFLITO, vm.estado.value.erro)
    }
    @Test fun desativacaoExigeConfirmacaoEEnviaTodosCamposComVersaoOriginal() = runTest {
        val porta = PortaTeste(); val vm = AlimentosViewModel(porta, StandardTestDispatcher(testScheduler)); runCurrent()
        vm.pedirDesativacao(alimento()); assertTrue(porta.alteracoes.isEmpty())
        vm.confirmarDesativacao(); runCurrent()
        val pedido = porta.alteracoes.single(); assertEquals(1L, pedido.first); assertFalse(pedido.second.ativo)
        assertEquals("1 unidade fictícia", pedido.second.medidaCaseira)
    }
    @Test fun stub501ApresentaIndisponibilidadeSemEsvaziarLista() = runTest {
        val porta = PortaTeste().apply { falhaAtualizar = ErroApi("NAO_IMPLEMENTADO", null, emptyList(), 501, null, null) }
        val vm = AlimentosViewModel(porta, StandardTestDispatcher(testScheduler)); runCurrent()
        assertEquals(MensagemAlimentos.INDISPONIVEL, vm.estado.value.erro); assertEquals(1, vm.estado.value.lista.size)
    }

    @Test fun respostaNaoCooperativaAposFecharNaoFechaNovoFormulario() = runTest {
        val liberar = CompletableDeferred<Unit>(); val porta = PortaTeste().apply { esperaCriar = liberar }
        val vm = AlimentosViewModel(porta, StandardTestDispatcher(testScheduler)); runCurrent()
        vm.novo(); vm.mudarCampo(CampoAlimento.NOME, "Alimento fictício"); vm.mudarCampo(CampoAlimento.MEDIDA, "1 unidade fictícia")
        vm.mudarCampo(CampoAlimento.GRAMAS, "50"); vm.salvar(); runCurrent()
        vm.fechar(); vm.novo(); val novo = vm.estado.value.formulario
        liberar.complete(Unit); runCurrent()
        assertEquals(novo, vm.estado.value.formulario); assertNull(vm.estado.value.aviso)
    }

    @Test fun reentradaNaMesmaContaDescartaFormularioEEstadoTransitorio() = runTest {
        val contexto = ContextoSessao(Uuid.random(), Perfil.NUTRICIONISTA)
        val sessao = MutableStateFlow<OperacaoSessao?>(OperacaoSessao(contexto, 1))
        val vm = AlimentosViewModel(PortaTeste(), StandardTestDispatcher(testScheduler), sessao = sessao)
        runCurrent(); vm.novo(); assertNotNull(vm.estado.value.formulario)
        sessao.value = OperacaoSessao(contexto, 2); runCurrent()
        assertNull(vm.estado.value.formulario); assertFalse(vm.estado.value.enviando)
        sessao.value = null; runCurrent(); assertTrue(vm.estado.value.lista.isEmpty())
    }

    private inner class PortaTeste : PortaAlimentos {
        val dados = MutableStateFlow(CatalogoAlimentos(listOf(grupo), listOf(alimento())))
        var atualizacoes = 0; var falha: Exception? = null; var falhaAtualizar: Exception? = null
        var esperaCriar: CompletableDeferred<Unit>? = null
        val criacoes = mutableListOf<CriarAlimentoRequisicao>(); val chaves = mutableListOf<Uuid>()
        val alteracoes = mutableListOf<Pair<Long, AtualizarAlimentoRequisicao>>()
        override fun observar() = dados
        override suspend fun atualizar() { atualizacoes++; falhaAtualizar?.let { throw it } }
        override suspend fun criar(requisicao: CriarAlimentoRequisicao, chave: Uuid): AlimentoResposta {
            criacoes += requisicao; chaves += chave; falha?.let { throw it }
            return withContext(NonCancellable) { esperaCriar?.await(); alimento().copy(id = requisicao.id!!) }
        }
        override suspend fun alterar(id: Uuid, versao: Long, requisicao: AtualizarAlimentoRequisicao): AlimentoResposta {
            alteracoes += versao to requisicao; falha?.let { throw it }; return alimento().copy(ativo = requisicao.ativo)
        }
    }
}
