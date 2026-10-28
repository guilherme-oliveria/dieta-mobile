package org.br.dietamais.dominio.avaliacao

import org.br.dietamais.data.remoto.dto.*
import kotlin.math.round
import kotlin.time.Instant

enum class SecaoAvaliacao { BASICAS, PERIMETROS, DOBRAS, COMPOSICAO }

/** Medidas editáveis da avaliação, com os limites de sanidade da RN-AVA-03 (o backend repete a validação). */
enum class MedidaAvaliacao(val secao: SecaoAvaliacao, val min: Double, val max: Double, val obrigatoria: Boolean = false, val ler: (AvaliacaoResposta) -> Double?) {
    PESO(SecaoAvaliacao.BASICAS, 20.0, 400.0, true, { it.pesoKg }),
    ESTATURA(SecaoAvaliacao.BASICAS, 0.5, 2.5, true, { it.estaturaM }),
    PESCOCO(SecaoAvaliacao.PERIMETROS, 10.0, 250.0, ler = { it.perimetros.pescoco }),
    TORAX(SecaoAvaliacao.PERIMETROS, 10.0, 250.0, ler = { it.perimetros.torax }),
    CINTURA(SecaoAvaliacao.PERIMETROS, 10.0, 250.0, ler = { it.perimetros.cintura }),
    ABDOMEN(SecaoAvaliacao.PERIMETROS, 10.0, 250.0, ler = { it.perimetros.abdomen }),
    QUADRIL(SecaoAvaliacao.PERIMETROS, 10.0, 250.0, ler = { it.perimetros.quadril }),
    BRACO(SecaoAvaliacao.PERIMETROS, 10.0, 250.0, ler = { it.perimetros.braco }),
    ANTEBRACO(SecaoAvaliacao.PERIMETROS, 10.0, 250.0, ler = { it.perimetros.antebraco }),
    COXA(SecaoAvaliacao.PERIMETROS, 10.0, 250.0, ler = { it.perimetros.coxa }),
    PANTURRILHA(SecaoAvaliacao.PERIMETROS, 10.0, 250.0, ler = { it.perimetros.panturrilha }),
    DOBRA_TRICEPS(SecaoAvaliacao.DOBRAS, 1.0, 80.0, ler = { it.dobras.triceps }),
    DOBRA_SUBESCAPULAR(SecaoAvaliacao.DOBRAS, 1.0, 80.0, ler = { it.dobras.subescapular }),
    DOBRA_PEITORAL(SecaoAvaliacao.DOBRAS, 1.0, 80.0, ler = { it.dobras.peitoral }),
    DOBRA_AXILAR_MEDIA(SecaoAvaliacao.DOBRAS, 1.0, 80.0, ler = { it.dobras.axilarMedia }),
    DOBRA_SUPRAILIACA(SecaoAvaliacao.DOBRAS, 1.0, 80.0, ler = { it.dobras.suprailiaca }),
    DOBRA_ABDOMINAL(SecaoAvaliacao.DOBRAS, 1.0, 80.0, ler = { it.dobras.abdominal }),
    DOBRA_COXA(SecaoAvaliacao.DOBRAS, 1.0, 80.0, ler = { it.dobras.coxa }),
    PERCENTUAL_GORDURA(SecaoAvaliacao.COMPOSICAO, 0.0, 100.0, ler = { it.composicao.percentualGordura }),
    PESO_GORDO(SecaoAvaliacao.COMPOSICAO, 0.0, 400.0, ler = { it.composicao.pesoGordoKg }),
    PESO_MAGRO(SecaoAvaliacao.COMPOSICAO, 0.0, 400.0, ler = { it.composicao.pesoMagroKg }),
}

enum class ErroMedida { OBRIGATORIA, INVALIDA, FORA_DO_INTERVALO, CASAS_DECIMAIS }

/** Aceita vírgula ou ponto como separador decimal; vazio vira nulo. */
fun lerDecimal(texto: String): Double? = texto.trim().replace(',', '.').takeIf { it.isNotEmpty() }?.toDoubleOrNull()?.takeIf { it.isFinite() }

fun validarMedidas(valores: Map<MedidaAvaliacao, String>): Map<MedidaAvaliacao, ErroMedida> {
    val erros = LinkedHashMap<MedidaAvaliacao, ErroMedida>()
    for (medida in MedidaAvaliacao.entries) {
        val texto = valores[medida].orEmpty().trim()
        if (texto.isEmpty()) { if (medida.obrigatoria) erros[medida] = ErroMedida.OBRIGATORIA; continue }
        val numero = lerDecimal(texto)
        when {
            numero == null -> erros[medida] = ErroMedida.INVALIDA
            temMaisDeDuasCasas(texto) -> erros[medida] = ErroMedida.CASAS_DECIMAIS
            numero < medida.min || numero > medida.max -> erros[medida] = ErroMedida.FORA_DO_INTERVALO
        }
    }
    return erros
}

