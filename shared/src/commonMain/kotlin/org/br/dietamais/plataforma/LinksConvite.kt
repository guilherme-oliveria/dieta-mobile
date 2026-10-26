package org.br.dietamais.plataforma

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.br.dietamais.data.sync.novoIdLocal
import kotlin.uuid.Uuid

data class LinkConvitePendente(val id: Uuid, val link: String) {
    override fun toString() = "LinkConvitePendente[conteúdo protegido]"
}

object LinksConvite {
    private val atual = MutableStateFlow<LinkConvitePendente?>(null)
    val pendente = atual.asStateFlow()
    fun receber(link: String) {
        if (link.isNotBlank() && link.length <= 2048) atual.value = LinkConvitePendente(novoIdLocal(), link)
    }
    fun consumir(id: Uuid) { atual.update { if (it?.id == id) null else it } }
}

fun receberLinkConvite(link: String) = LinksConvite.receber(link)
