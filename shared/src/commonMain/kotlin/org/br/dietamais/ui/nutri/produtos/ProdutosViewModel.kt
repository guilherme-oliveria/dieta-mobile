package org.br.dietamais.ui.nutri.produtos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.br.dietamais.data.remoto.*
import org.br.dietamais.data.remoto.dto.*
import org.br.dietamais.data.repositorio.PortaProdutos
import org.br.dietamais.data.sync.novoIdLocal
import org.br.dietamais.ui.nutri.alimentos.MensagemAlimentos
import kotlin.uuid.Uuid

enum class CampoProduto { NOME, MARCA, CLASSIFICACAO, MEDIDA, MEDIDA_GRAMAS, ENERGIA, CARBOIDRATO, PROTEINA, LIPIDIO }

/** Chaves dos 4 nutrientes obrigatórios por 100 g (as mesmas de `/referencias/produtos`); as demais (micronutrientes) seguem como vieram. */
object NutrientesProduto { const val ENERGIA = "energia_kcal"; const val CARBOIDRATO = "carboidrato_g"; const val PROTEINA = "proteina_g"; const val LIPIDIO = "lipideos_g"
    val macros = setOf(ENERGIA, CARBOIDRATO, PROTEINA, LIPIDIO) }

data class FormularioProduto(val id: Uuid = novoIdLocal(), val versao: Long? = null, val nome: String = "", val marca: String = "", val classificacao: String = "",
    val medida: String = "", val medidaGramas: String = "", val energia: String = "", val carboidrato: String = "", val proteina: String = "", val lipidio: String = "",
    val ativo: Boolean = true, val chave: Uuid = novoIdLocal(), val extras: Map<String, Double> = emptyMap(), val demais: ProdutoResposta? = null)

data class EstadoProdutos(val lista: List<ProdutoResposta> = emptyList(), val busca: String = "", val ativo: Boolean = true, val total: Long = 0, val carregando: Boolean = false,
    val enviando: Boolean = false, val semConexao: Boolean = false, val formulario: FormularioProduto? = null, val desativar: ProdutoResposta? = null,
    val conflito: Boolean = false, val erro: MensagemAlimentos? = null, val aviso: MensagemAlimentos? = null, val errosCampos: Map<String, MensagemAlimentos> = emptyMap()) {
    val temMais get() = lista.size < total
    override fun toString() = "EstadoProdutos[conteúdo protegido]"
}

/**
 * Produtos e suplementos da nutri (MOB-70, RN-ALI-05): lista paginada com busca e ativos/inativos, cadastro e edição com `If-Match`, desativar sem apagar.
 * Tudo online; o servidor valida assinatura, dono e valores.
 */
