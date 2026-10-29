package org.br.dietamais.plataforma

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.js.Js
import org.br.dietamais.data.remoto.dto.Plataforma

// O fetch do navegador já manda e recebe cookies na mesma origem (credentials: same-origin, o padrão); por isso o site e a API
// ficam na mesma origem, também em desenvolvimento (o servidor de dev encaminha /api para o backend).
internal actual fun motorHttp(): HttpClientEngine = Js.create()
internal actual fun plataformaApi() = Plataforma.WEB
