package org.br.dietamais.ui.nutri.anamnese

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.br.dietamais.data.remoto.ErroApi
import org.br.dietamais.data.remoto.FalhaComunicacao
import org.br.dietamais.data.remoto.dto.*
import org.br.dietamais.dominio.anamnese.ErroModelo
import kotlin.test.*
import kotlin.uuid.Uuid

@OptIn(ExperimentalCoroutinesApi::class)
class AnamneseNutriViewModelTest {
    private val paciente = Uuid.random()
    private fun modelo(nome: String = "Padrão", padrao: Boolean = true) = ModeloQuestionarioResposta(Uuid.random(), nome, padrao,
        listOf(BlocoQuestionario("Hábitos", listOf(perguntaFalsa("Fuma?", TipoPergunta.ESCOLHA_UNICA, true, listOf("Sim", "Não"))))), 1, instanteFalso, instanteFalso)
    private fun TestScope.vm(p: PortaAnamneseFalsa, online: Boolean = true) = AnamneseNutriViewModel(p, StandardTestDispatcher(testScheduler), flowOf(online))
    private fun erro(codigo: String) = ErroApi(codigo, null, emptyList(), 400, null, null)

    @Test fun abreCarregaModelosEQuestionariosDoPaciente() = runTest {
        val q = questionarioFalso(paciente, modelo().blocos); val outro = questionarioFalso(Uuid.random(), modelo().blocos)
        val p = PortaAnamneseFalsa(listOf(modelo()), listOf(q, outro)); val vm = vm(p); vm.abrir(paciente); advanceUntilIdle()
        assertEquals(1, vm.estado.value.modelos.size); assertEquals(listOf(q.id), vm.estado.value.questionarios.map { it.id }); assertEquals(1, p.atualizacoes)
    }

    @Test fun enviaOModeloComIdEstavelParaIdempotencia() = runTest {
        val m = modelo(); val p = PortaAnamneseFalsa(listOf(m)); val vm = vm(p); vm.abrir(paciente); advanceUntilIdle()
        vm.enviar(m.id); advanceUntilIdle()
        assertEquals(m.id, p.enviado?.modeloId); assertEquals(vm.estado.value.questionarios.single().id, p.enviado?.id)
        assertEquals(MensagemAnamnese.ENVIADO, vm.estado.value.aviso)
    }

    @Test fun semConexaoNaoEnvia() = runTest {
        val m = modelo(); val p = PortaAnamneseFalsa(listOf(m)); p.falha = FalhaComunicacao(); val vm = vm(p, online = false); vm.abrir(paciente); advanceUntilIdle()
        p.falha = null; vm.enviar(m.id); advanceUntilIdle()
        assertNull(p.enviado)
    }

    @Test fun vinculoEncerradoViraMensagem() = runTest {
        val m = modelo(); val p = PortaAnamneseFalsa(listOf(m)); val vm = vm(p); vm.abrir(paciente); advanceUntilIdle()
        p.falha = erro("ACESSO_NEGADO"); vm.enviar(m.id); advanceUntilIdle()
        assertEquals(MensagemAnamnese.VINCULO_ENCERRADO, vm.estado.value.erro); assertFalse(vm.estado.value.enviando)
    }

    @Test fun falhaDeRedeMarcaSemConexao() = runTest {
        val m = modelo(); val p = PortaAnamneseFalsa(listOf(m)); val vm = vm(p); vm.abrir(paciente); advanceUntilIdle()
        p.falha = FalhaComunicacao(); vm.enviar(m.id); advanceUntilIdle()
        assertTrue(vm.estado.value.semConexao); assertEquals(MensagemAnamnese.SEM_CONEXAO, vm.estado.value.erro)
    }

    @Test fun leituraDasRespostasAbreEFecha() = runTest {
        val q = questionarioFalso(paciente, modelo().blocos, StatusQuestionario.RESPONDIDO)
        val vm = vm(PortaAnamneseFalsa(listOf(modelo()), listOf(q))); vm.abrir(paciente); advanceUntilIdle()
        vm.ler(q.id); assertEquals(q.id, vm.estado.value.lendo?.id); vm.fecharLeitura(); assertNull(vm.estado.value.lendo)
    }

    @Test fun criaModeloNovoValidandoAntes() = runTest {
        val p = PortaAnamneseFalsa(); val vm = vm(p); vm.abrir(paciente); advanceUntilIdle()
        vm.gerenciarModelos(true); vm.novoModelo(); vm.salvarModelo(); advanceUntilIdle()
        assertNull(p.modeloCriado); assertEquals(ErroModelo.NOME_VAZIO, vm.estado.value.validacao.nome)
        vm.mudarNome("  Esportivo "); vm.mudarBloco(0, "Treino"); vm.adicionarPergunta(0)
        val pergunta = vm.estado.value.editor!!.blocos[0].perguntas.single()
        vm.mudarPergunta(pergunta.id) { it.copy(texto = "Quantas vezes treina?", tipo = TipoPergunta.NUMERO, obrigatoria = true) }
        vm.salvarModelo(); advanceUntilIdle()
        val enviado = assertNotNull(p.modeloCriado)
        assertEquals("Esportivo", enviado.nome); assertEquals(pergunta.id, enviado.blocos[0].perguntas[0].id); assertTrue(enviado.blocos[0].perguntas[0].obrigatoria)
        assertNull(vm.estado.value.editor); assertEquals(MensagemAnamnese.MODELO_SALVO, vm.estado.value.aviso); assertTrue(vm.estado.value.gerenciandoModelos)
    }

    @Test fun editaModeloExistenteComAVersaoQueTem() = runTest {
        val m = modelo("Antigo", false); val p = PortaAnamneseFalsa(listOf(m)); val vm = vm(p); vm.abrir(paciente); advanceUntilIdle()
        vm.editarModelo(m.id); vm.mudarNome("Novo"); vm.salvarModelo(); advanceUntilIdle()
        val (versao, req) = assertNotNull(p.modeloSalvo); assertEquals(1L, versao); assertEquals("Novo", req.nome); assertEquals(m.id, req.id)
    }

    @Test fun conflitoDeVersaoRecarregaEFechaOEditor() = runTest {
        val m = modelo("Antigo", false); val p = PortaAnamneseFalsa(listOf(m)); val vm = vm(p); vm.abrir(paciente); advanceUntilIdle()
        vm.editarModelo(m.id); p.falha = erro("CONFLITO_VERSAO"); vm.salvarModelo(); advanceUntilIdle()
        assertNull(vm.estado.value.editor); assertEquals(MensagemAnamnese.CONFLITO, vm.estado.value.erro)
    }

    @Test fun modeloInvalidoDoServidorMantemOEditorAberto() = runTest {
        val p = PortaAnamneseFalsa(); val vm = vm(p); vm.abrir(paciente); advanceUntilIdle()
        vm.novoModelo(); vm.mudarNome("x"); vm.mudarBloco(0, "b"); vm.adicionarPergunta(0)
        vm.mudarPergunta(vm.estado.value.editor!!.blocos[0].perguntas[0].id) { it.copy(texto = "p") }
        p.falha = erro("FORMATO_INVALIDO"); vm.salvarModelo(); advanceUntilIdle()
        assertNotNull(vm.estado.value.editor); assertEquals(MensagemAnamnese.MODELO_INVALIDO, vm.estado.value.erro)
    }
}
