package org.br.dietamais.plataforma

import kotlinx.coroutines.flow.MutableStateFlow

internal val conexaoIos = MutableStateFlow(EstadoConexao.DESCONHECIDA)
fun atualizarConectividadeIos(disponivel: Boolean) {
    conexaoIos.value = if (disponivel) EstadoConexao.DISPONIVEL else EstadoConexao.INDISPONIVEL
}
