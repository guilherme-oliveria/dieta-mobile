package org.br.dietamais.ui.auth.convite

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.*
import org.br.dietamais.data.remoto.*
import org.br.dietamais.data.remoto.dto.*
import org.br.dietamais.data.repositorio.PortaConvites
import kotlin.test.*
import kotlin.time.Instant
import kotlin.uuid.Uuid

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ConviteViewModelTest {
    private val nutri = NutricionistaResumoResposta(Uuid.random(), "Nutri fictícia", "CRN-1 0000")
    private val convite = ConviteResposta(nutri, TermoConsentimentoResposta("TESTE", "Consentimento fictício apenas para teste."), Instant.parse("2030-01-01T00:00:00Z"))
    private val usuario = UsuarioResposta(Uuid.random(), "Conta fictícia", "conta@example.invalid", true, listOf(Perfil.PACIENTE), Perfil.PACIENTE, pacienteId = Uuid.random(), fuso = "America/Sao_Paulo")

    @Test fun consultarLinkNaoAceitaNemMarcaConsentimento() = runTest {
        val porta = PortaTeste(); val vm = ConviteViewModel(porta, dispatcher = StandardTestDispatcher(testScheduler))
        vm.abrir("dietamais://convites/ABCD2345"); runCurrent()
        assertEquals(convite, vm.estado.value.convite)
        assertFalse(vm.estado.value.aceitouConsentimento)
        assertTrue(porta.aceites.isEmpty())
        vm.sessaoMudou(usuario); vm.aceitar(); runCurrent()
        assertEquals(MensagemConvite.ACEITE_OBRIGATORIO, vm.estado.value.erro)
        assertTrue(porta.aceites.isEmpty())
    }

    @Test fun loginPreservaConviteMasExigeNovoToqueNoAceite() = runTest {
        val porta = PortaTeste(); val vm = ConviteViewModel(porta, dispatcher = StandardTestDispatcher(testScheduler))
        vm.abrir("ABCD2345"); runCurrent(); vm.aguardarLogin()
        assertFalse(vm.estado.value.visivel)
        vm.sessaoMudou(usuario)
        assertTrue(vm.estado.value.visivel)
        assertEquals(convite, vm.estado.value.convite)
        assertTrue(porta.aceites.isEmpty())
        vm.aceitarConsentimento(true); vm.aceitar(); runCurrent()
        assertNull(porta.aceites.single().cadastro)
        assertEquals(convite.termo.versao, porta.aceites.single().versao)
        assertTrue(vm.estado.value.concluido)
    }

    @Test fun contaNovaSemTermosAprovadosNaoEnviaCadastroMesmoComConsentimentoClinico() = runTest {
        val porta = PortaTeste(); val vm = ConviteViewModel(porta, dispatcher = StandardTestDispatcher(testScheduler))
        vm.abrir("ABCD2345"); runCurrent(); vm.abrirCadastro(); runCurrent(); vm.aceitarConsentimento(true); vm.aceitarTermosUso(true); vm.aceitar(); runCurrent()
        assertEquals(MensagemConvite.TERMOS_INDISPONIVEIS, vm.estado.value.erro)
        assertFalse(vm.estado.value.aceitouTermosUso)
        assertTrue(porta.aceites.isEmpty())
    }

    @Test fun retentativaPreservaChaveEEvitaEnvioDuplo() = runTest {
        val porta = PortaTeste().apply { falhaAceitar = FalhaComunicacao() }
        val vm = ConviteViewModel(porta, dispatcher = StandardTestDispatcher(testScheduler))
        vm.sessaoMudou(usuario); vm.abrir("ABCD2345"); runCurrent(); vm.aceitarConsentimento(true)
        vm.aceitar(); vm.aceitar(); runCurrent(); vm.aceitar(); runCurrent()
        assertEquals(2, porta.aceites.size)
        assertEquals(porta.aceites[0].chave, porta.aceites[1].chave)
        assertEquals(MensagemConvite.SEM_CONEXAO, vm.estado.value.erro)
    }

    @Test fun novaEntradaDescartaConsentimentoERespostaDaConsultaAnterior() = runTest {
        val liberar = CompletableDeferred<Unit>()
        val porta = PortaTeste().apply { esperaConsulta = liberar }
        val vm = ConviteViewModel(porta, dispatcher = StandardTestDispatcher(testScheduler))
        vm.abrir("ABCD2345"); runCurrent(); vm.fechar(); liberar.complete(Unit); runCurrent()
        assertNull(vm.estado.value.convite)
        assertFalse(vm.estado.value.visivel)
        porta.esperaConsulta = null; vm.abrir("ZZZZ2345"); runCurrent()
        assertFalse(vm.estado.value.aceitouConsentimento)
        assertEquals("ZZZZ2345", vm.estado.value.codigo)
    }

    @Test fun erroDeExpiracaoOuInvalidezUsaCodigoSemAceitar() = runTest {
        listOf("CONVITE_EXPIRADO" to MensagemConvite.EXPIRADO, "CONVITE_INVALIDO" to MensagemConvite.INVALIDO,
            "TAXA_EXCEDIDA" to MensagemConvite.TAXA_EXCEDIDA, "CONSENTIMENTO_OBRIGATORIO" to MensagemConvite.CONSENTIMENTO_INDISPONIVEL).forEach { (codigo, esperado) ->
            val porta = PortaTeste().apply { falhaConsulta = ErroApi(codigo, null, emptyList(), 422, null, null) }
            val vm = ConviteViewModel(porta, dispatcher = StandardTestDispatcher(testScheduler))
            vm.abrir("ABCD2345"); runCurrent()
            assertEquals(esperado, vm.estado.value.erro)
            assertNull(vm.estado.value.convite)
        }
    }

    @Test fun codigoOuLinkForaDoEscopoNaoFazRequisicao() = runTest {
        val porta = PortaTeste(); val vm = ConviteViewModel(porta, dispatcher = StandardTestDispatcher(testScheduler))
        vm.abrir("https://falso.example.invalid/convite/ABCD2345"); runCurrent()
        assertTrue(porta.consultas.isEmpty())
        assertEquals(MensagemConvite.CODIGO_INVALIDO, vm.estado.value.erro)
    }

    @Test fun cadastroPublicadoExigeDoisAceitesEEnviaVersoesSeparadas() = runTest {
        val porta = PortaTeste().apply { termos = termosPublicados() }
        val vm = ConviteViewModel(porta, dispatcher = StandardTestDispatcher(testScheduler))
        vm.abrir("ABCD2345"); runCurrent(); vm.abrirCadastro(); runCurrent()
        vm.mudarCampo(CampoCadastroPaciente.NOME, "Paciente fictício"); vm.mudarCampo(CampoCadastroPaciente.EMAIL, "paciente@example.invalid")
        vm.mudarCampo(CampoCadastroPaciente.SENHA, "SENHA_FICTICIA_SEGURA"); vm.aceitarConsentimento(true); vm.aceitar(); runCurrent()
        assertTrue(porta.aceites.isEmpty())
        assertEquals(MensagemConvite.ACEITE_TERMOS_OBRIGATORIO, vm.estado.value.erro)
        vm.aceitarTermosUso(true); vm.aceitar(); runCurrent()
        val recebido = porta.aceites.single()
        assertEquals(convite.termo.versao, recebido.versao)
        assertEquals("TERMOS-TESTE", recebido.cadastro?.versaoTermos)
        assertEquals(true, recebido.cadastro?.aceitouTermos)
        assertTrue(vm.estado.value.concluido)
    }

    @Test fun atualizarVersaoDosTermosDescartaSeuAceiteSemFabricarNovo() = runTest {
        val porta = PortaTeste().apply { termos = termosPublicados() }
        val vm = ConviteViewModel(porta, dispatcher = StandardTestDispatcher(testScheduler))
        vm.abrir("ABCD2345"); runCurrent(); vm.abrirCadastro(); runCurrent(); vm.aceitarTermosUso(true)
        assertTrue(vm.estado.value.aceitouTermosUso)
        porta.termos = termosPublicados().copy(versao = "TERMOS-TESTE-2")
        vm.atualizarTermos(); runCurrent()
        assertFalse(vm.estado.value.aceitouTermosUso)
        assertEquals("TERMOS-TESTE-2", vm.estado.value.termosUso?.versao)
    }

    @Test fun repetirMesmoLinkPreservaLeituraEAceiteSemReconsultar() = runTest {
        val porta = PortaTeste(); val vm = ConviteViewModel(porta, dispatcher = StandardTestDispatcher(testScheduler))
        vm.sessaoMudou(usuario); vm.abrir("ABCD2345"); runCurrent(); vm.aceitarConsentimento(true)
        vm.abrir("dietamais://convites/ABCD2345"); runCurrent()
        assertTrue(vm.estado.value.aceitouConsentimento)
        assertEquals(listOf("ABCD2345"), porta.consultas)
    }

    @Test fun consultaDeTermosPendenteNaoImpedeEntrarEmContaExistente() = runTest {
        val liberar = CompletableDeferred<Unit>()
        val porta = PortaTeste().apply { esperaTermos = liberar }
        val vm = ConviteViewModel(porta, dispatcher = StandardTestDispatcher(testScheduler))
        vm.abrir("ABCD2345"); runCurrent(); vm.abrirCadastro(); runCurrent()
        assertTrue(vm.estado.value.carregando)
        vm.aguardarLogin(); vm.sessaoMudou(usuario); liberar.complete(Unit); runCurrent()
        assertTrue(vm.estado.value.visivel)
        assertFalse(vm.estado.value.carregando)
        vm.aceitarConsentimento(true); vm.aceitar(); runCurrent()
        assertNull(porta.aceites.single().cadastro)
        assertTrue(vm.estado.value.concluido)
    }

    private fun termosPublicados() = TermosUsoResposta("TERMOS-TESTE", "PUBLICADO", true, true, true,
        Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-01-02T00:00:00Z"), "Documento fictício de teste.", "/legal/terms-of-use")

    private data class Aceite(val versao: String, val cadastro: CadastroPacienteRequisicao?, val chave: Uuid)
    private inner class PortaTeste : PortaConvites {
        var falhaConsulta: Exception? = null; var falhaAceitar: Exception? = null
        var esperaConsulta: CompletableDeferred<Unit>? = null
        var esperaTermos: CompletableDeferred<Unit>? = null
        var termos = termosPublicados().copy(status = "RASCUNHO", aprovado = false, publicado = false, cadastroHabilitado = false, aprovadoEm = null, publicadoEm = null)
        val consultas = mutableListOf<String>(); val aceites = mutableListOf<Aceite>()
        override suspend fun consultar(codigo: String): ConviteResposta {
            consultas += codigo; esperaConsulta?.await(); falhaConsulta?.let { throw it }; return convite
        }
        override suspend fun consultarTermosUso(): TermosUsoResposta { esperaTermos?.await(); return termos }
        override suspend fun aceitar(codigo: String, versaoTermo: String, cadastro: CadastroPacienteRequisicao?, chave: Uuid): AceiteConviteResposta {
            aceites += Aceite(versaoTermo, cadastro, chave); falhaAceitar?.let { throw it }
            return AceiteConviteResposta(SessaoResposta(usuario = usuario), VinculoResposta(Uuid.random(), usuario.pacienteId!!, nutri,
                StatusVinculo.ATIVO, versao = 1, atualizadoEm = Instant.parse("2026-01-01T00:00:00Z")))
        }
    }
}
