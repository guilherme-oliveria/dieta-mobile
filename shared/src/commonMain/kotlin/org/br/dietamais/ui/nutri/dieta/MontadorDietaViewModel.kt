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
import org.br.dietamais.dominio.dieta.CalculadoraTotais
import org.br.dietamais.dominio.dieta.MacrosPorcao
import org.br.dietamais.dominio.dieta.TotaisCent
import kotlin.uuid.Uuid

class MontadorDietaViewModel(private val dietas: PortaDietas, private val alimentos: PortaAlimentos,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate, conectividade: Flow<Boolean>? = null) : ViewModel() {
    private val atual = MutableStateFlow(EstadoDieta())
    val estado = atual.asStateFlow()
    private var catalogo = CatalogoAlimentos()
    private var operacao: Job? = null
    init {
        viewModelScope.launch(dispatcher) { alimentos.observar().catch { }.collect { catalogo = it; recalcular() } }
        conectividade?.let { fluxo -> viewModelScope.launch(dispatcher) { fluxo.collect { online -> atual.update { it.copy(semConexao = !online) } } } }
    }

    // ---- lista de dietas do paciente
    fun abrirPaciente(pacienteId: Uuid) {
        if (atual.value.pacienteId == pacienteId) return
        operacao?.cancel()
        atual.value = EstadoDieta(pacienteId = pacienteId, semConexao = atual.value.semConexao); recalcular()
        recarregar()
    }
    fun recarregar() {
        val id = atual.value.pacienteId ?: return
        executar(carregar = true) { val itens = dietas.listar(id); atual.update { it.copy(dietas = itens, semConexao = false) } }
    }
    fun nova() {
        if (atual.value.pacienteId == null) return
        atual.update { it.copy(rascunho = RascunhoDieta(), erro = null, aviso = null, errosCampos = emptyMap()) }; recalcular()
    }
    fun abrir(dieta: DietaResposta) {
        val rascunho = RascunhoDieta(dieta.id, dieta.versao, dieta.status, dieta.titulo, dieta.orientacao.orEmpty(),
            dieta.refeicoes.sortedBy { it.ordem }.map { r -> RefeicaoEdicao(nome = r.nome, horario = r.horario.paraTexto(),
                itens = r.itens.map { ItemEdicao(alimentoId = it.alimentoId, porcoes = it.porcoes.paraTexto()) }) })
        atual.update { it.copy(rascunho = rascunho, erro = null, aviso = null, errosCampos = emptyMap(), seletorRefeicao = null) }; recalcular()
    }
    fun fechar() { atual.update { it.copy(rascunho = null, seletorRefeicao = null, confirmarPublicar = false, erro = null, errosCampos = emptyMap()) }; if (atual.value.pacienteId != null) recarregar() }

    // ---- edição do rascunho
    fun mudarTitulo(v: String) = editar { it.copy(titulo = v.take(150)) }
    fun mudarOrientacao(v: String) = editar { it.copy(orientacao = v.take(10_000)) }
    fun adicionarRefeicao() = editar { r -> if (r.refeicoes.size >= LIMITE_REFEICOES) r else r.copy(refeicoes = r.refeicoes + RefeicaoEdicao(horario = sugerirHorario(r.refeicoes))) }
    fun removerRefeicao(chave: Uuid) = editar { it.copy(refeicoes = it.refeicoes.filter { r -> r.chave != chave }) }
    fun moverRefeicao(chave: Uuid, delta: Int) = editar { r ->
        val i = r.refeicoes.indexOfFirst { it.chave == chave }; val j = i + delta
        if (i < 0 || j !in r.refeicoes.indices) r else r.copy(refeicoes = r.refeicoes.toMutableList().apply { add(j, removeAt(i)) })
    }
    fun mudarNomeRefeicao(chave: Uuid, v: String) = refeicao(chave) { it.copy(nome = v.take(150)) }
    fun mudarHorario(chave: Uuid, v: String) = refeicao(chave) { it.copy(horario = v.take(5)) }
    fun abrirSeletor(chave: Uuid?) { atual.update { it.copy(seletorRefeicao = chave, busca = "", grupoFiltro = null) } }
    fun mudarBusca(v: String) { atual.update { it.copy(busca = v.take(150)) } }
    fun mudarGrupoFiltro(id: Uuid?) { atual.update { it.copy(grupoFiltro = id) } }
    fun adicionarItem(chaveRefeicao: Uuid, alimentoId: Uuid) = refeicao(chaveRefeicao) { r ->
        if (r.itens.size >= LIMITE_ITENS) r else r.copy(itens = r.itens + ItemEdicao(alimentoId = alimentoId))
    }.also { atual.update { it.copy(seletorRefeicao = null) } }
    fun removerItem(chaveRefeicao: Uuid, item: Uuid) = refeicao(chaveRefeicao) { r -> r.copy(itens = r.itens.filter { it.chave != item }) }
    fun mudarPorcoes(chaveRefeicao: Uuid, item: Uuid, v: String) = refeicao(chaveRefeicao) { r ->
        r.copy(itens = r.itens.map { if (it.chave == item) it.copy(porcoes = v.take(12)) else it })
    }

