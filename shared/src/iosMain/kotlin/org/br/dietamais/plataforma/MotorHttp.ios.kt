package org.br.dietamais.plataforma

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin
import org.br.dietamais.data.remoto.dto.Plataforma

internal actual fun motorHttp(): HttpClientEngine = Darwin.create()
internal actual fun plataformaApi() = Plataforma.IOS
