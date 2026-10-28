package org.br.dietamais.ui.paciente.anamnese

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import kotlinx.serialization.json.JsonPrimitive
import org.br.dietamais.data.remoto.ErroApi
import org.br.dietamais.data.remoto.FalhaComunicacao
import org.br.dietamais.data.remoto.dto.*
import org.br.dietamais.data.repositorio.RespostasPendentes
import org.br.dietamais.dominio.anamnese.*
import org.br.dietamais.ui.nutri.anamnese.*
import kotlin.test.*
import kotlin.uuid.Uuid

@OptIn(ExperimentalCoroutinesApi::class)
class AnamnesePacienteViewModelTest {
    private val paciente = Uuid.random()
    private val fuma = perguntaFalsa("Você fuma?", TipoPergunta.ESCOLHA_UNICA, true, listOf("Sim", "Não"))
    private val quanto = perguntaFalsa("Quantos por dia?", TipoPergunta.NUMERO, true, condicao = CondicaoPergunta(fuma.chave, OperadorCondicao.IGUAL, "Sim"))
    private val peso = perguntaFalsa("Peso desejado", TipoPergunta.NUMERO)
    private val blocos = listOf(BlocoQuestionario("Hábitos", listOf(fuma, quanto)), BlocoQuestionario("Metas", listOf(peso)))
    private fun TestScope.vm(p: PortaAnamneseFalsa, online: Boolean = true, sincronizar: suspend () -> Unit = {}) =
        AnamnesePacienteViewModel(p, sincronizar, StandardTestDispatcher(testScheduler), flowOf(online))
    private fun erro(codigo: String) = ErroApi(codigo, null, emptyList(), 400, null, null)
    private fun TestScope.aberto(p: PortaAnamneseFalsa, q: QuestionarioResposta, online: Boolean = true, sincronizar: suspend () -> Unit = {}) =
        vm(p, online, sincronizar).also { advanceUntilIdle(); it.abrir(q.id) }

    @Test fun soAbrePendentes() = runTest {
        val feito = questionarioFalso(paciente, blocos, StatusQuestionario.RESPONDIDO); val p = PortaAnamneseFalsa(questionarios = listOf(feito))
        val vm = vm(p); advanceUntilIdle(); vm.abrir(feito.id)
        assertNull(vm.estado.value.abertoId); assertTrue(vm.estado.value.pendentes.isEmpty())
    }

    @Test fun passoTemSoAPerguntaVisivelECondicionalAparecePosEscolha() = runTest {
        val q = questionarioFalso(paciente, blocos); val p = PortaAnamneseFalsa(questionarios = listOf(q)); val vm = aberto(p, q)
        assertEquals(listOf("Hábitos", "Metas"), vm.estado.value.passos.map { it.titulo })
        assertEquals(listOf(fuma.chave), vm.estado.value.passos[0].perguntas.map { it.chave })
        vm.mudar(fuma.chave, EntradaResposta.Valor(texto("Sim"))); advanceUntilIdle()
        assertEquals(listOf(fuma.chave, quanto.chave), vm.estado.value.passos[0].perguntas.map { it.chave })
    }

    @Test fun escolhaGravaNaHoraOffline() = runTest {
        val q = questionarioFalso(paciente, blocos); val p = PortaAnamneseFalsa(questionarios = listOf(q)); val vm = aberto(p, q, online = false)
        vm.mudar(fuma.chave, EntradaResposta.Valor(texto("Não"))); advanceUntilIdle()
        assertEquals(listOf<Map<Uuid, kotlinx.serialization.json.JsonElement?>>(mapOf(fuma.chave to texto("Não"))), p.gravacoes)
    }

    @Test fun proximoBarraObrigatoriaNaoRespondida() = runTest {
        val q = questionarioFalso(paciente, blocos); val p = PortaAnamneseFalsa(questionarios = listOf(q)); val vm = aberto(p, q)
        vm.proximo(); advanceUntilIdle()
        assertEquals(0, vm.estado.value.passoAtual); assertEquals(ErroResposta.OBRIGATORIA, vm.estado.value.erros[fuma.chave]); assertEquals(MensagemAnamnese.OBRIGATORIAS, vm.estado.value.mensagem)
    }

    @Test fun proximoGravaDigitacaoValidaEAvanca() = runTest {
        val q = questionarioFalso(paciente, blocos); val p = PortaAnamneseFalsa(questionarios = listOf(q)); val vm = aberto(p, q)
        vm.mudar(fuma.chave, EntradaResposta.Valor(texto("Sim"))); vm.mudar(quanto.chave, EntradaResposta.Texto("10,5")); vm.proximo(); advanceUntilIdle()
        assertEquals(1, vm.estado.value.passoAtual)
        assertEquals(JsonPrimitive(10.5), p.questionarios.value.single().respostas[quanto.chave])
    }

