package org.br.dietamais.dominio.pacientes

import org.br.dietamais.data.remoto.dto.CadastroAmpliadoDto
import org.br.dietamais.data.remoto.dto.EnderecoDto

/** Textos do formulário do cadastro ampliado (RN-VIN-08). Tudo opcional; `toString` não mostra o conteúdo (dado pessoal). */
data class FormularioCadastroAmpliado(
    val escolaridade: String = "", val estadoCivil: String = "", val profissao: String = "", val convenio: String = "", val indicacao: String = "", val motivoConsulta: String = "",
    val logradouro: String = "", val numero: String = "", val complemento: String = "", val bairro: String = "", val cidade: String = "", val uf: String = "", val cep: String = "",
) { override fun toString() = "FormularioCadastroAmpliado[conteúdo protegido]" }

enum class ErroCadastroAmpliado { UF, CEP }

/** Sugestões que o app oferece; o servidor não fixa os valores (RN-VIN-08). */
val SUGESTOES_ESCOLARIDADE = listOf("Fundamental", "Médio", "Superior", "Pós-graduação")
val SUGESTOES_ESTADO_CIVIL = listOf("Solteiro(a)", "Casado(a)", "União estável", "Divorciado(a)", "Viúvo(a)")

fun soDigitos(texto: String) = texto.filter { it in '0'..'9' }

/** `uf` com 2 letras e `cep` com 8 dígitos, quando preenchidos (o servidor normaliza do mesmo jeito). */
fun errosDoCadastroAmpliado(f: FormularioCadastroAmpliado): Set<ErroCadastroAmpliado> = buildSet {
    if (f.uf.isNotBlank() && !Regex("[A-Za-z]{2}").matches(f.uf.trim())) add(ErroCadastroAmpliado.UF)
    if (f.cep.isNotBlank() && soDigitos(f.cep).length != 8) add(ErroCadastroAmpliado.CEP)
}

/** Monta o DTO só com o que foi preenchido; tudo vazio vira null (o campo nem vai na requisição). */
fun montarCadastroAmpliado(f: FormularioCadastroAmpliado): CadastroAmpliadoDto? {
    fun String.v() = trim().takeIf { it.isNotEmpty() }
    val endereco = EnderecoDto(f.logradouro.v(), f.numero.v(), f.complemento.v(), f.bairro.v(), f.cidade.v(), f.uf.v()?.uppercase(), f.cep.v()?.let { soDigitos(it) })
        .takeIf { it.logradouro != null || it.numero != null || it.complemento != null || it.bairro != null || it.cidade != null || it.uf != null || it.cep != null }
    val dto = CadastroAmpliadoDto(f.escolaridade.v(), f.estadoCivil.v(), f.profissao.v(), endereco, f.convenio.v(), f.indicacao.v(), f.motivoConsulta.v())
    return dto.takeIf { it.escolaridade != null || it.estadoCivil != null || it.profissao != null || it.endereco != null || it.convenio != null || it.indicacao != null || it.motivoConsulta != null }
}

/** Linha do endereço para a ficha: "Rua A, 10 - ap 2 - Centro - Cidade/UF - CEP 01310-100"; null se não há nada. */
fun enderecoEmUmaLinha(e: EnderecoDto?): String? {
    if (e == null) return null
    val rua = listOfNotNull(e.logradouro, e.numero).joinToString(", ").takeIf { it.isNotEmpty() }
    val cidade = listOfNotNull(e.cidade, e.uf).joinToString("/").takeIf { it.isNotEmpty() }
    val cep = e.cep?.takeIf { it.length == 8 }?.let { "CEP ${it.substring(0, 5)}-${it.substring(5)}" } ?: e.cep?.let { "CEP $it" }
    return listOfNotNull(rua, e.complemento, e.bairro, cidade, cep).joinToString(" - ").takeIf { it.isNotEmpty() }
}
