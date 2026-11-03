package org.br.dietamais.data.sync

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.br.dietamais.data.repositorio.*
import org.br.dietamais.dominio.rotina.*
import org.br.dietamais.plataforma.NotificacaoLocal
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours

/**
 * Reagenda todos os lembretes locais sempre que a dieta em uso, os check-ins ou as preferências mudam (RN-ROT-05).
 * Sem permissão ou com o interruptor desligado, cancela tudo.
 */
class AgendadorLembretes(private val dietas: PortaDietaPaciente, private val rotina: PortaRotina, private val preferencias: PortaPreferencias,
    private val notificacao: NotificacaoLocal, private val textos: suspend () -> TextosLembrete,
    private val relogio: Clock = Clock.System, private val fuso: () -> TimeZone = { TimeZone.currentSystemDefault() },
    private val tique: Flow<Unit> = flow { while (true) { emit(Unit); kotlinx.coroutines.delay(6.hours) } }) {

    suspend fun executar() {
        combine(dietas.observar(), rotina.observar(), preferencias.observarLembretes(), preferencias.observar().onStart { emit(null) }, tique) { d, r, ligado, prefs, _ ->
            Entrada(d, r, ligado, prefs?.silencio?.let { JanelaSilencio(it.ativo, it.inicio, it.fim) } ?: JanelaSilencio())
        }.collectLatest { e ->
            try {
                val dieta = dietaVigente(e.dietas.snapshots, e.dietas.emUso?.dietaId)
                if (dieta == null || !e.ligado || !notificacao.permitido()) { notificacao.cancelarTudo(); return@collectLatest }
                val agora = relogio.now(); val zona = fuso()
                val hoje = agora.toLocalDateTime(zona).date
                val feitas = e.rotina.checkins.filter { it.data == hoje && it.dietaId == dieta.dietaId }.map { it.refeicaoId }.toSet()
                val lembretes = planejarLembretes(dieta.refeicoes, agora, zona, e.silencio, feitas).map { it.copy(dietaId = dieta.dietaId) }
                notificacao.agendar(lembretes, textos())
            } catch (c: CancellationException) { throw c }
            catch (_: Exception) { /* lembrete é melhor esforço: falha do agendador não derruba o app */ }
        }
    }

    private data class Entrada(val dietas: DadosDietaPaciente, val rotina: DadosRotina, val ligado: Boolean, val silencio: JanelaSilencio)
}
