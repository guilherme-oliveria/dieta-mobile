package org.br.dietamais.ui.nutri.dieta

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.datetime.LocalTime
import org.br.dietamais.data.remoto.*
import org.br.dietamais.data.remoto.dto.*
import org.br.dietamais.data.repositorio.*
import org.br.dietamais.data.sync.novoIdLocal
import org.br.dietamais.dominio.dieta.ItemPorcao
import org.br.dietamais.dominio.dieta.PlanejadorPorcoes
import org.br.dietamais.dominio.dieta.TotaisPorcoes
import kotlin.math.abs
import kotlin.uuid.Uuid

/**
 * Planejador por porções do grupo (MOB-54b, RN-DIE-10 a 12): a nutri escolhe a porção de cada grupo uma vez e monta as refeições por
 * quantidade de porções, com total ao vivo contra as metas. A conta aqui é só feedback; o backend recalcula e decide ao salvar.
 */
class PlanejadorPorcoesViewModel(private val dietas: PortaDietas, private val porcoes: PortaPorcoes,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate, conectividade: Flow<Boolean>? = null) : ViewModel() {
    private val atual = MutableStateFlow(EstadoPlanejador())
    val estado = atual.asStateFlow()
    private var operacao: Job? = null
    init {
        conectividade?.let { fluxo -> viewModelScope.launch(dispatcher) { fluxo.collect { online -> atual.update { it.copy(semConexao = !online) } } } }
    }

    fun abrirPaciente(pacienteId: Uuid) {
        if (atual.value.pacienteId == pacienteId && atual.value.referencia != null) return
        operacao?.cancel()
        atual.value = EstadoPlanejador(pacienteId = pacienteId, semConexao = atual.value.semConexao)
        carregarReferencia()
    }
    fun carregarReferencia() = executar(carregar = true) { val r = porcoes.obter(); atual.update { it.copy(referencia = r, semConexao = false) }; recalcular() }

    fun nova() {
        if (atual.value.pacienteId == null || atual.value.referencia == null) return
        val principal = RefeicaoPlano(nome = "", horario = "07:00")
        atual.update { it.copy(rascunho = RascunhoPlano(refeicoes = listOf(principal)), refeicaoAtiva = principal.id, erro = null, aviso = null) }; recalcular()
    }
    /** Só abre dieta montada por grupos; dieta com alimento do catálogo continua no montador por alimentos. */
    fun abrir(dieta: DietaResposta): Boolean {
        val itensDieta = dieta.refeicoes.flatMap { it.itens }
        if (itensDieta.any { it.alimentoId != null } || atual.value.referencia == null) { atual.update { it.copy(erro = MensagemPlanejador.SO_NO_MONTADOR) }; return false }
        val escolhas = dieta.porcoes.orEmpty().associate { it.grupoId to it.opcao }
        val refeicoes = dieta.refeicoes.sortedBy { it.ordem }.map { r ->
            RefeicaoPlano(r.id, r.nome, r.horario.paraTexto(), r.itens.mapNotNull { i ->
                when {
                    i.textoLivre != null -> ItemPlano.TextoLivre(texto = i.textoLivre)
                    i.aVontade == true && i.grupoId != null -> ItemPlano.AVontade(grupoId = i.grupoId)
                    i.grupoId != null && i.porcoes != null -> ItemPlano.DoGrupo(grupoId = i.grupoId, porcoes = i.porcoes)
                    else -> null
                }
            }, r.alternativaDe)
        }
        val rascunho = RascunhoPlano(dieta.id, dieta.versao, dieta.status, dieta.titulo, dieta.orientacao.orEmpty(), dieta.rodape.orEmpty(), dieta.metas, escolhas, refeicoes)
        atual.update { it.copy(rascunho = rascunho, refeicaoAtiva = refeicoes.firstOrNull { r -> !r.alternativa }?.id, erro = null, aviso = null) }; recalcular()
        return true
    }
    fun fechar() { atual.update { it.copy(rascunho = null, refeicaoAtiva = null, erro = null) }; recalcular() }

    // ---- cabeçalho e metas
    fun mudarTitulo(v: String) = editar { it.copy(titulo = v.take(150)) }
    fun mudarOrientacao(v: String) = editar { it.copy(orientacao = v.take(10_000)) }
    fun mudarRodape(v: String) = editar { it.copy(rodape = v.take(2_000)) }
    fun mudarMetas(metas: MetasDieta?) = editar { it.copy(metas = metas) }

