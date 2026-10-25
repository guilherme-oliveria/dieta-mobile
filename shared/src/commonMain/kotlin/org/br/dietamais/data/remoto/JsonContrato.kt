package org.br.dietamais.data.remoto

import kotlinx.serialization.json.Json

val JsonContrato = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    explicitNulls = true
}
