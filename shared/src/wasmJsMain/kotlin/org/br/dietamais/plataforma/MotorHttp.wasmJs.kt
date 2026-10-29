package org.br.dietamais.plataforma

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.js.Js
import org.br.dietamais.data.remoto.dto.Plataforma

internal actual fun motorHttp(): HttpClientEngine = Js.create()
internal actual fun plataformaApi() = Plataforma.WEB
