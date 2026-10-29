package org.br.dietamais.data.remoto

/**
 * O que a sessão guarda além do [org.br.dietamais.plataforma.CofreSessao]. No app é o banco local (tabela `sessao` e limpeza dos
 * caches no logout); na web não há banco e a implementação é [ArmazemSessaoMemoria].
 */
interface ArmazemSessao {
    fun salvar(usuarioId: String, perfil: String)
    /** Chamado quando a sessão acaba ou muda de dono: apaga a sessão e todo dado local do usuário anterior. */
    fun limpar()
}

/** Web: nada além do cofre em memória; o navegador não guarda dado de saúde. */
object ArmazemSessaoMemoria : ArmazemSessao {
    override fun salvar(usuarioId: String, perfil: String) = Unit
    override fun limpar() = Unit
}
