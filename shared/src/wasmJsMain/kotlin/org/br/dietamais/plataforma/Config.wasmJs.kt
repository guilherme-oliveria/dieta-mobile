package org.br.dietamais.plataforma

/**
 * Mesma origem do site (`/api/v1`), como previsto em web.md. Em desenvolvimento, `window.DIETA_URL_API` (definida no `index.html`
 * ou no console) aponta para a API em outra porta.
 */
internal actual fun urlApiPadrao(): String = urlApiConfigurada() ?: (origemDoSite() + "/api/v1")

private fun urlApiConfigurada(): String? = js("(typeof window.DIETA_URL_API === 'string' && window.DIETA_URL_API) ? window.DIETA_URL_API : null")
private fun origemDoSite(): String = js("window.location.origin")
