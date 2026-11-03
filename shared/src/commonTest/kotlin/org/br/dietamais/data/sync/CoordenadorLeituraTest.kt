package org.br.dietamais.data.sync

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.br.dietamais.data.remoto.dto.Perfil
import org.br.dietamais.plataforma.EstadoConexao
import kotlin.test.*
import kotlin.time.Duration.Companion.minutes
import kotlin.uuid.Uuid

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class CoordenadorLeituraTest {
    private val contexto = ContextoLeituraSync(Uuid.random(), Perfil.PACIENTE, "epoca-ficticia")

    @Test fun abreRetomaEExecutaPeriodicoSomenteEmPrimeiroPlano() = runTest {
        val sessao = MutableStateFlow<ContextoLeituraSync?>(contexto)
        val rede = MutableStateFlow(EstadoConexao.DISPONIVEL)
        var chamadas = 0
        val coordenador = CoordenadorLeitura(backgroundScope, sessao, rede, { chamadas++ })
        coordenador.primeiroPlano(true); runCurrent(); assertEquals(1, chamadas)
        coordenador.primeiroPlano(true); runCurrent(); assertEquals(1, chamadas)
        advanceTimeBy(15.minutes); runCurrent(); assertEquals(2, chamadas)
        coordenador.primeiroPlano(false); runCurrent(); advanceTimeBy(30.minutes); runCurrent(); assertEquals(2, chamadas)
        coordenador.primeiroPlano(true); runCurrent(); assertEquals(3, chamadas)
    }

    @Test fun reconexaoDisparaSemRepetirMesmoEstadoDeRede() = runTest {
        val rede = MutableStateFlow(EstadoConexao.INDISPONIVEL)
        var chamadas = 0
        val coordenador = CoordenadorLeitura(backgroundScope, MutableStateFlow(contexto), rede, { chamadas++ })
        coordenador.primeiroPlano(true); runCurrent(); assertEquals(1, chamadas)
        rede.value = EstadoConexao.DISPONIVEL; runCurrent(); assertEquals(2, chamadas)
        rede.value = EstadoConexao.DISPONIVEL; runCurrent(); assertEquals(2, chamadas)
        coordenador.close(); advanceTimeBy(15.minutes); runCurrent(); assertEquals(2, chamadas)
    }

    @Test fun trocarSessaoCancelaPullAnteriorAntesDeExecutarOutro() = runTest {
        val sessao = MutableStateFlow<ContextoLeituraSync?>(contexto)
        val liberar = CompletableDeferred<Unit>()
        var ativas = 0; var maior = 0; var chamadas = 0
        val coordenador = CoordenadorLeitura(backgroundScope, sessao, MutableStateFlow(EstadoConexao.DISPONIVEL), {
            chamadas++; ativas++; maior = maxOf(maior, ativas)
            try { liberar.await() } finally { ativas-- }
        })
        coordenador.primeiroPlano(true); runCurrent()
        sessao.value = contexto.copy(operacao = "outra-epoca"); runCurrent()
        assertEquals(2, chamadas); assertEquals(1, maior)
        liberar.complete(Unit); runCurrent(); assertEquals(0, ativas)
    }

    @Test fun semSessaoNaoDisparaELoginRetomaSemNovoOnResume() = runTest {
        val sessao = MutableStateFlow<ContextoLeituraSync?>(null)
        var chamadas = 0
        val coordenador = CoordenadorLeitura(backgroundScope, sessao, MutableStateFlow(EstadoConexao.DISPONIVEL), { chamadas++ })
        coordenador.primeiroPlano(true); runCurrent(); assertEquals(0, chamadas)
        sessao.value = contexto; runCurrent(); assertEquals(1, chamadas)
        sessao.value = null; runCurrent(); advanceTimeBy(15.minutes); runCurrent(); assertEquals(1, chamadas)
    }
}
