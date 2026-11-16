package org.br.dietamais.dominio.pacientes

import org.br.dietamais.data.remoto.dto.PacienteResposta
import org.br.dietamais.data.remoto.dto.StatusVinculo

/** Letra do índice A-Z para nomes que não começam com letra (ART-P1-01). */
const val INICIAL_OUTROS = '#'

private fun semAcento(s: String) = s.lowercase().map {
    when (it) { 'á', 'à', 'â', 'ã', 'ä' -> 'a'; 'é', 'è', 'ê', 'ë' -> 'e'; 'í', 'ì', 'î', 'ï' -> 'i'; 'ó', 'ò', 'ô', 'õ', 'ö' -> 'o'; 'ú', 'ù', 'û', 'ü' -> 'u'; 'ç' -> 'c'; else -> it }
}.joinToString("")

/** Busca por nome (sem acento nem caixa) ou por telefone (só os dígitos), mais o filtro de status. Busca vazia mostra todos. */
fun filtrarPacientes(todos: List<PacienteResposta>, busca: String, filtro: StatusVinculo?): List<PacienteResposta> {
    val termo = semAcento(busca.trim())
    val digitos = busca.filter { it.isDigit() }
    return todos.filter { p ->
        (filtro == null || p.status == filtro) && (termo.isEmpty() || semAcento(p.nome).contains(termo) ||
            (digitos.length >= 3 && p.telefone.orEmpty().filter { it.isDigit() }.contains(digitos)))
    }
}

/** Inicial para o índice A-Z: a primeira letra do nome sem acento, em maiúscula; outro caractere vira [INICIAL_OUTROS]. */
fun inicialDoNome(nome: String): Char = semAcento(nome.trim()).firstOrNull()?.takeIf { it in 'a'..'z' }?.uppercaseChar() ?: INICIAL_OUTROS

data class GrupoPacientes(val inicial: Char, val pacientes: List<PacienteResposta>)

/** Ordena por nome sem acento e agrupa por inicial, com o grupo "#" por último. */
fun agruparPorInicial(lista: List<PacienteResposta>): List<GrupoPacientes> =
    lista.sortedBy { semAcento(it.nome.trim()) }.groupBy { inicialDoNome(it.nome) }
        .map { (i, ps) -> GrupoPacientes(i, ps) }.sortedWith(compareBy({ it.inicial == INICIAL_OUTROS }, { it.inicial }))
