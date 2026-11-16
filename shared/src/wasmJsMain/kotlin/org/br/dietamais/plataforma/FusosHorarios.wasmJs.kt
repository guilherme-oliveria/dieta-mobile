@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package org.br.dietamais.plataforma

/** Registra a base IANA no js-joda usado pelo kotlinx-datetime, antes de resolver fusos nomeados. */
@JsModule("@js-joda/timezone")
external object JsJodaTimeZoneModule

private val jsJodaTz = JsJodaTimeZoneModule
