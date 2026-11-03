package org.br.dietamais.ui.paciente.dieta

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.br.dietamais.data.remoto.*
import org.br.dietamais.data.remoto.dto.StatusCheckin
import org.br.dietamais.data.remoto.dto.TrocaCheckin
import org.br.dietamais.data.repositorio.*
import org.br.dietamais.dominio.rotina.*
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.uuid.Uuid

/** Hoje, Dieta, check-in, troca e água do paciente (MOB-14, 15, 17). Leitura e escrita são locais; só a troca de dieta em uso exige rede. */
class DietaPacienteViewModel(private val repositorio: PortaDietaPaciente, private val rotina: PortaRotina,
    private val relogio: Clock = Clock.System, private val fuso: TimeZone = TimeZone.currentSystemDefault(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate, conectividade: Flow<Boolean>? = null,
    private val tique: Flow<Unit> = flow { while (true) { emit(Unit); delay(1.minutes) } },
    private val aoGravar: () -> Unit = {}) : ViewModel() {
    private val atual = MutableStateFlow(EstadoDietaPaciente())
    val estado = atual.asStateFlow()
    private var dados = DadosDietaPaciente()
    private var registros = DadosRotina()
    private var diaEscolhido: LocalDate? = null

    init {
        viewModelScope.launch(dispatcher) {
            combine(repositorio.observar().catch { atual.update { it.copy(erro = MensagemDietaPaciente.ERRO, carregado = true) } },
                rotina.observar().catch { atual.update { it.copy(erro = MensagemDietaPaciente.ERRO, carregado = true) } }, tique) { d, r, _ -> d to r }
                .collect { (d, r) -> dados = d; registros = r; recalcular() }
        }
        conectividade?.let { fluxo -> viewModelScope.launch(dispatcher) { fluxo.collect { online -> atual.update { it.copy(semConexao = !online) } } } }
    }

    private fun agoraLocal() = relogio.now().toLocalDateTime(fuso)

    private fun recalcular() = atual.update { e ->
        val agora = agoraLocal(); val hoje = agora.date; val dias = diasComCheckin(hoje)
        val dia = diaEscolhido?.takeIf { it in dias } ?: hoje
        val dieta = dietaVigente(dados.snapshots, dados.emUso?.dietaId)
        val porNutri = dados.snapshots.sortedByDescending { it.publicadoEm }.distinctBy { it.nutricionista.id }
        // RN-ROT-01: um check-in por refeição/dia, o último vale
        val doDia = registros.checkins.filter { it.data == dia && it.dietaId == dieta?.dietaId }.groupBy { it.refeicaoId }
            .mapValues { (_, lista) -> lista.maxBy { it.registradoEm }.status }
        val peso = registros.pesos.maxByOrNull { it.registradoEm }?.pesoKg
        val aguaDoDia = registros.agua.filter { it.registradoEm.toLocalDateTime(fuso).date == dia }.sumOf { it.ml }
        e.copy(carregado = true, dieta = dieta, refeicoes = dieta?.let { refeicoesOrdenadas(it.refeicoes) }.orEmpty(),
            proxima = if (dia == hoje) dieta?.let { proximaRefeicao(it.refeicoes.filter { r -> r.id !in doDia }, agora.time) } else null,
            alternativas = if (porNutri.size > 1) porNutri else emptyList(), versaoSelecao = dados.emUso?.versao ?: 0,
            hoje = hoje, dia = dia, dias = dias, checkins = doDia, aguaMl = aguaDoDia, metaAguaMl = metaAguaMl(peso))
    }

    fun abrirRefeicao(id: Uuid?) = atual.update { it.copy(refeicaoAberta = id) }

    fun mudarDia(dia: LocalDate) { diaEscolhido = dia; recalcular() }

    // ---- check-in (MOB-17)
    fun marcar(refeicaoId: Uuid, status: StatusCheckin) {
        val e = atual.value; val dieta = e.dieta ?: return; val dia = e.dia ?: return
        if (status == StatusCheckin.TROCADO || status == StatusCheckin.OUTRO || dieta.refeicoes.none { it.id == refeicaoId }) return
        gravar { rotina.registrarCheckin(dieta.dietaId, refeicaoId, dia, status, emptyList(), relogio.now()) }
    }

    fun beberAgua(ml: Int) {
        if (ml !in 1..5000) return
        gravar { rotina.registrarAgua(ml, relogio.now()) }
    }

    // ---- troca de alimento (MOB-15)
    fun abrirTroca(refeicaoId: Uuid) = atual.update { it.copy(troca = EstadoTroca(refeicaoId)) }
    fun cancelarTroca() = atual.update { it.copy(troca = null) }
    fun escolherSubstituto(itemId: Uuid, alimentoId: Uuid?) = atual.update { e ->
        val t = e.troca ?: return@update e
        e.copy(troca = t.copy(escolhas = if (alimentoId == null) t.escolhas - itemId else t.escolhas + (itemId to alimentoId)))
    }
    fun confirmarTroca() {
        val e = atual.value; val t = e.troca ?: return; val dieta = e.dieta ?: return; val dia = e.dia ?: return
        val refeicao = dieta.refeicoes.firstOrNull { it.id == t.refeicaoId } ?: return
        // só vale item da refeição com substituto permitido pela lista congelada (o backend confere de novo)
        val trocas = t.escolhas.mapNotNull { (itemId, alimentoId) ->
            val item = refeicao.itens.firstOrNull { it.id == itemId } ?: return@mapNotNull null
            if (substitutosDoItem(item, dieta.listaSubstituicao).none { it.alimento.id == alimentoId }) null else TrocaCheckin(itemId, alimentoId)
        }
        if (trocas.isEmpty()) return
        gravar { rotina.registrarCheckin(dieta.dietaId, t.refeicaoId, dia, StatusCheckin.TROCADO, trocas, relogio.now()) }
        atual.update { it.copy(troca = null) }
    }

    private fun gravar(bloco: () -> Unit) {
        try { bloco(); atual.update { it.copy(erro = null) }; aoGravar() }
        catch (c: CancellationException) { throw c }
        catch (_: SessaoAlterada) { }
        catch (_: Exception) { atual.update { it.copy(erro = MensagemDietaPaciente.ERRO) } }
    }

    fun escolher(dietaId: Uuid) {
        val e = atual.value
        if (e.enviando) return
        if (e.semConexao) { atual.update { it.copy(erro = MensagemDietaPaciente.SEM_CONEXAO) }; return }
        viewModelScope.launch(dispatcher) {
            atual.update { it.copy(enviando = true, erro = null) }
            try {
                val resposta = repositorio.selecionar(dietaId, e.versaoSelecao)
                dados = dados.copy(emUso = resposta); recalcular()
                atual.update { it.copy(enviando = false) }
            } catch (c: CancellationException) { throw c }
            catch (erro: ErroApi) {
                atual.update { it.copy(enviando = false, erro = if (erro.codigo == "CONFLITO_VERSAO") MensagemDietaPaciente.CONFLITO else MensagemDietaPaciente.ERRO) }
            } catch (_: FalhaComunicacao) { atual.update { it.copy(enviando = false, erro = MensagemDietaPaciente.SEM_CONEXAO) } }
            catch (_: SessaoAlterada) { atual.update { it.copy(enviando = false) } }
            catch (_: Exception) { atual.update { it.copy(enviando = false, erro = MensagemDietaPaciente.ERRO) } }
        }
    }
}
