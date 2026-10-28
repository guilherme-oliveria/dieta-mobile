package org.br.dietamais.data.sync

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.br.dietamais.data.remoto.dto.Perfil
import kotlin.test.*
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.Uuid

@OptIn(ExperimentalCoroutinesApi::class)
class RegistradorPushTest {
    private val usuario = Uuid.random()
    private fun contexto(id: Uuid = usuario, perfil: Perfil = Perfil.PACIENTE) = ContextoLeituraSync(id, perfil, "op-$id")

    @Test fun registraQuandoHaSessaoTokenERedeEUmaVezSo() = runTest {
        val chamadas = mutableListOf<String>()
        val ctx = MutableStateFlow<ContextoLeituraSync?>(null); val token = MutableStateFlow<String?>(null); val online = MutableStateFlow(true)
        RegistradorPush({ chamadas += it }, backgroundScope, ctx, token, online, 5.seconds)
        runCurrent(); assertTrue(chamadas.isEmpty())
        ctx.value = contexto(); runCurrent(); assertTrue(chamadas.isEmpty()) // falta o token
        token.value = "t1"; runCurrent(); assertEquals(listOf("t1"), chamadas)
        online.value = false; runCurrent(); online.value = true; runCurrent()
        assertEquals(listOf("t1"), chamadas) // mesmo usuário e token: não repete
    }

    @Test fun novoTokenOuOutraContaNoMesmoAparelhoRegistramDeNovo() = runTest {
        val chamadas = mutableListOf<String>()
        val ctx = MutableStateFlow<ContextoLeituraSync?>(contexto()); val token = MutableStateFlow<String?>("t1")
        RegistradorPush({ chamadas += it }, backgroundScope, ctx, token, MutableStateFlow(true), 5.seconds)
        runCurrent(); token.value = "t2"; runCurrent()
        ctx.value = contexto(Uuid.random(), Perfil.NUTRICIONISTA); runCurrent()
        assertEquals(listOf("t1", "t2", "t2"), chamadas)
    }

    @Test fun semRedeNaoTentaEFalhaTentaDeNovoDepoisDaEspera() = runTest {
        var tentativas = 0
        val ctx = MutableStateFlow<ContextoLeituraSync?>(contexto()); val online = MutableStateFlow(false)
        RegistradorPush({ tentativas++; if (tentativas < 3) error("falha") }, backgroundScope, ctx, MutableStateFlow("t1"), online, 5.seconds)
        runCurrent(); assertEquals(0, tentativas)
        online.value = true; runCurrent(); assertEquals(1, tentativas)
        advanceTimeBy(5.seconds); runCurrent(); assertEquals(2, tentativas)
        advanceTimeBy(5.seconds); runCurrent(); assertEquals(3, tentativas)
        advanceTimeBy(60.seconds); runCurrent(); assertEquals(3, tentativas) // registrou: para
    }

    @Test fun inativoNaoFazNada() = runTest {
        var chamadas = 0
        RegistradorPush({ chamadas++ }, backgroundScope, MutableStateFlow(contexto()), MutableStateFlow("t"), MutableStateFlow(true), ativo = false)
        runCurrent(); assertEquals(0, chamadas)
    }
}
