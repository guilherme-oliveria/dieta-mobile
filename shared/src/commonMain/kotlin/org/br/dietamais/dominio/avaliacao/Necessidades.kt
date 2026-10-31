package org.br.dietamais.dominio.avaliacao

import kotlinx.serialization.json.JsonElement
import org.br.dietamais.data.remoto.dto.EnergiaAvaliacaoDto
import org.br.dietamais.data.remoto.dto.Sexo as SexoDto
import org.br.dietamais.dominio.calculos.*

/** Como o GET é calculado (BE-43b). `MET` precisa da rotina do dia: o app só guarda e devolve a rotina que já existe; quem calcula é o servidor. */
enum class MetodoEnergia { FAO_1985, FAO_2004, IOM_2005, RDI_2005, MET }

/** Condição fisiológica que muda as metas (só para mulher): gestante ou lactante (período da lactação). */
enum class CondicaoEnergia { NENHUMA, GESTANTE, LACTANTE_ATE_6_MESES, LACTANTE_7_A_12_MESES }

/** Passo "Necessidades" do formulário: tudo opcional, com o padrão do servidor (FAO 2004, faixas DRI e lipídio mínimo de 20%). */
data class EscolhasEnergia(
    val metodo: MetodoEnergia = MetodoEnergia.FAO_2004, val carboidrato: ProtocoloCarboidrato = ProtocoloCarboidrato.FAIXA_DRI,
    val proteina: ProtocoloProteina = ProtocoloProteina.FAIXA_DRI, val lipidio: ProtocoloLipidio = ProtocoloLipidio.MINIMO_20,
    val nivel: NivelAtividade? = null, val objetivo: ObjetivoPeso = ObjetivoPeso.MANUTENCAO, val kgPorMes: String = "",
    val condicao: CondicaoEnergia = CondicaoEnergia.NENHUMA, val dislipidemico: Boolean = false, val colesterolTotal: Double? = null,
    val rotinaMet: JsonElement? = null,
    /** A nutri mexeu em algum campo do passo: só então o servidor recebe `energia` (sem isso vale o padrão ao informar o nível). */
    val tocada: Boolean = false,
) {
    val lactacao: PeriodoLactacao? get() = when (condicao) {
        CondicaoEnergia.LACTANTE_ATE_6_MESES -> PeriodoLactacao.ATE_6_MESES
        CondicaoEnergia.LACTANTE_7_A_12_MESES -> PeriodoLactacao.DE_7_A_12_MESES
        else -> null
    }
    val gestante: Boolean get() = condicao == CondicaoEnergia.GESTANTE
}

/** Lê as escolhas gravadas; texto desconhecido cai no padrão (o contrato evolui antes do app). */
fun escolhasDe(energia: EnergiaAvaliacaoDto?, nivel: String?, objetivo: String?, kgPorMes: Double?): EscolhasEnergia {
    val condicao = when {
        energia?.lactacao == "ATE_6_MESES" -> CondicaoEnergia.LACTANTE_ATE_6_MESES
        energia?.lactacao == "DE_7_A_12_MESES" -> CondicaoEnergia.LACTANTE_7_A_12_MESES
        energia?.gestante == true -> CondicaoEnergia.GESTANTE
        else -> CondicaoEnergia.NENHUMA
    }
    return EscolhasEnergia(
        metodo = MetodoEnergia.entries.firstOrNull { it.name == energia?.metodo } ?: MetodoEnergia.FAO_2004,
        carboidrato = ProtocoloCarboidrato.entries.firstOrNull { it.name == energia?.carboidrato } ?: ProtocoloCarboidrato.FAIXA_DRI,
        proteina = ProtocoloProteina.entries.firstOrNull { it.name == energia?.proteina } ?: ProtocoloProteina.FAIXA_DRI,
        lipidio = ProtocoloLipidio.entries.firstOrNull { it.name == energia?.lipidio } ?: ProtocoloLipidio.MINIMO_20,
        nivel = NivelAtividade.entries.firstOrNull { it.name == nivel },
        objetivo = ObjetivoPeso.entries.firstOrNull { it.name == objetivo } ?: ObjetivoPeso.MANUTENCAO,
        kgPorMes = kgPorMes?.let { textoDecimal(it) }.orEmpty(), condicao = condicao, dislipidemico = energia?.dislipidemico == true,
        colesterolTotal = energia?.colesterolTotal, rotinaMet = energia?.rotinaMet, tocada = energia != null,
    )
}

/** O que sobe para o servidor em `energia`; nulo enquanto a nutri não escolheu nada nem informou o nível. */
fun energiaParaEnviar(e: EscolhasEnergia): EnergiaAvaliacaoDto? =
    if (!e.tocada && e.nivel == null) null
    else EnergiaAvaliacaoDto(metodo = e.metodo.name, gestante = e.gestante.takeIf { it }, lactacao = e.lactacao?.name, carboidrato = e.carboidrato.name,
        proteina = e.proteina.name, lipidio = e.lipidio.name, dislipidemico = e.dislipidemico.takeIf { it }, colesterolTotal = e.colesterolTotal,
        rotinaMet = e.rotinaMet.takeIf { e.metodo == MetodoEnergia.MET })

