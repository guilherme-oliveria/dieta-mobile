package org.br.dietamais

import java.util.Properties

private object VersaoBuild

internal fun versaoAplicacao(): String = checkNotNull(
    VersaoBuild::class.java.getResourceAsStream("/dietamais-versao.properties")
) { "Metadados de versão ausentes." }.use { stream ->
    checkNotNull(Properties().apply { load(stream) }.getProperty("versao")) { "Versão do app ausente." }
}