    // ---- salvar, publicar, duplicar
    fun salvar() {
        if (!podeEnviar()) return
        val r = atual.value.rascunho?.takeIf { it.editavel } ?: return
        val pacienteId = atual.value.pacienteId ?: return
        val pedido = montar(r) ?: return
        executar {
            val resposta = if (r.versao == null) dietas.criar(pacienteId, pedido, r.chave) else dietas.salvar(r.dietaId, r.versao, pedido)
            atual.update { it.copy(rascunho = it.rascunho?.copy(dietaId = resposta.id, versao = resposta.versao, alterado = false), aviso = MensagemDieta.SALVA) }
            val lista = dietas.listar(pacienteId); atual.update { it.copy(dietas = lista) }
        }
    }
    fun pedirPublicacao() {
        val r = atual.value.rascunho ?: return
        if (!r.editavel || r.versao == null || r.alterado || !podeEnviar()) return
        atual.update { it.copy(confirmarPublicar = true, erro = null) }
    }
    fun cancelarPublicacao() { atual.update { it.copy(confirmarPublicar = false) } }
    fun confirmarPublicacao() {
        val r = atual.value.rascunho ?: return
        val versao = r.versao ?: return
        if (!r.editavel || !podeEnviar()) return
        val pacienteId = atual.value.pacienteId ?: return
        executar {
            dietas.publicar(r.dietaId, versao)
            val lista = dietas.listar(pacienteId)
            atual.update { it.copy(dietas = lista, rascunho = null, confirmarPublicar = false, aviso = MensagemDieta.PUBLICADA) }
        }
    }
    fun duplicar(dieta: DietaResposta) {
        if (!podeEnviar()) return
        val pacienteId = atual.value.pacienteId ?: return
        executar {
            dietas.duplicar(dieta.id)
            val lista = dietas.listar(pacienteId); atual.update { it.copy(dietas = lista, aviso = MensagemDieta.DUPLICADA) }
        }
    }

