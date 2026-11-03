package org.br.dietamais.plataforma

import androidx.compose.runtime.Composable

/** Web: ainda sem seletor de arquivo (a opção da logo some); a logo se envia pelo app Android ou desktop. */
@Composable
actual fun rememberSeletorImagem(aoEscolher: (ImagemEscolhida) -> Unit): (() -> Unit)? = null
