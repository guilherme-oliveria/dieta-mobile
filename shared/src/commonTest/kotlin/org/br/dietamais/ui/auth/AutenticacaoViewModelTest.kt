package org.br.dietamais.ui.auth

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.br.dietamais.data.remoto.*
import org.br.dietamais.data.remoto.dto.*
import org.br.dietamais.data.repositorio.PortaAutenticacao
import kotlin.test.*
import kotlin.uuid.Uuid

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class AutenticacaoViewModelTest {
    private val usuario = UsuarioResposta(Uuid.parse("019a0f32-5000-7000-8000-000000000001"), "Conta fictícia",
        "teste@example.invalid", false, listOf(Perfil.NUTRICIONISTA), Perfil.NUTRICIONISTA, fuso = "America/Sao_Paulo")
    private val termos = TermosCadastro("TESTE-SEM-VALIDADE", "Texto fictício apenas para testes.")

    @Test fun entrarSemCamposNaoChamaServidor() = runTest {
        val porta = PortaTeste()
        val vm = AutenticacaoViewModel(porta, termos, StandardTestDispatcher(testScheduler))
        runCurrent()
        vm.entrar(); runCurrent()
        assertEquals(0, porta.entradas.size)
        assertEquals(MensagemAutenticacao.OBRIGATORIO, vm.estado.value.errosCampos["email"])
        assertNull(vm.estado.value.usuario)
    }

    @Test fun loginAtivaContaDescartaSenhaEMostraEmailNaoConfirmado() = runTest {
        val porta = PortaTeste().apply { resposta = SessaoResposta(usuario = usuario) }
        val vm = AutenticacaoViewModel(porta, termos, StandardTestDispatcher(testScheduler))
        runCurrent()
        vm.mudarEmail(" teste@example.invalid "); vm.mudarSenha("SENHA_FICTICIA"); vm.entrar(); runCurrent()
        assertEquals(usuario, vm.estado.value.usuario)
        assertEquals("", vm.estado.value.senha)
        assertFalse(vm.estado.value.usuario!!.emailConfirmado)
        assertEquals("teste@example.invalid", porta.entradas.single().email)
    }

    @Test fun variosPerfisPedemEscolhaESenhaFicaSomenteAteEscolher() = runTest {
        val porta = PortaTeste().apply { resposta = SessaoResposta(usuario = usuario.copy(perfil = null, perfis = listOf(Perfil.NUTRICIONISTA, Perfil.PACIENTE))) }
        val vm = AutenticacaoViewModel(porta, termos, StandardTestDispatcher(testScheduler))
        runCurrent()
        vm.mudarEmail(usuario.email); vm.mudarSenha("SENHA_FICTICIA"); vm.entrar(); runCurrent()
        assertEquals(TelaAutenticacao.ESCOLHER_PERFIL, vm.estado.value.tela)
        assertNull(vm.estado.value.usuario)
        assertEquals("SENHA_FICTICIA", vm.estado.value.senha)
        vm.escolherPerfil(Perfil.ADMIN); runCurrent()
        assertEquals(1, porta.entradas.size)
        porta.resposta = SessaoResposta(usuario = usuario)
        vm.escolherPerfil(Perfil.NUTRICIONISTA); runCurrent()
        assertEquals(Perfil.NUTRICIONISTA, porta.entradas.last().perfil)
        assertEquals("", vm.estado.value.senha)
        assertEquals(usuario, vm.estado.value.usuario)
    }

    @Test fun cancelarEscolhaDescartaSenhaERespostaTardiaNaoNavega() = runTest {
        val liberar = CompletableDeferred<Unit>()
        val porta = PortaTeste().apply { esperaEntrada = liberar; resposta = SessaoResposta(usuario = usuario) }
        val vm = AutenticacaoViewModel(porta, termos, StandardTestDispatcher(testScheduler))
        runCurrent()
        vm.mudarEmail(usuario.email); vm.mudarSenha("SENHA_FICTICIA"); vm.entrar(); runCurrent()
        vm.voltar(); liberar.complete(Unit); runCurrent()
        assertNull(vm.estado.value.usuario)
        assertEquals("", vm.estado.value.senha)
    }

    @Test fun bloqueioUsaCodigoSemExibirTextoRemoto() = runTest {
        val porta = PortaTeste().apply { falha = ErroApi("LOGIN_BLOQUEADO", null, emptyList(), 429, null, null) }
        val vm = AutenticacaoViewModel(porta, termos, StandardTestDispatcher(testScheduler))
        runCurrent()
        vm.mudarEmail(usuario.email); vm.mudarSenha("SENHA_FICTICIA"); vm.entrar(); runCurrent()
        assertEquals(MensagemAutenticacao.LOGIN_BLOQUEADO, vm.estado.value.erro)
        assertNull(vm.estado.value.usuario)
    }

    @Test fun cadastroSemTermosPublicadosNaoFabricaAceite() = runTest {
        val porta = PortaTeste()
        val vm = AutenticacaoViewModel(porta, null, StandardTestDispatcher(testScheduler))
        runCurrent(); vm.abrirCadastro(); vm.aceitarTermos(true); vm.cadastrar(); runCurrent()
        assertEquals(0, porta.cadastros.size)
        assertFalse(vm.estado.value.aceitouTermos)
        assertEquals(MensagemAutenticacao.TERMOS_INDISPONIVEIS, vm.estado.value.erro)
    }

    @Test fun cadastroEnviaVersaoExibidaSemLoginAutomatico() = runTest {
        val porta = PortaTeste()
        val vm = AutenticacaoViewModel(porta, termos, StandardTestDispatcher(testScheduler))
        runCurrent(); vm.abrirCadastro()
        vm.mudarNome("Nutri fictícia"); vm.mudarCrn("CRN-1 0000"); vm.mudarEmail(usuario.email)
        vm.mudarSenha("SENHA_FICTICIA_FORTE"); vm.aceitarTermos(true); vm.cadastrar(); runCurrent()
        assertEquals(termos.versao, porta.cadastros.single().versaoTermos)
        assertTrue(porta.cadastros.single().aceitouTermos)
        assertTrue(porta.entradas.isEmpty())
        assertEquals(TelaAutenticacao.ENTRAR, vm.estado.value.tela)
        assertEquals(MensagemAutenticacao.CADASTRO_CONCLUIDO, vm.estado.value.aviso)
        assertEquals("", vm.estado.value.senha)
    }

    @Test fun recuperacaoMostraRespostaGenericaSemDizerSeEmailExiste() = runTest {
        val porta = PortaTeste()
        val vm = AutenticacaoViewModel(porta, termos, StandardTestDispatcher(testScheduler))
        runCurrent(); vm.abrirRecuperacao(); vm.mudarEmail(usuario.email); vm.recuperarSenha(); runCurrent()
        assertEquals(listOf(usuario.email), porta.recuperacoes)
        assertEquals(MensagemAutenticacao.RECUPERACAO_ENVIADA, vm.estado.value.aviso)
        assertFalse(vm.estado.value.carregando)
    }

    @Test fun submissaoDuplaNaoRepeteOperacaoEFalhaRedePermiteTentarNovamente() = runTest {
        val liberar = CompletableDeferred<Unit>()
        val porta = PortaTeste().apply { esperaEntrada = liberar; falha = FalhaComunicacao() }
        val vm = AutenticacaoViewModel(porta, termos, StandardTestDispatcher(testScheduler))
        runCurrent(); vm.mudarEmail(usuario.email); vm.mudarSenha("SENHA_FICTICIA")
        vm.entrar(); vm.entrar(); runCurrent()
        assertEquals(1, porta.entradas.size)
        liberar.complete(Unit); runCurrent()
        assertEquals(MensagemAutenticacao.SEM_CONEXAO, vm.estado.value.erro)
        assertFalse(vm.estado.value.carregando)
    }

    @Test fun cadastroReutilizaChaveAposFalhaDeRedeEAlteraAoEditarFormulario() = runTest {
        val porta = PortaTeste().apply { falhaCadastro = FalhaComunicacao() }
        val vm = AutenticacaoViewModel(porta, termos, StandardTestDispatcher(testScheduler))
        runCurrent(); vm.abrirCadastro(); vm.mudarNome("Nutri fictícia"); vm.mudarCrn("CRN-1 0000")
        vm.mudarEmail(usuario.email); vm.mudarSenha("SENHA_FICTICIA_FORTE"); vm.aceitarTermos(true)
        vm.cadastrar(); runCurrent(); vm.cadastrar(); runCurrent()
        assertEquals(porta.chavesCadastro[0], porta.chavesCadastro[1])
        vm.mudarNome("Outra nutri fictícia"); vm.cadastrar(); runCurrent()
        assertNotEquals(porta.chavesCadastro[1], porta.chavesCadastro[2])
    }

    @Test fun restauracaoAbreContaSalvaAntesDaRedeEResisteAFalhaTransitoria() = runTest {
        val liberar = CompletableDeferred<Unit>()
        val porta = PortaTeste().apply {
            salvo = usuario; sessao.value = ContextoSessao(usuario.id, Perfil.NUTRICIONISTA)
            esperaEu = liberar; falhaEu = FalhaComunicacao()
        }
        val vm = AutenticacaoViewModel(porta, termos, StandardTestDispatcher(testScheduler))
        runCurrent()
        assertEquals(usuario, vm.estado.value.usuario)
        assertFalse(vm.estado.value.restaurando)
        liberar.complete(Unit); runCurrent()
        assertEquals(usuario, vm.estado.value.usuario)
        assertEquals(0, porta.saidas)
    }

    @Test fun recarregarPaginaMostraCarregandoEDepoisAContaRestauradaSemPassarPelaTelaDeEntrar() = runTest {
        val liberar = CompletableDeferred<Unit>()
        val porta = PortaTeste().apply { esperaRestaurar = liberar; salvo = usuario; sessao.value = ContextoSessao(usuario.id, Perfil.NUTRICIONISTA) }
        val vm = AutenticacaoViewModel(porta, termos, StandardTestDispatcher(testScheduler))
        runCurrent()
        assertTrue(vm.estado.value.restaurando, "enquanto o refresh não responde, a tela é de carregando e não de Entrar")
        assertNull(vm.estado.value.usuario)
        liberar.complete(Unit); runCurrent()
        assertFalse(vm.estado.value.restaurando)
        assertEquals(usuario, vm.estado.value.usuario)
    }

    @Test fun semSessaoNoCookieVaiParaEntrarSemErro() = runTest {
        val porta = PortaTeste().apply { salvo = null }
        val vm = AutenticacaoViewModel(porta, termos, StandardTestDispatcher(testScheduler))
        runCurrent()
        assertFalse(vm.estado.value.restaurando)
        assertNull(vm.estado.value.usuario)
        assertNull(vm.estado.value.erro)
    }

    @Test fun semRedeNaRestauracaoAvisaMasNaoApagaNadaNemFicaPresoNoCarregando() = runTest {
        val porta = PortaTeste().apply { falhaRestaurar = FalhaComunicacao() }
        val vm = AutenticacaoViewModel(porta, termos, StandardTestDispatcher(testScheduler))
        runCurrent()
        assertFalse(vm.estado.value.restaurando)
        assertNull(vm.estado.value.usuario)
        assertEquals(MensagemAutenticacao.SEM_CONEXAO, vm.estado.value.erro)
        assertEquals(0, porta.saidas, "falha de rede não é logout")
    }

    @Test fun contextoDeOutroUsuarioRecusaShellEEncerraSessao() = runTest {
        val porta = PortaTeste().apply { respostaEu = EuResposta(usuario.copy(id = Uuid.random()), vinculos = emptyList()) }
        val vm = AutenticacaoViewModel(porta, termos, StandardTestDispatcher(testScheduler))
        runCurrent(); vm.mudarEmail(usuario.email); vm.mudarSenha("SENHA_FICTICIA"); vm.entrar(); runCurrent()
        assertNull(vm.estado.value.usuario)
        assertEquals(MensagemAutenticacao.RESPOSTA_INVALIDA, vm.estado.value.erro)
        assertEquals(1, porta.saidas)
    }

    @Test fun sessaoRetornadaDoConviteTrocaShellEDescartaCredenciais() = runTest {
        val paciente = usuario.copy(perfil = Perfil.PACIENTE, perfis = listOf(Perfil.NUTRICIONISTA, Perfil.PACIENTE), pacienteId = Uuid.random())
        val porta = PortaTeste()
        val vm = AutenticacaoViewModel(porta, null, StandardTestDispatcher(testScheduler))
        runCurrent(); vm.mudarEmail(usuario.email); vm.mudarSenha("SENHA_FICTICIA")
        porta.salvo = paciente; porta.respostaEu = EuResposta(paciente, vinculos = emptyList())
        vm.recarregarSessaoDoConvite(); runCurrent()
        assertEquals(paciente, vm.estado.value.usuario)
        assertEquals("", vm.estado.value.senha)
        assertEquals(0, porta.entradas.size)
    }

    private inner class PortaTeste : PortaAutenticacao {
        override val sessao = MutableStateFlow<ContextoSessao?>(null)
        var resposta = SessaoResposta(usuario = usuario)
        var falha: Exception? = null
        var esperaEntrada: CompletableDeferred<Unit>? = null
        var falhaCadastro: Exception? = null
        var salvo: UsuarioResposta? = null
        var falhaRestaurar: Exception? = null
        var esperaRestaurar: CompletableDeferred<Unit>? = null
        var respostaEu: EuResposta? = null
        var falhaEu: Exception? = null
        var esperaEu: CompletableDeferred<Unit>? = null
        var saidas = 0
        val entradas = mutableListOf<LoginRequisicao>()
        val cadastros = mutableListOf<CadastroNutriRequisicao>()
        val chavesCadastro = mutableListOf<Uuid>()
        val recuperacoes = mutableListOf<String>()
        override suspend fun restaurar(): UsuarioResposta? { esperaRestaurar?.await(); falhaRestaurar?.let { throw it }; return salvo }
        override suspend fun entrar(email: String, senha: String, perfil: Perfil?): SessaoResposta {
            entradas += LoginRequisicao(email, senha, DispositivoRequisicao(Uuid.random(), Plataforma.DESKTOP, "Teste"), perfil)
            esperaEntrada?.await(); falha?.let { throw it }; return resposta
        }
        override suspend fun cadastrar(requisicao: CadastroNutriRequisicao, chave: Uuid) {
            cadastros += requisicao; chavesCadastro += chave; falhaCadastro?.let { throw it }
        }
        override suspend fun recuperar(email: String) { recuperacoes += email }
        override suspend fun redefinir(token: String, senha: String) {}
        override suspend fun consultarEu(): EuResposta {
            esperaEu?.await(); falhaEu?.let { throw it }
            return respostaEu ?: EuResposta(usuario = resposta.usuario, vinculos = emptyList())
        }
        override suspend fun sair() { saidas++; sessao.value = null }
    }
}
