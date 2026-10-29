package org.br.dietamais.data.online

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.encodeToJsonElement
import org.br.dietamais.data.remoto.*
import org.br.dietamais.data.remoto.dto.*
import org.br.dietamais.data.repositorio.DadosDietaPaciente
import org.br.dietamais.data.repositorio.PortaDietaPaciente
import kotlin.uuid.Uuid

/** Dietas recebidas pelo paciente direto da API (web): `GET /eu/dietas` ao abrir e a escolha da dieta em uso por `PUT`. */
class DietaPacienteOnline(private val api: ApiDieta) : PortaDietaPaciente {
    private class Dados(val dono: Uuid, val resposta: DietasPacienteResposta)
    private val guardado = MutableStateFlow<Dados?>(null)

    private val visivel: Flow<DadosDietaPaciente> = combine(api.sessao, guardado) { sessao, dados ->
        if (sessao != null && dados?.dono == sessao.usuarioId) dados.resposta.paraDados() else DadosDietaPaciente()
    }

    override fun observar(): Flow<DadosDietaPaciente> = flow {
        carregar()
        emitAll(visivel)
    }

    private suspend fun carregar() {
        val esperado = contexto()
        val resposta = api.obter<DietasPacienteResposta>("eu/dietas")
        if (api.operacaoSessao.value != esperado) throw SessaoAlterada()
        guardado.value = Dados(esperado.contexto.usuarioId, resposta)
    }

    override suspend fun selecionar(dietaId: Uuid, versao: Long): DietaEmUsoResposta {
        val esperado = contexto()
        val resposta = api.substituir<DietaEmUsoResposta>("eu/dieta-em-uso", JsonContrato.encodeToJsonElement(SelecionarDietaEmUsoRequisicao(dietaId)), versao)
        if (api.operacaoSessao.value != esperado) throw SessaoAlterada()
        guardado.value?.takeIf { it.dono == esperado.contexto.usuarioId }?.let {
            guardado.value = Dados(it.dono, it.resposta.copy(dietaEmUsoId = resposta.dietaId, versao = resposta.versao))
        }
        return resposta
    }

    private fun DietasPacienteResposta.paraDados() = DadosDietaPaciente(itens, dietaEmUsoId?.let { DietaEmUsoResposta(it, versao) })
    private fun contexto() = api.operacaoSessao.value?.takeIf { it.contexto.perfil == Perfil.PACIENTE } ?: throw SessaoAlterada()
}
