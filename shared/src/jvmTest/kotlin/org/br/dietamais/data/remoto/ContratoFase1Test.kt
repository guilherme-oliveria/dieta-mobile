package org.br.dietamais.data.remoto

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.*
import org.br.dietamais.data.remoto.dto.*
import kotlin.test.*

class ContratoFase1Test {
    private val serializers: Map<String, KSerializer<*>> = mapOf(
        "CadastroNutriRequisicao" to CadastroNutriRequisicao.serializer(), "CadastroNutriResposta" to CadastroNutriResposta.serializer(),
        "LoginRequisicao" to LoginRequisicao.serializer(), "SessaoResposta" to SessaoResposta.serializer(),
        "RefreshRequisicao" to RefreshRequisicao.serializer(), "LogoutRequisicao" to LogoutRequisicao.serializer(),
        "ConfirmarEmailRequisicao" to ConfirmarEmailRequisicao.serializer(), "EsqueciSenhaRequisicao" to EsqueciSenhaRequisicao.serializer(),
        "RedefinirSenhaRequisicao" to RedefinirSenhaRequisicao.serializer(), "EuResposta" to EuResposta.serializer(),
        "PaginaPacientesResposta" to PaginaPacientesResposta.serializer(), "CriarPacienteRequisicao" to CriarPacienteRequisicao.serializer(),
        "AtualizarPacienteRequisicao" to AtualizarPacienteRequisicao.serializer(), "PacienteResposta" to PacienteResposta.serializer(),
        "ConviteCriadoResposta" to ConviteCriadoResposta.serializer(), "ConviteResposta" to ConviteResposta.serializer(),
        "AceitarConviteRequisicao" to AceitarConviteRequisicao.serializer(), "AceiteConviteResposta" to AceiteConviteResposta.serializer(),
        "VinculoResposta" to VinculoResposta.serializer(), "SyncResposta" to SyncResposta.serializer(), "ProblemaResposta" to ProblemaResposta.serializer(),
    )

    @Test
    fun desserializaTodosExemplosDoManifestoEToleraCampoNovo() {
        val manifesto = JsonContrato.parseToJsonElement(recursoContrato("manifesto.json")).jsonArray
        val arquivos = mutableSetOf<String>()
        manifesto.forEach { endpoint ->
            endpoint.jsonObject.getValue("exemplos").jsonArray.forEach { exemplo ->
                val arquivo = exemplo.jsonObject.getValue("arquivo").jsonPrimitive.content
                val tipo = exemplo.jsonObject.getValue("tipo").jsonPrimitive.content.substringAfterLast('.')
                val serializer = assertNotNull(serializers[tipo], tipo)
                val objeto = JsonContrato.parseToJsonElement(recursoContrato(arquivo)).jsonObject
                val original = JsonContrato.decodeFromJsonElement(serializer, objeto)
                val futuro = JsonObject(objeto + ("campoFuturo" to buildJsonObject { put("versao", 2) }))
                assertEquals(original, JsonContrato.decodeFromJsonElement(serializer, futuro), arquivo)
                arquivos += arquivo
            }
        }
        assertEquals(49, arquivos.size)
    }

    @Test
    fun enumsDesconhecidosUsamOutroSemAtribuirPerfilPermitido() {
        val resposta = recursoContrato("consultarEu/resposta.json").replace("NUTRICIONISTA", "PERFIL_FUTURO").replace("\"TESTE\"", "\"PLANO_FUTURO\"")
        val eu = JsonContrato.decodeFromString<EuResposta>(resposta)
        assertEquals(Perfil.OUTRO, eu.usuario.perfil)
        assertEquals(listOf(Perfil.OUTRO), eu.usuario.perfis)
        assertEquals(StatusAssinatura.OUTRO, eu.assinatura!!.status)
        val paciente = JsonContrato.decodeFromString<PacienteResposta>(recursoContrato("criarPaciente/resposta.json")
            .replace("CONVIDADO", "STATUS_FUTURO").replace("NAO_INFORMADO", "SEXO_FUTURO"))
        assertEquals(StatusVinculo.OUTRO, paciente.status)
        assertEquals(Sexo.OUTRO, paciente.sexo)
    }

    @Test
    fun escolhaDePerfilSemTokensECursorTombstoneRespeitamContrato() {
        val escolha = JsonContrato.decodeFromString<SessaoResposta>(recursoContrato("entrar/resposta-escolher-perfil.json"))
        assertNull(escolha.accessToken)
        assertNull(escolha.usuario.perfil)
        assertEquals(setOf(Perfil.NUTRICIONISTA, Perfil.PACIENTE), escolha.usuario.perfis.toSet())
        val sync = JsonContrato.decodeFromString<SyncResposta>(recursoContrato("puxarMudancas/resposta-exclusao.json"))
        val tombstone = sync.mudancas.pacientes.first { it.excluido }
        assertNull(tombstone.dados)
        assertTrue(sync.cursor.all(Char::isDigit))
    }
}
