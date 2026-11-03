package org.br.dietamais.plataforma

import kotlinx.datetime.TimeZone

actual fun idFusoDoAparelho(): String = TimeZone.currentSystemDefault().id
