package org.br.dietamais.ui.comum

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import org.br.dietamais.data.sync.*
import org.br.dietamais.plataforma.EstadoConexao
import org.br.dietamais.recursos.*
import org.br.dietamais.ui.theme.Dimens
import org.jetbrains.compose.resources.stringResource

@Composable
fun IndicadorLeitura(estado: EstadoLeituraSync, conexao: EstadoConexao, aoAtualizar: () -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Dimens.espacoMinimo)) {
        if (conexao == EstadoConexao.INDISPONIVEL || estado.resultado == ResultadoLeituraSync.SEM_REDE) FaixaOffline()
        if (estado.resultado == ResultadoLeituraSync.EM_ANDAMENTO) Text(stringResource(Res.string.sync_em_andamento),
            style = MaterialTheme.typography.bodySmall, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        if (estado.resultado == ResultadoLeituraSync.FALHOU) Text(stringResource(
            if (estado.codigoErro == "TAXA_EXCEDIDA") Res.string.auth_erro_taxa else Res.string.sync_erro),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        BotaoTexto(stringResource(Res.string.sync_atualizar), aoAtualizar, habilitado = estado.resultado != ResultadoLeituraSync.EM_ANDAMENTO)
    }
}
