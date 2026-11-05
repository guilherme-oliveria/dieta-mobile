package org.br.dietamais.ui.nutri.dieta

import org.br.dietamais.data.remoto.dto.*
import org.br.dietamais.data.repositorio.PortaDietas
import org.br.dietamais.data.repositorio.PortaModelosDieta
import org.br.dietamais.data.sync.novoIdLocal
import kotlin.uuid.Uuid

/**
 * MOB-56b: o editor de modelo reaproveita o planejador por porções. O modelo é lido como uma dieta em rascunho (sem paciente, sem totais do
 * servidor) e a gravação vira `POST/PUT /eu/nutri/modelos-dieta`. Descrição, condição e restrições não têm campo no planejador: seguem as do
 * modelo aberto ([base]); a energia de referência é o VET das metas editadas ou, sem metas, a do modelo aberto.
 */
object ModeloComoDieta {
    /** Paciente "vazio": o planejador exige um dono, mas o modelo é da nutri. */
    val SEM_PACIENTE: Uuid = Uuid.NIL

    /** Só o planejador por porções edita sem perder itens; modelo com alimento do catálogo continua só leitura e duplicável. */
    fun editavel(m: ModeloDietaResposta) = !m.doSistema && m.refeicoes.none { r -> r.itens.any { it.alimentoId != null } }

    fun comoDieta(m: ModeloDietaResposta): DietaResposta {
        val zero = TotaisNutricionaisResposta(0.0, 0.0, 0.0, 0.0)
        return DietaResposta(id = m.id, pacienteId = SEM_PACIENTE, vinculoId = Uuid.NIL, titulo = m.titulo, status = StatusDieta.RASCUNHO,
            orientacao = m.orientacao, refeicoes = m.refeicoes.mapIndexed { i, r ->
                RefeicaoResposta(id = r.id ?: novoIdLocal(), nome = r.nome, horario = r.horario, ordem = i, tipo = r.tipo, alternativaDe = r.alternativaDe, totais = zero,
                    itens = r.itens.map { it ->
                        ItemRefeicaoResposta(id = it.id ?: novoIdLocal(), alimentoId = it.alimentoId, grupoId = it.grupoId, porcoes = it.porcoes,
                            observacao = it.observacao, aVontade = it.aVontade, textoLivre = it.textoLivre, totais = zero)
                    })
            }, totais = zero, versao = m.versao, criadoEm = m.criadoEm, atualizadoEm = m.atualizadoEm, porcoes = m.porcoes, metas = m.metas, rodape = m.rodape)
    }

    fun comoRequisicao(d: SalvarDietaRequisicao, base: ModeloDietaResposta?) = SalvarModeloDietaRequisicao(id = d.id, titulo = d.titulo,
        descricao = base?.descricao, condicao = base?.condicao, orientacao = d.orientacao, refeicoes = d.refeicoes, porcoes = d.porcoes, metas = d.metas,
        rodape = d.rodape, restricoes = base?.restricoes,
        // RN-MOD-02: sem energia de referência a aplicação cai em SEM_ESCALA; o VET editado nas metas vale mais que a referência antiga
        kcalReferencia = d.metas?.vetKcal ?: base?.kcalReferencia)
}

/** Faz o planejador gravar modelos. [base] é o modelo aberto (quem abre o editor define); `null` = modelo novo. */
class DietasDeModelo(private val modelos: PortaModelosDieta) : PortaDietas {
    @Volatile var base: ModeloDietaResposta? = null

    override suspend fun listar(pacienteId: Uuid): List<DietaResposta> = emptyList()
    override suspend fun criar(pacienteId: Uuid, requisicao: SalvarDietaRequisicao, chave: Uuid): DietaResposta {
        val m = modelos.criar(ModeloComoDieta.comoRequisicao(requisicao, base), chave)
        base = m
        return ModeloComoDieta.comoDieta(m)
    }
    override suspend fun salvar(id: Uuid, versao: Long, requisicao: SalvarDietaRequisicao): DietaResposta {
        val m = modelos.atualizar(id, versao, ModeloComoDieta.comoRequisicao(requisicao.copy(id = null), base))
        base = m
        return ModeloComoDieta.comoDieta(m)
    }
    override suspend fun publicar(id: Uuid, versao: Long): DietaSnapshotResposta = throw UnsupportedOperationException()
    override suspend fun duplicar(id: Uuid, chave: Uuid): DietaResposta = throw UnsupportedOperationException()
}
