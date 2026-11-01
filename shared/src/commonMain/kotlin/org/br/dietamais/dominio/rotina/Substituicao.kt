package org.br.dietamais.dominio.rotina

import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.DatePeriod
import org.br.dietamais.data.remoto.dto.AlimentoResposta
import org.br.dietamais.data.remoto.dto.ItemRefeicaoResposta
import org.br.dietamais.data.remoto.dto.ListaSubstituicaoResposta
import kotlin.math.round

/** Substituto com a quantidade que mantém as mesmas porções do item (RN-SUB-02). */
data class Substituto(val alimento: AlimentoResposta, val porcoes: Double, val gramas: Double)

/** RN-SUB-01: mesmo grupo e dentro da lista congelada na dieta; o próprio alimento não conta e inativos ficam de fora. */
fun substitutosDoItem(item: ItemRefeicaoResposta, lista: ListaSubstituicaoResposta): List<Substituto> =
    if (item.porcoes == null || item.grupoId == null) emptyList() else lista.alimentos.filter { it.grupoId == item.grupoId && it.id != item.alimentoId && it.ativo }
        .sortedBy { it.nome.lowercase() }
        .map { calcularSubstituto(item, it) }

fun calcularSubstituto(item: ItemRefeicaoResposta, substituto: AlimentoResposta) =
    (item.porcoes ?: 0.0).let { porcoes -> Substituto(substituto, porcoes, round(porcoes * substituto.gramasPorPorcao * 100 + 1e-6) / 100) }

/** Dias em que o check-in é aceito: hoje e até 3 dias para trás (RN-ROT-01). Mais recente primeiro. */
fun diasComCheckin(hoje: LocalDate): List<LocalDate> = (0..3).map { hoje.minus(DatePeriod(days = it)) }

/** Meta de água (RN-ROT-03): 35 ml por kg; sem peso conhecido usa 2 000 ml. */
fun metaAguaMl(pesoKg: Double?): Int = if (pesoKg == null || pesoKg <= 0) 2000 else round(pesoKg * 35).toInt()
