package org.br.dietamais.dominio.calculos

/** Sexo biológico usado pelas fórmulas da planilha. */
enum class Sexo { MASCULINO, FEMININO }

/** Jackson e Pollock: 3 ou 7 dobras (RN-CAL-03). */
enum class ProtocoloDobras { TRES, SETE }

/** Peso magro (peso − gordo) ou muscular (peso − gordo − ósseo − residual). */
enum class ProtocoloMassa { MAGRO, MUSCULAR }

/** Classificação do IMC da planilha: adulto (MG_I a OB_III) e idoso (MG, NO, SP). */
enum class ClasseImc { MG_III, MG_II, MG_I, MG, NO, SP, OB_I, OB_II, OB_III }

enum class RiscoComorbidades { B, ME, A, MO, S, MS }
enum class ClasseCintura { B, A, MA }
enum class Compleicao { PEQUENA, MEDIA, GRANDE }
enum class ClassePercentualGordura { MT_BXO, EXC, MT_BOM, BOM, ADEQ, MOD_ALTO, ALTO, MT_ALTO }

/** Motivos de resultado nulo (RN-CAL-04). */
object Motivo {
    const val FALTA_PUNHO = "FALTA_PUNHO"
    const val FALTA_CINTURA = "FALTA_CINTURA"
    const val FALTA_DOBRAS = "FALTA_DOBRAS"
    const val FALTA_DIAMETRO_PUNHO = "FALTA_DIAMETRO_PUNHO"
    const val FALTA_DIAMETRO_JOELHO = "FALTA_DIAMETRO_JOELHO"
    const val FALTA_PERCENTUAL_DESEJADO = "FALTA_PERCENTUAL_DESEJADO"
}

/** Dobras em mm; nulo = não medida. */
data class DobrasCalculo(
    val triceps: Double? = null, val peitoral: Double? = null, val axilarMedia: Double? = null, val subescapular: Double? = null,
    val abdominal: Double? = null, val suprailiaca: Double? = null, val coxa: Double? = null,
)

data class EntradaAvaliacaoAdulto(
    val sexo: Sexo, val idadeAnos: Int, val pesoKg: Double, val estaturaM: Double,
    val protocoloDobras: ProtocoloDobras, val protocoloMassa: ProtocoloMassa,
    val dobras: DobrasCalculo = DobrasCalculo(), val cinturaCm: Double? = null, val punhoCm: Double? = null,
    val diametroPunhoCm: Double? = null, val diametroJoelhoCm: Double? = null, val percentualDesejado: Double? = null,
)

/** Resultado da prévia local (RN-CAL-01: o valor gravado é o do backend). Kg e % com 2 casas; densidade com 4. */
data class ResultadoAvaliacaoAdulto(
    val imc: Double, val classificacaoImc: ClasseImc, val riscoComorbidades: RiscoComorbidades,
    val pesoTeoricoMinKg: Double, val pesoTeoricoMaxKg: Double, val compleicao: Compleicao?, val classificacaoCintura: ClasseCintura?,
    val somatorioDobrasMm: Double?, val densidadeCorporal: Double?, val percentualGordura: Double?,
    val classificacaoPercentualGordura: ClassePercentualGordura?, val percentualMagro: Double?, val pesoGordoKg: Double?,
    val pesoOsseoKg: Double?, val pesoResidualKg: Double, val pesoMagroKg: Double?, val pesoParaPercentualDesejadoKg: Double?,
    val textoResumo: String?, val motivosNulos: Map<String, String>,
)
