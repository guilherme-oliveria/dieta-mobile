package org.br.dietamais.data.repositorio

import org.br.dietamais.data.remoto.ApiDieta
import org.br.dietamais.data.remoto.dto.PorcoesGruposResposta

/** Porções por grupo da dieta (`GET /referencias/porcoes`, BE-47b): base do planejador por porções (MOB-54b). */
interface PortaPorcoes {
    suspend fun obter(): PorcoesGruposResposta
}

class PorcoesRepositorio(private val api: ApiDieta) : PortaPorcoes {
    override suspend fun obter(): PorcoesGruposResposta = api.obter("referencias/porcoes")
}
