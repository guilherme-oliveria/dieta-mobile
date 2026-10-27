package org.br.dietamais.data.sync

import org.br.dietamais.data.local.BancoDieta
import org.br.dietamais.data.remoto.ApiDieta
import org.br.dietamais.data.remoto.RespostaInvalida
import org.br.dietamais.data.remoto.dto.SyncResposta

class FonteLeituraHttp(private val api: ApiDieta, private val limite: Int = 500) : FonteLeituraSync {
    init { require(limite in 1..1000) }
    override suspend fun puxar(cursor: String?): SyncResposta {
        val resposta = api.obter<SyncResposta>("sync", buildMap {
            cursor?.let { put("cursor", it) }
            put("limite", limite.toString())
        })
        if (resposta.mudancas.pacientes.size + resposta.mudancas.vinculos.size + resposta.mudancas.nutris.size > limite) throw RespostaInvalida()
        return resposta
    }
}

fun ApiDieta.contextoLeituraSync(): ContextoLeituraSync? = operacaoSessao.value?.let {
    ContextoLeituraSync(it.contexto.usuarioId, it.contexto.perfil, it.id.toString())
}

fun criarSincronizador(api: ApiDieta, banco: BancoDieta): Sincronizador = Sincronizador(
    FonteLeituraHttp(api), ArmazemLeituraSql(banco, api::contextoLeituraSync), api::contextoLeituraSync)
