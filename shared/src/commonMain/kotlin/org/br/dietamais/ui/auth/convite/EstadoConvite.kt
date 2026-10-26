package org.br.dietamais.ui.auth.convite

import org.br.dietamais.data.remoto.dto.ConviteResposta
import org.br.dietamais.data.remoto.dto.TermosUsoResposta

enum class MensagemConvite {
    CODIGO_INVALIDO, INVALIDO, EXPIRADO, ACEITE_OBRIGATORIO, ACEITE_TERMOS_OBRIGATORIO, TERMOS_INDISPONIVEIS, CONSENTIMENTO_INDISPONIVEL,
    SEM_CONEXAO, TAXA_EXCEDIDA, ENTRAR_NECESSARIO, VINCULO_EXISTENTE, CAMPO_INVALIDO, OBRIGATORIO, EMAIL_INVALIDO, SENHA_CURTA,
    COFRE_INDISPONIVEL, ERRO_PADRAO,
}
enum class CampoCadastroPaciente { NOME, EMAIL, SENHA }

data class EstadoConvite(
    val visivel: Boolean = false, val codigo: String = "", val convite: ConviteResposta? = null,
    val aceitouConsentimento: Boolean = false, val cadastro: Boolean = false, val aguardandoLogin: Boolean = false,
    val nome: String = "", val email: String = "", val senha: String = "", val carregando: Boolean = false,
    val aceitando: Boolean = false, val concluido: Boolean = false, val erro: MensagemConvite? = null,
    val termosUso: TermosUsoResposta? = null, val aceitouTermosUso: Boolean = false,
    val errosCampos: Map<String, MensagemConvite> = emptyMap(),
) { override fun toString() = "EstadoConvite[conteúdo protegido]" }