class ProdutosViewModel(private val repositorio: PortaProdutos, private val dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
    conectividade: Flow<Boolean>? = null) : ViewModel() {
    private val atual = MutableStateFlow(EstadoProdutos())
    val estado = atual.asStateFlow()
    private var operacao: Job? = null
    private var geracao = 0L
    init { conectividade?.let { fluxo -> viewModelScope.launch(dispatcher) { fluxo.collect { online -> atual.update { it.copy(semConexao = !online) } } } } }

    fun carregar() { if (atual.value.lista.isEmpty() && !atual.value.carregando) recarregar() }
    fun recarregar() = listar(0)
    fun maisResultados() { val e = atual.value; if (e.temMais && !e.carregando) listar(e.lista.size / PAGINA) }
    fun mudarBusca(valor: String) { atual.update { it.copy(busca = valor.take(150)) }; recarregar() }
    fun mudarAtivo(valor: Boolean) { atual.update { it.copy(ativo = valor) }; recarregar() }

    private fun listar(pagina: Int, manterAviso: Boolean = false) {
        val e = atual.value
        val esperada = ++geracao
        operacao?.cancel()
        atual.update { it.copy(carregando = true, erro = null, aviso = if (manterAviso) it.aviso else null) }
        operacao = viewModelScope.launch(dispatcher) {
            try {
                val r = repositorio.listar(e.busca, e.ativo, pagina)
                if (geracao == esperada) atual.update { it.copy(lista = if (pagina == 0) r.itens else (it.lista + r.itens).distinctBy { p -> p.id }, total = r.total, semConexao = false) }
            } catch (c: CancellationException) { throw c }
            catch (f: Exception) { if (geracao == esperada) apresentar(f) }
            finally { if (geracao == esperada) atual.update { it.copy(carregando = false) } }
        }
    }

    fun novo() { if (!ocupado()) atual.update { it.copy(formulario = FormularioProduto(), conflito = false, erro = null, aviso = null, errosCampos = emptyMap(), desativar = null) } }
    fun editar(p: ProdutoResposta) {
        if (ocupado()) return
        fun n(c: String) = p.nutrientes[c]?.let { v -> texto(v) }.orEmpty()
        atual.update { it.copy(formulario = FormularioProduto(p.id, p.versao, p.nome, p.marca.orEmpty(), p.classificacao.orEmpty(), p.medidaCaseira.orEmpty(),
            p.medidaGramas?.let { v -> texto(v) }.orEmpty(), n(NutrientesProduto.ENERGIA), n(NutrientesProduto.CARBOIDRATO), n(NutrientesProduto.PROTEINA), n(NutrientesProduto.LIPIDIO),
            p.ativo, extras = p.nutrientes.filterKeys { k -> k !in NutrientesProduto.macros }, demais = p), conflito = false, erro = null, aviso = null, errosCampos = emptyMap(), desativar = null) }
    }
    fun fechar() { geracao++; operacao?.cancel(); atual.update { it.copy(formulario = null, desativar = null, conflito = false, erro = null, errosCampos = emptyMap(), enviando = false, carregando = false) } }
    fun mudarCampo(campo: CampoProduto, valor: String) {
        if (atual.value.enviando || atual.value.conflito) return
        atual.update { e -> e.copy(formulario = e.formulario?.let { f -> when (campo) {
            CampoProduto.NOME -> f.copy(nome = valor.take(151)); CampoProduto.MARCA -> f.copy(marca = valor.take(81))
            CampoProduto.CLASSIFICACAO -> f.copy(classificacao = valor.take(81)); CampoProduto.MEDIDA -> f.copy(medida = valor.take(61))
            CampoProduto.MEDIDA_GRAMAS -> f.copy(medidaGramas = valor.take(20)); CampoProduto.ENERGIA -> f.copy(energia = valor.take(20))
            CampoProduto.CARBOIDRATO -> f.copy(carboidrato = valor.take(20)); CampoProduto.PROTEINA -> f.copy(proteina = valor.take(20))
            CampoProduto.LIPIDIO -> f.copy(lipidio = valor.take(20))
        }.copy(chave = novoIdLocal()) }, errosCampos = emptyMap(), erro = null) }
    }

    fun salvar() {
        if (ocupado() || atual.value.conflito) return
        val f = atual.value.formulario ?: return
        val erros = mutableMapOf<String, MensagemAlimentos>()
        if (f.nome.isBlank()) erros["nome"] = MensagemAlimentos.OBRIGATORIO else if (f.nome.length > 150) erros["nome"] = MensagemAlimentos.CAMPO_INVALIDO
        val medidaG = f.medidaGramas.takeIf { it.isNotBlank() }?.let { v -> numero(v) }
        if (f.medidaGramas.isNotBlank() && (medidaG == null || medidaG <= 0)) erros["medidaGramas"] = MensagemAlimentos.POSITIVO
        val macros = listOf(NutrientesProduto.ENERGIA to f.energia, NutrientesProduto.CARBOIDRATO to f.carboidrato, NutrientesProduto.PROTEINA to f.proteina, NutrientesProduto.LIPIDIO to f.lipidio).associate { (k, v) ->
            if (v.isBlank()) erros[k] = MensagemAlimentos.OBRIGATORIO
            val n = numero(v)
            if (v.isNotBlank() && (n == null || n < 0)) erros[k] = MensagemAlimentos.CAMPO_INVALIDO
            k to (n ?: 0.0)
        }
        atual.update { it.copy(errosCampos = erros) }; if (erros.isNotEmpty()) return
        val nutrientes = f.extras + macros
        val esperada = geracao
        executar {
            fun t(v: String) = v.trim().ifEmpty { null }
            if (f.versao == null) repositorio.criar(CriarProdutoRequisicao(f.id, f.nome.trim(), t(f.marca), t(f.classificacao), f.demais?.apresentacao, f.demais?.embalagem, f.demais?.sabores,
                t(f.medida), medidaG, f.demais?.observacao, nutrientes), f.chave)
            else repositorio.alterar(f.id, f.versao, AtualizarProdutoRequisicao(f.nome.trim(), t(f.marca), t(f.classificacao), f.demais?.apresentacao, f.demais?.embalagem, f.demais?.sabores,
                t(f.medida), medidaG, f.demais?.observacao, nutrientes, f.ativo))
            if (geracao == esperada) { atual.update { it.copy(formulario = null, enviando = false, aviso = MensagemAlimentos.SALVO) }; listar(0, manterAviso = true) }
        }
    }
    fun pedirDesativacao(p: ProdutoResposta) { if (!ocupado() && p.ativo) atual.update { it.copy(desativar = p, erro = null) } }
    fun confirmarDesativacao() {
        if (ocupado() || atual.value.conflito) return
        val p = atual.value.desativar ?: return
        val esperada = geracao
        executar {
            repositorio.alterar(p.id, p.versao, AtualizarProdutoRequisicao(p.nome, p.marca, p.classificacao, p.apresentacao, p.embalagem, p.sabores, p.medidaCaseira,
                p.medidaGramas, p.observacao, p.nutrientes, false))
            if (geracao == esperada) { atual.update { it.copy(desativar = null, enviando = false, aviso = MensagemAlimentos.SALVO) }; listar(0, manterAviso = true) }
        }
    }
    fun descartarEAtualizar() { fechar(); recarregar() }

    private fun ocupado() = atual.value.enviando
    private fun executar(bloco: suspend () -> Unit) {
        val esperada = geracao
        atual.update { it.copy(enviando = true, erro = null, aviso = null) }
        operacao = viewModelScope.launch(dispatcher) {
            try { bloco() }
            catch (c: CancellationException) { throw c }
            catch (f: Exception) { if (geracao == esperada) apresentar(f) }
            finally { if (geracao == esperada) atual.update { it.copy(enviando = false) } }
        }
    }
    private fun apresentar(falha: Throwable) {
        val m = when (falha) {
            is FalhaComunicacao -> MensagemAlimentos.SEM_CONEXAO
            is ErroApi -> when (falha.codigo) {
                "CONFLITO_VERSAO" -> MensagemAlimentos.CONFLITO; "ASSINATURA_INATIVA" -> MensagemAlimentos.ASSINATURA_INATIVA
                "TAXA_EXCEDIDA" -> MensagemAlimentos.TAXA_EXCEDIDA; else -> MensagemAlimentos.ERRO
            }
            else -> MensagemAlimentos.ERRO
        }
        atual.update { it.copy(erro = m, conflito = it.conflito || m == MensagemAlimentos.CONFLITO, semConexao = it.semConexao || falha is FalhaComunicacao,
            errosCampos = (falha as? ErroApi)?.campos?.associate { c -> c.campo to if (c.codigo == "OBRIGATORIO") MensagemAlimentos.OBRIGATORIO else MensagemAlimentos.CAMPO_INVALIDO }.orEmpty()) }
    }
    companion object {
        const val PAGINA = org.br.dietamais.data.repositorio.ProdutosRepositorio.TAMANHO
        internal fun numero(v: String) = v.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() }
        internal fun texto(v: Double) = v.toString().removeSuffix(".0").replace('.', ',')
    }
}
