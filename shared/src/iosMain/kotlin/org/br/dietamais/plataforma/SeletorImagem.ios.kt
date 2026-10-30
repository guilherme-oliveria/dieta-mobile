package org.br.dietamais.plataforma

import androidx.compose.runtime.Composable

/** iOS: ainda sem seletor (UIDocumentPicker/PHPicker pedem validação no Mac); a opção da logo some. */
@Composable
actual fun rememberSeletorImagem(aoEscolher: (ImagemEscolhida) -> Unit): (() -> Unit)? = null
