package org.br.dietamais.plataforma

import io.ktor.client.engine.HttpClientEngine
import org.br.dietamais.data.remoto.dto.Plataforma

internal expect fun motorHttp(): HttpClientEngine
internal expect fun plataformaApi(): Plataforma