private fun temMaisDeDuasCasas(texto: String): Boolean {
    val fracao = texto.trim().replace(',', '.').substringAfter('.', "").trimEnd('0')
    return fracao.length > 2
}

fun montarRequisicao(valores: Map<MedidaAvaliacao, String>, dataHora: Instant, parecer: String, compartilhada: Boolean,
    justificativa: String? = null, id: kotlin.uuid.Uuid? = null): SalvarAvaliacaoRequisicao {
    fun v(m: MedidaAvaliacao) = lerDecimal(valores[m].orEmpty())
    return SalvarAvaliacaoRequisicao(
        id = id, dataHora = dataHora, pesoKg = checkNotNull(v(MedidaAvaliacao.PESO)), estaturaM = checkNotNull(v(MedidaAvaliacao.ESTATURA)),
        perimetros = PerimetrosAvaliacao(v(MedidaAvaliacao.PESCOCO), v(MedidaAvaliacao.TORAX), v(MedidaAvaliacao.CINTURA), v(MedidaAvaliacao.ABDOMEN),
            v(MedidaAvaliacao.QUADRIL), v(MedidaAvaliacao.BRACO), v(MedidaAvaliacao.ANTEBRACO), v(MedidaAvaliacao.COXA), v(MedidaAvaliacao.PANTURRILHA)),
        dobras = DobrasAvaliacao(v(MedidaAvaliacao.DOBRA_TRICEPS), v(MedidaAvaliacao.DOBRA_SUBESCAPULAR), v(MedidaAvaliacao.DOBRA_PEITORAL),
            v(MedidaAvaliacao.DOBRA_AXILAR_MEDIA), v(MedidaAvaliacao.DOBRA_SUPRAILIACA), v(MedidaAvaliacao.DOBRA_ABDOMINAL), v(MedidaAvaliacao.DOBRA_COXA)),
        composicao = ComposicaoAvaliacao(v(MedidaAvaliacao.PERCENTUAL_GORDURA), v(MedidaAvaliacao.PESO_GORDO), v(MedidaAvaliacao.PESO_MAGRO)),
        parecer = parecer.trim().ifEmpty { null }, compartilhada = compartilhada, justificativa = justificativa?.trim()?.ifEmpty { null })
}

fun valoresDe(avaliacao: AvaliacaoResposta): Map<MedidaAvaliacao, String> = MedidaAvaliacao.entries.mapNotNull { m ->
    m.ler(avaliacao)?.let { m to textoDecimal(it) }
}.toMap()

/** Mostra até 2 casas, sem zeros à direita, com vírgula (pt-BR). */
fun textoDecimal(valor: Double): String {
    val arredondado = round(valor * 100) / 100
    val inteiro = arredondado.toLong()
    if (arredondado == inteiro.toDouble()) return inteiro.toString()
    val centavos = round(kotlin.math.abs(arredondado - inteiro) * 100).toInt()
    val sinal = if (arredondado < 0 && inteiro == 0L) "-" else ""
    val fracao = centavos.toString().padStart(2, '0').trimEnd('0')
    return "$sinal$inteiro,$fracao"
}

/** Prévia do IMC enquanto a nutri digita; o valor oficial vem do backend ao salvar (RN-AVA-02). */
fun imcDe(pesoKg: Double, estaturaM: Double): Double = round(pesoKg / (estaturaM * estaturaM) * 100) / 100

fun classificarImc(imc: Double): ClassificacaoImc = when {
    imc < 18.5 -> ClassificacaoImc.ABAIXO_DO_PESO
    imc < 25.0 -> ClassificacaoImc.NORMAL
    imc < 30.0 -> ClassificacaoImc.SOBREPESO
    imc < 35.0 -> ClassificacaoImc.OBESIDADE_I
    imc < 40.0 -> ClassificacaoImc.OBESIDADE_II
    else -> ClassificacaoImc.OBESIDADE_III
}

/** Variação de uma medida entre duas avaliações; `delta` é depois − antes. Só entra o que existe nas duas. */
data class VariacaoAvaliacao(val medida: MedidaAvaliacao?, val antes: Double, val depois: Double) {
    val delta: Double get() = round((depois - antes) * 100) / 100
    val ehImc: Boolean get() = medida == null
}

/** Compara duas avaliações (a mais antiga primeiro): IMC no início, depois as medidas presentes nas duas. */
fun compararAvaliacoes(a: AvaliacaoResposta, b: AvaliacaoResposta): List<VariacaoAvaliacao> {
    val (antes, depois) = if (a.dataHora <= b.dataHora) a to b else b to a
    val lista = ArrayList<VariacaoAvaliacao>()
    lista += VariacaoAvaliacao(null, antes.imc, depois.imc)
    for (m in MedidaAvaliacao.entries) {
        val x = m.ler(antes); val y = m.ler(depois)
        if (x != null && y != null) lista += VariacaoAvaliacao(m, x, y)
    }
    return lista
}