/** O que falta para a prévia de energia (RN-CAL-04): vira uma frase na tela, nunca um zero. */
enum class FaltaEnergia { PESO_ESTATURA, SEXO, NASCIMENTO, FAIXA_ETARIA, NIVEL_ATIVIDADE, METODO_MET }

data class PreviaEnergia(
    val gebKcal: Double? = null, val getKcal: Double? = null, val vetKcal: Double? = null, val kcalPorKg: Double? = null,
    val carboidrato: MetaMacro? = null, val proteina: MetaMacro? = null, val lipidio: MetaMacro? = null,
    val lipidios: Lipidios? = null, val colesterolMaximoMg: Double? = null, val faltas: List<FaltaEnergia> = emptyList(),
)

/** Prévia local do GET, do VET e das metas enquanto a nutri escolhe (offline); o valor gravado é o do servidor (RN-CAL-01). */
fun previaEnergia(valores: Map<MedidaAvaliacao, String>, sexo: SexoDto?, idadeAnos: Int?, previa: PreviaAvaliacao, e: EscolhasEnergia): PreviaEnergia {
    fun v(m: MedidaAvaliacao) = lerDecimal(valores[m].orEmpty())?.takeIf { it in m.min..m.max }
    val peso = v(MedidaAvaliacao.PESO); val estatura = v(MedidaAvaliacao.ESTATURA)
    if (peso == null || estatura == null) return PreviaEnergia(faltas = listOf(FaltaEnergia.PESO_ESTATURA))
    val faltas = ArrayList<FaltaEnergia>()
    val sexoCalculo = when (sexo) { SexoDto.MASCULINO -> Sexo.MASCULINO; SexoDto.FEMININO -> Sexo.FEMININO; else -> null }
    if (sexoCalculo == null) faltas += FaltaEnergia.SEXO
    if (idadeAnos == null) faltas += FaltaEnergia.NASCIMENTO else if (idadeAnos < 18) faltas += FaltaEnergia.FAIXA_ETARIA
    if (faltas.isNotEmpty() || sexoCalculo == null || idadeAnos == null) return PreviaEnergia(faltas = faltas)
    if (e.metodo == MetodoEnergia.MET) return PreviaEnergia(faltas = listOf(FaltaEnergia.METODO_MET))
    val mulher = sexoCalculo == Sexo.FEMININO
    val lactante = mulher && e.lactacao != null
    val gestante = mulher && e.gestante
    val classe = previa.resultado?.classificacaoImc
    val obeso = if (classe != null) classe in listOf(ClasseImc.SP, ClasseImc.OB_I, ClasseImc.OB_II, ClasseImc.OB_III) else imcDe(peso, estatura) >= 25.0
    val geb = when (e.metodo) {
        MetodoEnergia.FAO_1985 -> CalculosEnergia.gebFao1985(sexoCalculo, idadeAnos, peso)
        MetodoEnergia.FAO_2004 -> CalculosEnergia.gebFao2004(sexoCalculo, idadeAnos, peso)
        MetodoEnergia.IOM_2005 -> CalculosEnergia.gebIom2002(sexoCalculo, idadeAnos, peso, estatura, obeso)
        else -> null
    }
    val nivel = e.nivel ?: return PreviaEnergia(gebKcal = geb, faltas = listOf(FaltaEnergia.NIVEL_ATIVIDADE))
    val get = when (e.metodo) {
        MetodoEnergia.FAO_1985 -> CalculosEnergia.getFao1985(checkNotNull(geb), sexoCalculo, nivel)
        MetodoEnergia.FAO_2004 -> CalculosEnergia.getFao2004(checkNotNull(geb), nivel, lactante)
        MetodoEnergia.IOM_2005 -> CalculosEnergia.getIom2005(checkNotNull(geb), nivel)
        else -> CalculosEnergia.getRdi2005(sexoCalculo, idadeAnos, peso, estatura, nivel, e.lactacao.takeIf { lactante })
    }
    val vet = CalculosEnergia.vet(get, e.objetivo, lerDecimal(e.kgPorMes))
    return PreviaEnergia(gebKcal = geb, getKcal = get, vetKcal = vet, kcalPorKg = CalculosEnergia.kcalPorKg(vet, peso),
        carboidrato = CalculosEnergia.carboidrato(e.carboidrato, vet, peso, gestante, lactante),
        proteina = CalculosEnergia.proteina(e.proteina, vet, peso, sexoCalculo, gestante, lactante), lipidio = CalculosEnergia.lipidio(e.lipidio, vet),
        lipidios = if (e.dislipidemico) CalculosEnergia.lipidiosDislipidemico(vet) else null,
        colesterolMaximoMg = if (e.dislipidemico) CalculosEnergia.colesterolMaximoMg(e.colesterolTotal) else null)
}