    // ---- refeições
    fun adicionarRefeicao() = editar { r ->
        if (r.refeicoes.size >= LIMITE_REFEICOES) r else r.copy(refeicoes = r.refeicoes + RefeicaoPlano(horario = sugerirHorario(r.refeicoes))).also { ativar(it.refeicoes.last().id) }
    }
    /** RN-DIE-11: a alternativa pertence a uma refeição principal e herda nome e horário dela. */
    fun adicionarAlternativa(principalId: Uuid) = editar { r ->
        val base = r.refeicoes.firstOrNull { it.id == principalId && !it.alternativa } ?: return@editar r
        if (r.refeicoes.size >= LIMITE_REFEICOES) r else {
            val nova = RefeicaoPlano(nome = base.nome, horario = base.horario, alternativaDe = base.id)
            val fim = r.refeicoes.indexOfLast { it.id == base.id || it.alternativaDe == base.id }
            ativar(nova.id)
            r.copy(refeicoes = r.refeicoes.take(fim + 1) + nova + r.refeicoes.drop(fim + 1))
        }
    }
    fun removerRefeicao(id: Uuid) = editar { r ->
        val restantes = r.refeicoes.filter { it.id != id && it.alternativaDe != id }
        if (atual.value.refeicaoAtiva !in restantes.map { it.id }) ativar(restantes.firstOrNull()?.id)
        r.copy(refeicoes = restantes)
    }
    fun mudarNomeRefeicao(id: Uuid, v: String) = refeicao(id) { it.copy(nome = v.take(150)) }
    fun mudarHorario(id: Uuid, v: String) = refeicao(id) { it.copy(horario = v.take(5)) }
    fun selecionarRefeicao(id: Uuid) = ativar(id)

    // ---- itens
    /** RN-DIE-10: a primeira vez que o grupo entra no plano ganha a porção do guia alimentar (ou a primeira); depois a escolha vale para todos os itens. */
    fun adicionarGrupo(refeicaoId: Uuid, grupoId: Uuid) {
        if (!grupoExiste(grupoId)) return
        editar { r -> r.copy(escolhas = if (grupoId in r.escolhas) r.escolhas else r.escolhas + (grupoId to padrao(grupoId))) }
        adicionarItem(refeicaoId, ItemPlano.DoGrupo(grupoId = grupoId))
    }
    fun adicionarAVontade(refeicaoId: Uuid, grupoId: Uuid) { if (grupoExiste(grupoId)) adicionarItem(refeicaoId, ItemPlano.AVontade(grupoId = grupoId)) }
    fun adicionarTexto(refeicaoId: Uuid, texto: String) { texto.trim().take(200).takeIf { it.isNotEmpty() }?.let { adicionarItem(refeicaoId, ItemPlano.TextoLivre(texto = it)) } }
    fun escolherPorcao(grupoId: Uuid, opcao: Int) {
        if (referencia()?.grupos?.firstOrNull { it.grupoId == grupoId }?.opcoes?.any { it.opcao == opcao } == true) editar { it.copy(escolhas = it.escolhas + (grupoId to opcao)) }
    }
    /** Passos de 0,5 porção, de 0,5 a 99. */
    fun ajustarPorcoes(refeicaoId: Uuid, item: Uuid, passos: Int) = refeicao(refeicaoId) { r ->
        r.copy(itens = r.itens.map { i -> if (i.chave == item && i is ItemPlano.DoGrupo) i.copy(porcoes = (i.porcoes + passos * PASSO).coerceIn(PASSO, MAXIMO)) else i })
    }
    fun removerItem(refeicaoId: Uuid, item: Uuid) = refeicao(refeicaoId) { r -> r.copy(itens = r.itens.filter { it.chave != item }) }

    // ---- salvar
    fun salvar() {
        val a = atual.value
        if (a.enviando || a.carregando || a.semConexao) return
        val r = a.rascunho?.takeIf { it.editavel } ?: return
        val pacienteId = a.pacienteId ?: return
        val pedido = montar(r) ?: return
        executar {
            val resposta = if (r.versao == null) dietas.criar(pacienteId, pedido, r.chave) else dietas.salvar(r.dietaId, r.versao, pedido)
            atual.update { it.copy(rascunho = it.rascunho?.copy(dietaId = resposta.id, versao = resposta.versao, alterado = false), aviso = MensagemPlanejador.SALVA) }
        }
    }

