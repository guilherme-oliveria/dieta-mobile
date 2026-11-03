package org.br.dietamais.ui.comum

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.br.dietamais.data.remoto.ErroApi
import org.br.dietamais.data.remoto.FalhaComunicacao
import org.br.dietamais.data.remoto.SessaoAlterada
import org.br.dietamais.recursos.*
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

enum class MensagemPdf { SEM_CONEXAO, INDISPONIVEL, ERRO }

fun mensagemPdf(falha: Throwable): MensagemPdf? = when (falha) {
    is SessaoAlterada -> null
    is FalhaComunicacao -> MensagemPdf.SEM_CONEXAO
    is ErroApi -> if (falha.codigo == "RECURSO_NAO_ENCONTRADO") MensagemPdf.INDISPONIVEL else MensagemPdf.ERRO
    else -> MensagemPdf.ERRO
}

/**
 * Pede o link do PDF ao backend e o abre no navegador do sistema, que baixa e oferece compartilhar (MOB-33).
 * O link vale 10 minutos; o arquivo não fica guardado no aparelho pelo app.
 */
@Composable
fun BotaoPdf(rotulo: String, habilitado: Boolean, obterUrl: suspend () -> String, modifier: Modifier = Modifier) {
    val escopo = rememberCoroutineScope()
    val navegador = LocalUriHandler.current
    var gerando by remember { mutableStateOf(false) }
    var erro by remember { mutableStateOf<MensagemPdf?>(null) }
    Column(modifier) {
        BotaoTexto(rotulo, {
            if (gerando) return@BotaoTexto
            gerando = true; erro = null
            escopo.launch {
                try { navegador.openUri(obterUrl()) }
                catch (c: CancellationException) { throw c }
                catch (falha: Exception) { erro = mensagemPdf(falha) }
                finally { gerando = false }
            }
        }, habilitado = habilitado && !gerando, carregando = gerando)
        erro?.let { Text(stringResource(textoErroPdf(it)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
    }
}

private fun textoErroPdf(m: MensagemPdf): StringResource = when (m) {
    MensagemPdf.SEM_CONEXAO -> Res.string.pdf_erro_sem_conexao
    MensagemPdf.INDISPONIVEL -> Res.string.pdf_erro_indisponivel
    MensagemPdf.ERRO -> Res.string.pdf_erro_geral
}
