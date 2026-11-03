package org.br.dietamais.ui.paciente.perfil

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.datetime.LocalTime
import org.br.dietamais.data.remoto.*
import org.br.dietamais.data.remoto.dto.*
import org.br.dietamais.data.repositorio.*

enum class MensagemPerfil { SEM_CONEXAO, CONFLITO, HORARIO_INVALIDO, SILENCIO_IGUAL, ERRO, SALVO, LINK_REVOGADO }

data class EstadoPerfilPaciente(
    val lembretes: Boolean = true, val tipos: List<PreferenciaTipo> = emptyList(), val silencioAtivo: Boolean = true,
    val inicio: String = "22:00", val fim: String = "07:00", val versao: Long = 0, val alterado: Boolean = false,
    val nutris: List<VinculoResposta> = emptyList(), val calendario: CalendarioTokenResposta? = null,
    val carregando: Boolean = false, val enviando: Boolean = false, val semConexao: Boolean = false,
    val erro: MensagemPerfil? = null, val aviso: MensagemPerfil? = null,
) { override fun toString() = "EstadoPerfilPaciente[protegido]" }

/** Perfil do paciente (MOB-19) e link da agenda (MOB-21). Lembretes locais são do aparelho; tipos de push e silêncio vão ao backend (RN-NOT-03). */
class PerfilPacienteViewModel(private val preferencias: PortaPreferencias, private val vinculos: PortaVinculosLocais, private val calendario: PortaCalendario,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate, conectividade: Flow<Boolean>? = null) : ViewModel() {
    private val atual = MutableStateFlow(EstadoPerfilPaciente())
    val estado = atual.asStateFlow()

    init {
        viewModelScope.launch(dispatcher) { preferencias.observarLembretes().catch { }.collect { v -> atual.update { it.copy(lembretes = v) } } }
        viewModelScope.launch(dispatcher) { vinculos.observarVinculos().catch { }.collect { v -> atual.update { it.copy(nutris = v) } } }
        viewModelScope.launch(dispatcher) { preferencias.observar().catch { }.collect { p -> if (p != null && !atual.value.alterado) aplicar(p) } }
        conectividade?.let { fluxo -> viewModelScope.launch(dispatcher) { fluxo.collect { online -> atual.update { it.copy(semConexao = !online) } } } }
        recarregar()
    }

    private fun aplicar(p: PreferenciasNotificacaoResposta) = atual.update {
        it.copy(tipos = p.tipos, silencioAtivo = p.silencio.ativo, inicio = p.silencio.inicio.paraHora(), fim = p.silencio.fim.paraHora(), versao = p.versao, alterado = false)
    }

    fun recarregar() { executar(carregar = true) { aplicar(preferencias.atualizar()) } }

    fun mudarLembretes(ativo: Boolean) { try { preferencias.definirLembretes(ativo) } catch (_: Exception) { atual.update { it.copy(erro = MensagemPerfil.ERRO) } } }
    fun mudarTipo(tipo: TipoNotificacao, ativo: Boolean) = atual.update { e -> e.copy(tipos = e.tipos.map { if (it.tipo == tipo) it.copy(ativo = ativo) else it }, alterado = true, aviso = null) }
    fun mudarSilencioAtivo(ativo: Boolean) = atual.update { it.copy(silencioAtivo = ativo, alterado = true, aviso = null) }
    fun mudarInicio(v: String) = atual.update { it.copy(inicio = v.take(5), alterado = true, aviso = null) }
    fun mudarFim(v: String) = atual.update { it.copy(fim = v.take(5), alterado = true, aviso = null) }

    fun salvar() {
        val e = atual.value
        if (e.enviando) return
        val inicio = hora(e.inicio); val fim = hora(e.fim)
        if (inicio == null || fim == null) { atual.update { it.copy(erro = MensagemPerfil.HORARIO_INVALIDO) }; return }
        if (inicio == fim) { atual.update { it.copy(erro = MensagemPerfil.SILENCIO_IGUAL) }; return }
        executar { aplicar(preferencias.salvar(AtualizarPreferenciasRequisicao(e.tipos, SilencioNotificacao(e.silencioAtivo, inicio, fim)))); atual.update { it.copy(aviso = MensagemPerfil.SALVO) } }
    }

    fun gerarLink() { executar { val c = calendario.gerar(); atual.update { it.copy(calendario = c) } } }
    fun revogarLink() { executar { calendario.revogar(); atual.update { it.copy(calendario = null, aviso = MensagemPerfil.LINK_REVOGADO) } } }

    private fun executar(carregar: Boolean = false, bloco: suspend () -> Unit) {
        if (atual.value.semConexao) { if (!carregar) atual.update { it.copy(erro = MensagemPerfil.SEM_CONEXAO) }; return }
        viewModelScope.launch(dispatcher) {
            atual.update { it.copy(enviando = !carregar, carregando = carregar, erro = null) }
            try { bloco(); atual.update { it.copy(enviando = false, carregando = false) } }
            catch (c: CancellationException) { throw c }
            catch (erro: ErroApi) { atual.update { it.copy(enviando = false, carregando = false, erro = if (erro.codigo == "CONFLITO_VERSAO") MensagemPerfil.CONFLITO else MensagemPerfil.ERRO) } }
            catch (_: FalhaComunicacao) { atual.update { it.copy(enviando = false, carregando = false, erro = MensagemPerfil.SEM_CONEXAO) } }
            catch (_: SessaoAlterada) { atual.update { it.copy(enviando = false, carregando = false) } }
            catch (_: Exception) { atual.update { it.copy(enviando = false, carregando = false, erro = MensagemPerfil.ERRO) } }
        }
    }

    companion object {
        fun hora(texto: String): LocalTime? = Regex("([01]\\d|2[0-3]):([0-5]\\d)").matchEntire(texto.trim())?.let { LocalTime(it.groupValues[1].toInt(), it.groupValues[2].toInt()) }
    }
}

private fun LocalTime.paraHora() = "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
