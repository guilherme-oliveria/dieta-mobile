package org.br.dietamais.plataforma

import androidx.compose.runtime.Composable

/** Arquivo de imagem escolhido pelo usuário; o tipo e o limite de tamanho são conferidos por quem recebe. */
class ImagemEscolhida(val bytes: ByteArray)

/**
 * Devolve uma função que abre o seletor de arquivos do sistema e chama [aoEscolher] com a imagem escolhida (nada, se cancelar).
 * Nulo onde ainda não há seletor (web e iOS): a tela esconde a opção.
 */
@Composable
expect fun rememberSeletorImagem(aoEscolher: (ImagemEscolhida) -> Unit): (() -> Unit)?
