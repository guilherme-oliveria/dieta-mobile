package org.br.dietamais.dominio.avaliacao

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.br.dietamais.data.remoto.dto.DadosInfantisAvaliacaoDto
import org.br.dietamais.data.remoto.dto.AvaliacaoResposta
import org.br.dietamais.data.remoto.dto.FaixaEtariaAvaliacao
import kotlin.math.round
import kotlin.time.Instant

data class FormularioInfantil(val pesoNascerKg: String = "", val modoCurva: String = "PERCENTIL",
    val etniaSlaughter: String? = null, val fatorAtividade: String = "")

enum class CampoInfantil { PESO_NASCER, FATOR_ATIVIDADE }

/** Mesmos meses completos e dia local usados pelo servidor, inclusive no aniversário. */
fun idadeAvaliacaoMeses(nascimento: LocalDate?, data: Instant): Int? {
    val dia = data.toLocalDateTime(TimeZone.of("America/Sao_Paulo")).date
    return idadeMesesCompletos(nascimento, dia)
}

fun idadeMesesCompletos(nascimento: LocalDate?, dia: LocalDate): Int? {
    nascimento ?: return null
    if (nascimento > dia) return null
    val meses = (dia.year - nascimento.year) * 12 + dia.month.ordinal - nascimento.month.ordinal
    return meses - if (dia.day < nascimento.day) 1 else 0
}

fun ehAvaliacaoInfantil(nascimento: LocalDate?, data: Instant): Boolean = idadeAvaliacaoMeses(nascimento, data)?.let { it < 216 } == true

fun validarFormularioInfantil(form: FormularioInfantil): Map<CampoInfantil, ErroMedida> = buildMap {
    fun validar(campo: CampoInfantil, texto: String, min: Double, max: Double) {
        if (texto.isBlank()) return
        val normal = texto.trim().replace(',', '.')
        val numero = lerDecimal(texto)
        val erro = when {
            numero == null || !normal.matches(Regex("[+-]?[0-9]+(?:\\.[0-9]+)?")) -> ErroMedida.INVALIDA
            normal.substringAfter('.', "").trimEnd('0').length > 3 -> ErroMedida.CASAS_DECIMAIS
            numero < min || numero > max -> ErroMedida.FORA_DO_INTERVALO
            else -> null
        }
        if (erro != null) put(campo, erro)
    }
    validar(CampoInfantil.PESO_NASCER, form.pesoNascerKg, 0.001, 20.0)
    validar(CampoInfantil.FATOR_ATIVIDADE, form.fatorAtividade, 0.001, 9.999)
}

/** Conversão de unidades; não calcula classificações clínicas. */
fun dadosInfantisParaEnviar(form: FormularioInfantil): DadosInfantisAvaliacaoDto {
    require(validarFormularioInfantil(form).isEmpty())
    return DadosInfantisAvaliacaoDto(pesoNascerGramas = lerDecimal(form.pesoNascerKg)?.let { round(it * 1000).toInt() },
        modoCurva = form.modoCurva, etniaSlaughter = form.etniaSlaughter, fatorAtividade = lerDecimal(form.fatorAtividade))
}

/** No formulário pediátrico, comprimento/estatura é digitado em cm com precisão de 0,1 cm. API recebe m. */
fun medidasInfantisParaServidor(valores: Map<MedidaAvaliacao, String>): Map<MedidaAvaliacao, String> {
    val cm = lerDecimal(valores[MedidaAvaliacao.ESTATURA].orEmpty()) ?: return valores
    return valores + (MedidaAvaliacao.ESTATURA to (round(cm * 10) / 1000).toString())
}

fun formularioInfantilDe(dados: DadosInfantisAvaliacaoDto?): FormularioInfantil {
    val gramas = dados?.pesoNascerGramas
    val peso = gramas?.let { g ->
        val fracao = (g % 1000).toString().padStart(3, '0').trimEnd('0')
        if (fracao.isEmpty()) (g / 1000).toString() else "${g / 1000},$fracao"
    }.orEmpty()
    return FormularioInfantil(peso, dados?.modoCurva ?: "PERCENTIL", dados?.etniaSlaughter,
        dados?.fatorAtividade?.toString()?.replace('.', ',').orEmpty())
}

fun valoresInfantisDe(avaliacao: AvaliacaoResposta): Map<MedidaAvaliacao, String> =
    valoresDe(avaliacao) + (MedidaAvaliacao.ESTATURA to textoDecimal(avaliacao.estaturaM * 100))

fun AvaliacaoResposta.ehPediatrica(): Boolean = when (faixaEtaria) {
    FaixaEtariaAvaliacao.CRIANCA, FaixaEtariaAvaliacao.ADOLESCENTE -> true
    FaixaEtariaAvaliacao.ADULTO, FaixaEtariaAvaliacao.IDOSO -> false
    else -> infantil != null || resultadosInfantis != null
}