    // ---- internos
    private fun referencia() = atual.value.referencia
    private fun padrao(grupoId: Uuid): Int {
        val opcoes = referencia()?.grupos?.firstOrNull { it.grupoId == grupoId }?.opcoes.orEmpty()
        return (opcoes.firstOrNull { it.guiaAlimentar } ?: opcoes.minByOrNull { it.opcao })?.opcao ?: 1
    }
    private fun ativar(id: Uuid?) { atual.update { it.copy(refeicaoAtiva = id) } }
    private fun grupoExiste(grupoId: Uuid) = referencia()?.grupos?.any { it.grupoId == grupoId } == true
    private fun adicionarItem(refeicaoId: Uuid, item: ItemPlano) = refeicao(refeicaoId) { r -> if (r.itens.size >= LIMITE_ITENS) r else r.copy(itens = r.itens + item) }
    private fun editar(bloco: (RascunhoPlano) -> RascunhoPlano) {
        val r = atual.value.rascunho ?: return
        if (!r.editavel || atual.value.enviando) return
        val novo = bloco(r).copy(alterado = true) // fora do update: o bloco pode mudar o estado (ativar) e o CAS repetiria sem fim
        atual.update { it.copy(rascunho = novo, aviso = null, erro = null) }; recalcular()
    }
    private fun refeicao(id: Uuid, bloco: (RefeicaoPlano) -> RefeicaoPlano) = editar { r -> r.copy(refeicoes = r.refeicoes.map { if (it.id == id) bloco(it) else it }) }

    private fun recalcular() = atual.update { e ->
        val r = e.rascunho ?: return@update e.copy(totaisRefeicao = emptyMap(), diferencas = emptyMap(), totalDia = ZERO, saldo = null)
        val opcoes = e.referencia?.grupos.orEmpty().associate { it.grupoId to it.opcoes.associateBy { o -> o.opcao } }
        val totais = r.refeicoes.associate { ref ->
            ref.id to PlanejadorPorcoes.somar(ref.itens.filterIsInstance<ItemPlano.DoGrupo>().mapNotNull { i ->
                r.escolhas[i.grupoId]?.let { op -> opcoes[i.grupoId]?.get(op) }?.let { ItemPorcao(it, i.porcoes) }
            })
        }
        // RN-DIE-11: o dia soma só as principais; a alternativa mostra a diferença para a principal.
        val dia = PlanejadorPorcoes.somar(r.refeicoes.filter { !it.alternativa }.flatMap { ref -> itensCalculo(ref, r, opcoes) })
        val diferencas = r.refeicoes.filter { it.alternativa }.mapNotNull { alt ->
            val principal = totais[alt.alternativaDe] ?: return@mapNotNull null; val t = totais.getValue(alt.id)
            alt.id to DiferencaPlano(PlanejadorPorcoes.arredondar(t.energia - principal.energia), PlanejadorPorcoes.arredondar(t.carboidratos - principal.carboidratos),
                PlanejadorPorcoes.arredondar(t.proteinas - principal.proteinas), PlanejadorPorcoes.arredondar(t.gorduras - principal.gorduras))
        }.toMap()
        e.copy(totaisRefeicao = totais, diferencas = diferencas, totalDia = dia, saldo = r.metas?.let { PlanejadorPorcoes.saldo(it, dia) })
    }
    private fun itensCalculo(ref: RefeicaoPlano, r: RascunhoPlano, opcoes: Map<Uuid, Map<Int, OpcaoPorcaoResposta>>) =
        ref.itens.filterIsInstance<ItemPlano.DoGrupo>().mapNotNull { i -> r.escolhas[i.grupoId]?.let { op -> opcoes[i.grupoId]?.get(op) }?.let { ItemPorcao(it, i.porcoes) } }

