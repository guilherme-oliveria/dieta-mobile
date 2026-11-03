package org.br.dietamais.plataforma

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import org.br.dietamais.data.remoto.dto.Plataforma

internal actual fun motorHttp(): HttpClientEngine = OkHttp.create()
internal actual fun plataformaApi() = Plataforma.ANDROID
