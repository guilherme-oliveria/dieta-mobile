package org.br.dietamais.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.br.dietamais.data.remoto.dto.Perfil
import org.br.dietamais.recursos.*
import org.br.dietamais.ui.comum.*
import org.jetbrains.compose.resources.stringResource

@Composable
fun EscolherPerfilTela(estado: EstadoAutenticacao, aoEscolher: (Perfil) -> Unit, aoVoltar: () -> Unit) {
    LayoutAutenticacao(stringResource(Res.string.auth_escolher_titulo), stringResource(Res.string.auth_escolher_descricao), estado) {
        estado.perfis.filter { it == Perfil.NUTRICIONISTA || it == Perfil.PACIENTE }.forEach { perfil ->
            BotaoContornado(stringResource(if (perfil == Perfil.NUTRICIONISTA) Res.string.auth_perfil_nutri else Res.string.auth_perfil_paciente),
                { aoEscolher(perfil) }, Modifier.fillMaxWidth(), carregando = estado.carregando)
        }
        BotaoTexto(stringResource(Res.string.auth_voltar), aoVoltar, Modifier.fillMaxWidth())
    }
}