    private fun montar(r: RascunhoPlano): SalvarDietaRequisicao? {
        val falha = when {
            r.titulo.isBlank() -> MensagemPlanejador.TITULO_OBRIGATORIO
            r.refeicoes.none { !it.alternativa && it.itens.isNotEmpty() } -> MensagemPlanejador.DIETA_VAZIA
            r.refeicoes.any { it.nome.isBlank() } -> MensagemPlanejador.NOME_REFEICAO_OBRIGATORIO
            r.refeicoes.any { MontadorDietaViewModel.horarioValido(it.horario) == null } -> MensagemPlanejador.HORARIO_INVALIDO
            r.metas != null && !metasValidas(r.metas) -> MensagemPlanejador.METAS_INVALIDAS
            else -> null
        }
        if (falha != null) { atual.update { it.copy(erro = falha) }; return null }
        val refeicoes = r.refeicoes.mapIndexed { indice, ref ->
            RefeicaoRequisicao(id = ref.id, nome = ref.nome.trim(), horario = MontadorDietaViewModel.horarioValido(ref.horario)!!, ordem = indice,
                itens = ref.itens.map { i ->
                    when (i) {
                        is ItemPlano.DoGrupo -> ItemRefeicaoRequisicao(id = i.chave, grupoId = i.grupoId, porcoes = i.porcoes)
                        is ItemPlano.AVontade -> ItemRefeicaoRequisicao(id = i.chave, grupoId = i.grupoId, aVontade = true)
                        is ItemPlano.TextoLivre -> ItemRefeicaoRequisicao(id = i.chave, textoLivre = i.texto, aVontade = true)
                    }
                },
                tipo = if (ref.alternativa) "ALTERNATIVA" else null, alternativaDe = ref.alternativaDe)
        }
        val usados = r.refeicoes.flatMap { it.itens }.filterIsInstance<ItemPlano.DoGrupo>().map { it.grupoId }.toSet()
        return SalvarDietaRequisicao(id = if (r.versao == null) r.dietaId else null, titulo = r.titulo.trim(), orientacao = r.orientacao.trim().ifEmpty { null },
            refeicoes = refeicoes, porcoes = r.escolhas.filterKeys { it in usados }.map { PorcaoGrupo(it.key, it.value) }, metas = r.metas,
            rodape = r.rodape.trim().ifEmpty { null })
    }
    private fun executar(carregar: Boolean = false, bloco: suspend () -> Unit) {
        atual.update { it.copy(carregando = carregar, enviando = !carregar, erro = null, aviso = null) }
        operacao = viewModelScope.launch(dispatcher) {
            try { bloco() }
            catch (c: CancellationException) { throw c }
            catch (falha: Exception) { apresentar(falha) }
            finally { atual.update { it.copy(carregando = false, enviando = false) } }
        }
    }
    private fun apresentar(falha: Throwable) {
        val mensagem = when (falha) {
            is FalhaComunicacao -> MensagemPlanejador.SEM_CONEXAO
            is ErroApi -> when (falha.codigo) {
                "CONFLITO_VERSAO" -> MensagemPlanejador.CONFLITO
                "ASSINATURA_INATIVA" -> MensagemPlanejador.ASSINATURA_INATIVA
                "RECURSO_NAO_ENCONTRADO" -> MensagemPlanejador.NAO_ENCONTRADA
                "DIETA_VAZIA" -> MensagemPlanejador.DIETA_VAZIA
                "DIETA_NAO_EDITAVEL" -> MensagemPlanejador.NAO_EDITAVEL
                "TAXA_EXCEDIDA" -> MensagemPlanejador.TAXA_EXCEDIDA
                "FORMATO_INVALIDO", "VALOR_FORA_DO_INTERVALO", "ITEM_INVALIDO", "PORCAO_INVALIDA" -> MensagemPlanejador.LIMITE_EXCEDIDO
                else -> MensagemPlanejador.ERRO
            }
            else -> MensagemPlanejador.ERRO
        }
        atual.update { it.copy(erro = mensagem, semConexao = it.semConexao || falha is FalhaComunicacao) }
    }

    companion object {
        const val LIMITE_REFEICOES = 20
        const val LIMITE_ITENS = 60
        const val PASSO = 0.5
        const val MAXIMO = 99.0
        private val ZERO = TotaisPorcoes(0.0, 0.0, 0.0, 0.0, emptyMap())
        /** VET maior que zero e macros somando 100% (RN-DIE-10, metas). */
        fun metasValidas(m: MetasDieta) = m.vetKcal > 0 && listOf(m.carboidratoPct, m.proteinaPct, m.lipidioPct).all { it >= 0 } &&
            abs(m.carboidratoPct + m.proteinaPct + m.lipidioPct - 100.0) <= 0.01
        private fun sugerirHorario(refeicoes: List<RefeicaoPlano>): String {
            val ultimo = refeicoes.lastOrNull { !it.alternativa }?.let { MontadorDietaViewModel.horarioValido(it.horario) } ?: return "07:00"
            return LocalTime(((ultimo.hour + 3) % 24), ultimo.minute).paraTexto()
        }
    }
}
