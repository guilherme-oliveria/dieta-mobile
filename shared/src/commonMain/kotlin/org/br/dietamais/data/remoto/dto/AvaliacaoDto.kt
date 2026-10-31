package org.br.dietamais.data.remoto.dto

import kotlinx.serialization.Serializable
import kotlin.time.Instant
import kotlin.uuid.Uuid

@Serializable(with = ClassificacaoImcSerializer::class)
enum class ClassificacaoImc { ABAIXO_DO_PESO, NORMAL, SOBREPESO, OBESIDADE_I, OBESIDADE_II, OBESIDADE_III, OUTRO }
object ClassificacaoImcSerializer : EnumComOutro<ClassificacaoImc>("ClassificacaoImc", ClassificacaoImc.entries.toTypedArray(), ClassificacaoImc.OUTRO)

@Serializable
data class PerimetrosAvaliacao(val pescoco: Double? = null, val torax: Double? = null, val cintura: Double? = null, val abdomen: Double? = null,
    val quadril: Double? = null, val braco: Double? = null, val antebraco: Double? = null, val coxa: Double? = null, val panturrilha: Double? = null)
@Serializable
data class DobrasAvaliacao(val triceps: Double? = null, val subescapular: Double? = null, val peitoral: Double? = null, val axilarMedia: Double? = null,
    val suprailiaca: Double? = null, val abdominal: Double? = null, val coxa: Double? = null)
@Serializable
data class ComposicaoAvaliacao(val percentualGordura: Double? = null, val pesoGordoKg: Double? = null, val pesoMagroKg: Double? = null)

@Serializable
data class AvaliacaoResposta(val id: Uuid, val pacienteId: Uuid, val vinculoId: Uuid, val dataHora: Instant, val pesoKg: Double, val estaturaM: Double,
    val imc: Double, val classificacaoImc: ClassificacaoImc, val perimetros: PerimetrosAvaliacao = PerimetrosAvaliacao(),
    val dobras: DobrasAvaliacao = DobrasAvaliacao(), val composicao: ComposicaoAvaliacao = ComposicaoAvaliacao(), val parecer: String? = null,
    val compartilhada: Boolean = true, val versao: Long, val criadoEm: Instant, val atualizadoEm: Instant,
    // Avaliação ampliada (BE-41): todos opcionais; o app antigo e o backend antigo continuam compatíveis.
    val faixaEtaria: FaixaEtariaAvaliacao? = null, val protocolo: ProtocoloAvaliacaoDto? = null,
    val complementares: MedidasComplementaresDto? = null, val resultados: ResultadosAvaliacaoDto? = null,
    val compartilhamento: CompartilhamentoAvaliacaoDto? = null)

@Serializable(with = FaixaEtariaAvaliacaoSerializer::class)
enum class FaixaEtariaAvaliacao { CRIANCA, ADOLESCENTE, ADULTO, IDOSO, OUTRO }
object FaixaEtariaAvaliacaoSerializer : EnumComOutro<FaixaEtariaAvaliacao>("FaixaEtariaAvaliacao", FaixaEtariaAvaliacao.entries.toTypedArray(), FaixaEtariaAvaliacao.OUTRO)

@Serializable
data class ProtocoloAvaliacaoDto(val dobras: String? = null, val massa: String? = null, val versaoReferencia: String? = null)

@Serializable
data class MedidasComplementaresDto(val pesoUsualKg: Double? = null, val pesoDesejadoKg: Double? = null, val punhoCm: Double? = null,
    val diametroPunhoCm: Double? = null, val diametroJoelhoCm: Double? = null, val percentualGorduraDesejado: Double? = null,
    val nivelAtividade: String? = null, val objetivo: String? = null, val objetivoKgPorMes: Double? = null, val menstruada: Boolean? = null)

/** Classificações chegam como texto e viram enum só no uso (`dominio/calculos`): valor desconhecido vira nulo, nunca falha (contrato evolui antes do app). */
@Serializable
data class ResultadosAvaliacaoDto(val idadeAnos: Int? = null, val classificacaoImc: String? = null, val riscoComorbidades: String? = null,
    val pesoTeoricoMinKg: Double? = null, val pesoTeoricoMaxKg: Double? = null, val compleicao: String? = null, val classificacaoCintura: String? = null,
    val somatorioDobrasMm: Double? = null, val densidadeCorporal: Double? = null, val percentualGordura: Double? = null,
    val classificacaoPercentualGordura: String? = null, val percentualMagro: Double? = null, val pesoGordoKg: Double? = null,
    val pesoOsseoKg: Double? = null, val pesoResidualKg: Double? = null, val pesoMagroKg: Double? = null,
    val pesoParaPercentualDesejadoKg: Double? = null, val textoResumo: String? = null, val motivosNulos: Map<String, String> = emptyMap())

/** `null` em uma chave = padrão (tudo visível, menos `exames`). O backend já filtra para o paciente; o app só reflete. */
@Serializable
data class CompartilhamentoAvaliacaoDto(val medidas: Boolean? = null, val composicao: Boolean? = null, val necessidades: Boolean? = null,
    val resumo: Boolean? = null, val exames: Boolean? = null)

@Serializable
data class AvaliacoesResposta(val itens: List<AvaliacaoResposta> = emptyList())

@Serializable
data class SalvarAvaliacaoRequisicao(val id: Uuid? = null, val dataHora: Instant, val pesoKg: Double, val estaturaM: Double,
    val perimetros: PerimetrosAvaliacao = PerimetrosAvaliacao(), val dobras: DobrasAvaliacao = DobrasAvaliacao(),
    val composicao: ComposicaoAvaliacao = ComposicaoAvaliacao(), val parecer: String? = null, val compartilhada: Boolean? = null,
    val justificativa: String? = null,
    // Avaliação ampliada (BE-41): opcionais; sem eles o servidor usa o padrão da nutri e o compartilhamento padrão.
    val protocolo: ProtocoloAvaliacaoDto? = null, val complementares: MedidasComplementaresDto? = null,
    val compartilhamento: CompartilhamentoAvaliacaoDto? = null)