    @Test fun digitacaoInvalidaNaoGravaENaoAvanca() = runTest {
        val q = questionarioFalso(paciente, blocos); val p = PortaAnamneseFalsa(questionarios = listOf(q)); val vm = aberto(p, q)
        vm.mudar(fuma.chave, EntradaResposta.Valor(texto("Sim"))); vm.mudar(quanto.chave, EntradaResposta.Texto("muitos")); vm.proximo(); advanceUntilIdle()
        assertEquals(0, vm.estado.value.passoAtual); assertEquals(ErroResposta.INVALIDA, vm.estado.value.erros[quanto.chave]); assertEquals(MensagemAnamnese.ENTRADA_INVALIDA, vm.estado.value.mensagem)
        assertNull(p.questionarios.value.single().respostas[quanto.chave])
    }

    @Test fun naoRegravaORespostaQueNaoMudou() = runTest {
        val q = questionarioFalso(paciente, blocos, respostas = mapOf(fuma.chave to texto("Não"))); val p = PortaAnamneseFalsa(questionarios = listOf(q)); val vm = aberto(p, q)
        vm.mudar(fuma.chave, EntradaResposta.Valor(texto("Não"))); vm.proximo(); advanceUntilIdle()
        assertTrue(p.gravacoes.isEmpty())
    }

    @Test fun sairGuardaOQueEstaValido() = runTest {
        val q = questionarioFalso(paciente, blocos); val p = PortaAnamneseFalsa(questionarios = listOf(q)); val vm = aberto(p, q)
        vm.mudar(peso.chave, EntradaResposta.Texto("70")); vm.fechar(); advanceUntilIdle()
        assertNull(vm.estado.value.abertoId); assertEquals(JsonPrimitive(70.0), p.questionarios.value.single().respostas[peso.chave])
    }

    @Test fun concluirExigeObrigatoriasEVaiAoPassoDelas() = runTest {
        val q = questionarioFalso(paciente, blocos, respostas = mapOf(fuma.chave to texto("Sim"))); val p = PortaAnamneseFalsa(questionarios = listOf(q)); val vm = aberto(p, q)
        vm.proximo(); advanceUntilIdle()
        assertEquals(MensagemAnamnese.OBRIGATORIAS, vm.estado.value.mensagem) // "Quantos por dia?" é obrigatória e agora visível
        vm.mudar(quanto.chave, EntradaResposta.Texto("5")); vm.proximo(); advanceUntilIdle()
        vm.concluir(); advanceUntilIdle()
        assertEquals(1, p.concluidos); assertNull(vm.estado.value.abertoId); assertEquals(MensagemAnamnese.CONCLUIDO, vm.estado.value.mensagem)
    }

    @Test fun concluirSincronizaAntesDeEnviar() = runTest {
        val q = questionarioFalso(paciente, blocos, respostas = mapOf(fuma.chave to texto("Não"))); val p = PortaAnamneseFalsa(questionarios = listOf(q))
        var sincronizou = 0
        val vm = aberto(p, q, sincronizar = { sincronizou++; assertEquals(0, p.concluidos) })
        vm.concluir(); advanceUntilIdle()
        assertEquals(1, sincronizou); assertEquals(1, p.concluidos)
    }

    @Test fun semConexaoNaoConclui() = runTest {
        val q = questionarioFalso(paciente, blocos, respostas = mapOf(fuma.chave to texto("Não"))); val p = PortaAnamneseFalsa(questionarios = listOf(q)); val vm = aberto(p, q, online = false)
        vm.concluir(); advanceUntilIdle()
        assertEquals(0, p.concluidos); assertNotNull(vm.estado.value.abertoId)
    }

    @Test fun respostasAindaNaFilaAvisamEMantemOAssistente() = runTest {
        val q = questionarioFalso(paciente, blocos, respostas = mapOf(fuma.chave to texto("Não"))); val p = PortaAnamneseFalsa(questionarios = listOf(q)); val vm = aberto(p, q)
        p.falha = RespostasPendentes(); vm.concluir(); advanceUntilIdle()
        assertEquals(MensagemAnamnese.PENDENTES, vm.estado.value.mensagem); assertNotNull(vm.estado.value.abertoId); assertFalse(vm.estado.value.concluindo)
    }

    @Test fun questionarioFechadoNoServidor() = runTest {
        val q = questionarioFalso(paciente, blocos, respostas = mapOf(fuma.chave to texto("Não"))); val p = PortaAnamneseFalsa(questionarios = listOf(q)); val vm = aberto(p, q)
        p.falha = erro("QUESTIONARIO_FECHADO"); vm.concluir(); advanceUntilIdle()
        assertEquals(MensagemAnamnese.FECHADO, vm.estado.value.mensagem)
    }

    @Test fun redeCaidaNaHoraDeConcluirMarcaSemConexao() = runTest {
        val q = questionarioFalso(paciente, blocos, respostas = mapOf(fuma.chave to texto("Não"))); val p = PortaAnamneseFalsa(questionarios = listOf(q)); val vm = aberto(p, q)
        p.falha = FalhaComunicacao(); vm.concluir(); advanceUntilIdle()
        assertTrue(vm.estado.value.semConexao); assertEquals(MensagemAnamnese.SEM_CONEXAO, vm.estado.value.mensagem)
    }
}
