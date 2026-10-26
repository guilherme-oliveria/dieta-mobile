package org.br.dietamais.ui.auth.convite

import androidx.compose.runtime.Composable
import org.br.dietamais.recursos.*
import org.jetbrains.compose.resources.stringResource

@Composable
fun textoMensagemConvite(mensagem: MensagemConvite): String = stringResource(when (mensagem) {
    MensagemConvite.CODIGO_INVALIDO -> Res.string.convite_erro_codigo
    MensagemConvite.INVALIDO -> Res.string.convite_erro_invalido
    MensagemConvite.EXPIRADO -> Res.string.convite_erro_expirado
    MensagemConvite.ACEITE_OBRIGATORIO -> Res.string.convite_erro_aceite
    MensagemConvite.ACEITE_TERMOS_OBRIGATORIO -> Res.string.auth_erro_aceite
    MensagemConvite.TERMOS_INDISPONIVEIS -> Res.string.convite_cadastro_bloqueado
    MensagemConvite.CONSENTIMENTO_INDISPONIVEL -> Res.string.pacientes_consentimento_indisponivel
    MensagemConvite.SEM_CONEXAO -> Res.string.auth_erro_rede
    MensagemConvite.TAXA_EXCEDIDA -> Res.string.auth_erro_taxa
    MensagemConvite.ENTRAR_NECESSARIO -> Res.string.convite_erro_entrar
    MensagemConvite.VINCULO_EXISTENTE -> Res.string.convite_erro_vinculo
    MensagemConvite.CAMPO_INVALIDO -> Res.string.auth_erro_campo
    MensagemConvite.OBRIGATORIO -> Res.string.auth_erro_obrigatorio
    MensagemConvite.EMAIL_INVALIDO -> Res.string.auth_erro_email
    MensagemConvite.SENHA_CURTA -> Res.string.auth_erro_senha
    MensagemConvite.COFRE_INDISPONIVEL -> Res.string.auth_erro_cofre
    MensagemConvite.ERRO_PADRAO -> Res.string.auth_erro_generico
})
