package org.br.dietamais.plataforma

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Web Share API quando existir; senão copia para a área de transferência. */
actual class Compartilhador : Compartilhamento {
    actual override suspend fun compartilhar(texto: String): ResultadoCompartilhamento =
        if (temShare()) executar(ResultadoCompartilhamento.ABERTO) { ok, erro -> share(texto, ok, erro) }
        else executar(ResultadoCompartilhamento.COPIADO) { ok, erro -> copiar(texto, ok, erro) }

    private suspend fun executar(resultado: ResultadoCompartilhamento, chamar: (() -> Unit, () -> Unit) -> Unit): ResultadoCompartilhamento =
        suspendCancellableCoroutine { continuacao ->
            chamar({ continuacao.resume(resultado) }, { continuacao.resumeWithException(CompartilhamentoIndisponivel()) })
        }
}

private fun temShare(): Boolean = js("typeof navigator.share === 'function'")
private fun share(texto: String, ok: () -> Unit, erro: () -> Unit): Unit =
    js("navigator.share({ text: texto }).then(function () { ok(); }, function () { erro(); })")
private fun copiar(texto: String, ok: () -> Unit, erro: () -> Unit): Unit =
    js("(navigator.clipboard ? navigator.clipboard.writeText(texto) : Promise.reject()).then(function () { ok(); }, function () { erro(); })")
