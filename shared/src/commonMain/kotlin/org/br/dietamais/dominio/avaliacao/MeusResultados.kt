package org.br.dietamais.dominio.avaliacao

import org.br.dietamais.data.remoto.dto.AvaliacaoResposta
import org.br.dietamais.data.remoto.dto.ClassificacaoImc
import org.br.dietamais.dominio.calculos.*
import kotlin.time.Instant

/** Semáforo da tela "Meus resultados": sempre vem com texto, nunca só cor. */
enum class Semaforo { BOM, ATENCAO, ALERTA }

enum class TipoIndicador { PESO_ALTURA, CINTURA, GORDURA, RISCO }

/** `classe` é o código do servidor já reconhecido; a tela escolhe a frase sem siglas. */
data class Indicador(val tipo: TipoIndicador, val semaforo: Semaforo, val imc: ClassificacaoImc? = null, val cintura: ClasseCintura? = null,
    val gordura: ClassePercentualGordura? = null, val risco: RiscoComorbidades? = null, val valor: Double? = null)

enum class ParteCorpo { GORDURA, MASSA_MAGRA, MUSCULO, OSSO, RESIDUAL }

data class MetaPeso(val percentualDesejado: Double, val pesoMetaKg: Double, val faltamKg: Double)

data class MeusResultados(
    val data: Instant, val pesoKg: Double, val imc: Double, val indicadores: List<Indicador>, val composicao: List<Pair<ParteCorpo, Double>>,
    val percentualGordura: Double?, val meta: MetaPeso?, val pesoSaudavel: ClosedFloatingPointRange<Double>?, val textoResumo: String?,
    /** (dias desde a primeira avaliação, peso) das avaliações compartilhadas. */
    val seriePeso: List<Pair<Double, Double>>,
)

private inline fun <reified E : Enum<E>> reconhecer(codigo: String?): E? = codigo?.let { c -> enumValues<E>().firstOrNull { it.name == c } }

fun semaforoImc(c: ClasseImc) = when (c) {
    ClasseImc.NO -> Semaforo.BOM
    ClasseImc.MG, ClasseImc.MG_I, ClasseImc.SP -> Semaforo.ATENCAO
    else -> Semaforo.ALERTA
}
fun semaforoGordura(c: ClassePercentualGordura) = when (c) {
    ClassePercentualGordura.EXC, ClassePercentualGordura.MT_BOM, ClassePercentualGordura.BOM, ClassePercentualGordura.ADEQ -> Semaforo.BOM
    ClassePercentualGordura.MT_BXO, ClassePercentualGordura.MOD_ALTO -> Semaforo.ATENCAO
    ClassePercentualGordura.ALTO, ClassePercentualGordura.MT_ALTO -> Semaforo.ALERTA
}
fun semaforoCintura(c: ClasseCintura) = when (c) { ClasseCintura.B -> Semaforo.BOM; ClasseCintura.A -> Semaforo.ATENCAO; ClasseCintura.MA -> Semaforo.ALERTA }
fun semaforoRisco(r: RiscoComorbidades) = when (r) {
    RiscoComorbidades.B -> Semaforo.BOM; RiscoComorbidades.ME, RiscoComorbidades.A -> Semaforo.ATENCAO
    RiscoComorbidades.MO, RiscoComorbidades.S, RiscoComorbidades.MS -> Semaforo.ALERTA
}
private fun semaforoImcDto(c: ClassificacaoImc) = when (c) {
    ClassificacaoImc.NORMAL -> Semaforo.BOM; ClassificacaoImc.SOBREPESO -> Semaforo.ATENCAO
    ClassificacaoImc.ABAIXO_DO_PESO -> Semaforo.ATENCAO; else -> Semaforo.ALERTA
}
private fun imcDto(c: ClasseImc) = when (c) {
    ClasseImc.NO -> ClassificacaoImc.NORMAL; ClasseImc.SP -> ClassificacaoImc.SOBREPESO
    ClasseImc.OB_I -> ClassificacaoImc.OBESIDADE_I; ClasseImc.OB_II -> ClassificacaoImc.OBESIDADE_II; ClasseImc.OB_III -> ClassificacaoImc.OBESIDADE_III
    else -> ClassificacaoImc.ABAIXO_DO_PESO
}

/**
 * Resumo para o paciente a partir das avaliações que a nutri compartilhou (o backend já tirou o resto, RN-RES-01). Código desconhecido some do
 * resumo em vez de quebrar. Sem avaliação, nulo. Funciona offline: só lê o que já está no banco.
 */
fun meusResultados(avaliacoes: List<AvaliacaoResposta>): MeusResultados? {
    val compartilhadas = avaliacoes.filter { it.compartilhada }.sortedBy { it.dataHora }
    val a = compartilhadas.lastOrNull() ?: return null
    val r = a.resultados
    val indicadores = ArrayList<Indicador>()
    val imcClasse = reconhecer<ClasseImc>(r?.classificacaoImc)
    indicadores += if (imcClasse != null) Indicador(TipoIndicador.PESO_ALTURA, semaforoImc(imcClasse), imc = imcDto(imcClasse), valor = a.imc)
    else Indicador(TipoIndicador.PESO_ALTURA, semaforoImcDto(a.classificacaoImc), imc = a.classificacaoImc, valor = a.imc)
    reconhecer<ClasseCintura>(r?.classificacaoCintura)?.let { indicadores += Indicador(TipoIndicador.CINTURA, semaforoCintura(it), cintura = it, valor = a.perimetros.cintura) }
    reconhecer<ClassePercentualGordura>(r?.classificacaoPercentualGordura)?.let { indicadores += Indicador(TipoIndicador.GORDURA, semaforoGordura(it), gordura = it, valor = r?.percentualGordura) }
    reconhecer<RiscoComorbidades>(r?.riscoComorbidades)?.let { indicadores += Indicador(TipoIndicador.RISCO, semaforoRisco(it), risco = it) }

    val gordo = r?.pesoGordoKg; val magro = r?.pesoMagroKg
    val composicao = when {
        gordo == null || magro == null -> emptyList()
        r.pesoOsseoKg != null && r.pesoResidualKg != null && a.protocolo?.massa == "MUSCULAR" ->
            listOf(ParteCorpo.GORDURA to gordo, ParteCorpo.MUSCULO to magro, ParteCorpo.OSSO to r.pesoOsseoKg, ParteCorpo.RESIDUAL to r.pesoResidualKg)
        else -> listOf(ParteCorpo.GORDURA to gordo, ParteCorpo.MASSA_MAGRA to magro)
    }
    val alvo = a.complementares?.percentualGorduraDesejado; val pesoMeta = r?.pesoParaPercentualDesejadoKg
    val meta = if (alvo != null && pesoMeta != null) MetaPeso(alvo, pesoMeta, CalculosAdulto.arredondar(a.pesoKg - pesoMeta)) else null
    val saudavel = if (r?.pesoTeoricoMinKg != null && r.pesoTeoricoMaxKg != null) r.pesoTeoricoMinKg..r.pesoTeoricoMaxKg else null
    val inicio = compartilhadas.first().dataHora
    val serie = compartilhadas.map { (it.dataHora - inicio).inWholeDays.toDouble() to it.pesoKg }
    return MeusResultados(a.dataHora, a.pesoKg, a.imc, indicadores, composicao, r?.percentualGordura, meta, saudavel, r?.textoResumo?.takeIf { it.isNotBlank() }, serie)
}