    // ---- internos
    private fun podeEnviar() = !atual.value.enviando && !atual.value.carregando && !atual.value.semConexao
    private fun editar(bloco: (RascunhoDieta) -> RascunhoDieta) {
        val r = atual.value.rascunho ?: return
        if (!r.editavel || atual.value.enviando) return
        atual.update { it.copy(rascunho = bloco(r).copy(alterado = true), errosCampos = emptyMap(), aviso = null, erro = null) }; recalcular()
    }
    private fun refeicao(chave: Uuid, bloco: (RefeicaoEdicao) -> RefeicaoEdicao) = editar { r -> r.copy(refeicoes = r.refeicoes.map { if (it.chave == chave) bloco(it) else it }) }
    private fun macros(alimentoId: Uuid): MacrosPorcao? {
        val alimento = catalogo.alimentos.firstOrNull { it.id == alimentoId } ?: return null
        val grupo = catalogo.grupos.firstOrNull { it.id == alimento.grupoId } ?: return null
        return MacrosPorcao(grupo.kcalPorPorcao, grupo.carboidratosPorPorcao, grupo.proteinasPorPorcao, grupo.gordurasPorPorcao, alimento.gramasPorPorcao)
    }
    private fun recalcular() = atual.update { estado ->
        val porRefeicao = estado.rascunho?.refeicoes.orEmpty().associate { r ->
            r.chave to CalculadoraTotais.somar(r.itens.mapNotNull { i -> porcoesValidas(i.porcoes)?.let { p -> macros(i.alimentoId)?.let { m -> CalculadoraTotais.item(m, p) } } })
        }
        estado.copy(grupos = catalogo.grupos, alimentos = catalogo.alimentos, totaisRefeicao = porRefeicao, totais = CalculadoraTotais.somar(porRefeicao.values))
    }
    private fun montar(r: RascunhoDieta): SalvarDietaRequisicao? {
        val erros = mutableMapOf<String, MensagemDieta>()
        if (r.titulo.isBlank()) erros["titulo"] = MensagemDieta.TITULO_OBRIGATORIO
        val refeicoes = r.refeicoes.mapIndexed { indice, ref ->
            if (ref.nome.isBlank()) erros["refeicao:${ref.chave}"] = MensagemDieta.NOME_REFEICAO_OBRIGATORIO
            val horario = horarioValido(ref.horario)
            if (horario == null) erros["horario:${ref.chave}"] = MensagemDieta.HORARIO_INVALIDO
            val itens = ref.itens.map { i ->
                val p = porcoesValidas(i.porcoes)
                if (p == null) erros["porcoes:${i.chave}"] = MensagemDieta.PORCOES_INVALIDAS
                ItemRefeicaoRequisicao(alimentoId = i.alimentoId, porcoes = p ?: 1.0)
            }
            RefeicaoRequisicao(nome = ref.nome.trim(), horario = horario ?: LocalTime(0, 0), ordem = indice, itens = itens)
        }
        atual.update { it.copy(errosCampos = erros) }
        if (erros.isNotEmpty()) return null
        return SalvarDietaRequisicao(id = if (r.versao == null) r.dietaId else null, titulo = r.titulo.trim(), orientacao = r.orientacao.trim().ifEmpty { null }, refeicoes = refeicoes)
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
            is FalhaComunicacao -> MensagemDieta.SEM_CONEXAO
            is ErroApi -> when (falha.codigo) {
                "CONFLITO_VERSAO" -> MensagemDieta.CONFLITO
                "ASSINATURA_INATIVA" -> MensagemDieta.ASSINATURA_INATIVA
                "RECURSO_NAO_ENCONTRADO" -> MensagemDieta.NAO_ENCONTRADA
                "DIETA_VAZIA" -> MensagemDieta.DIETA_VAZIA
                "DIETA_NAO_EDITAVEL" -> MensagemDieta.NAO_EDITAVEL
                "TAXA_EXCEDIDA" -> MensagemDieta.TAXA_EXCEDIDA
                "FORMATO_INVALIDO", "VALOR_FORA_DO_INTERVALO" -> MensagemDieta.LIMITE_EXCEDIDO
                else -> MensagemDieta.ERRO
            }
            else -> MensagemDieta.ERRO
        }
        atual.update { it.copy(erro = mensagem, confirmarPublicar = false, semConexao = it.semConexao || falha is FalhaComunicacao) }
    }

    companion object {
        const val LIMITE_REFEICOES = 20
        const val LIMITE_ITENS = 60
        /** "1,5" ou "1.5" com até 4 casas, maior que zero e menor que 1.000.000. */
        fun porcoesValidas(texto: String): Double? {
            val t = texto.trim().replace(',', '.')
            if (!Regex("\\d{1,6}(\\.\\d{1,4})?").matches(t)) return null
            return t.toDouble().takeIf { it > 0 && it < 1_000_000 }
        }
        fun horarioValido(texto: String): LocalTime? {
            val m = Regex("([01]?\\d|2[0-3]):([0-5]\\d)").matchEntire(texto.trim()) ?: return null
            return LocalTime(m.groupValues[1].toInt(), m.groupValues[2].toInt())
        }
        private fun sugerirHorario(refeicoes: List<RefeicaoEdicao>): String {
            val ultimo = refeicoes.lastOrNull()?.let { horarioValido(it.horario) } ?: return "07:00"
            return LocalTime(((ultimo.hour + 3) % 24), ultimo.minute).paraTexto()
        }
    }
}

internal fun LocalTime.paraTexto() = "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
internal fun Double.paraTexto() = toString().removeSuffix(".0").replace('.', ',')
