package org.br.dietamais.dominio.contas

import org.br.dietamais.data.remoto.dto.TermosUsoResposta

fun TermosUsoResposta.permiteCadastro(): Boolean = status == "PUBLICADO" && aprovado && publicado && cadastroHabilitado &&
    versao.isNotBlank() && conteudo.isNotBlank() && aprovadoEm != null && publicadoEm != null && aprovadoEm <= publicadoEm
