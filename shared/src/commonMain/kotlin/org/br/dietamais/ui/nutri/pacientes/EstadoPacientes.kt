package org.br.dietamais.ui.nutri.pacientes

import org.br.dietamais.data.remoto.dto.*
import org.br.dietamais.data.sync.novoIdLocal
import kotlin.uuid.Uuid

enum class CampoPaciente { NOME, EMAIL, NASCIMENTO, TELEFONE, ESTATURA, OBJETIVO, OBSERVACOES }
enum class AbaFicha { RESUMO, AVALIACOES, EVOLUCAO, DIETA, LISTA, ANAMNESE }
enum class MensagemPacientes {
    OBRIGATORIO, EMAIL_INVALIDO, DATA_INVALIDA, ALTURA_INVALIDA, CAMPO_INVALIDO, SEM_CONEXAO,
    LIMITE_ATINGIDO, ASSINATURA_INATIVA, EMAIL_NAO_CONFIRMADO, VINCULO_JA_EXISTE, TAXA_EXCEDIDA,
    CONVITES_INDISPONIVEIS, CONSENTIMENTO_INDISPONIVEL, NAO_ENCONTRADO, ERRO_PADRAO, COMPARTILHAMENTO_FALHOU, COMPARTILHAMENTO_ABERTO,
    CONVITE_COPIADO, PACIENTE_CRIADO,
}

data class FormularioPaciente(
    val id: Uuid = novoIdLocal(), val chave: Uuid = novoIdLocal(), val nome: String = "", val email: String = "",
    val nascimento: String = "", val sexo: Sexo? = null, val telefone: String = "", val estatura: String = "",
    val objetivo: String = "", val observacoes: String = "", val ampliado: org.br.dietamais.dominio.pacientes.FormularioCadastroAmpliado = org.br.dietamais.dominio.pacientes.FormularioCadastroAmpliado(),
) { override fun toString() = "FormularioPaciente[conteúdo protegido]" }

data class EstadoPacientes(
    val lista: List<PacienteResposta> = emptyList(), val busca: String = "", val filtro: StatusVinculo? = null,
    val filtroAtencao: org.br.dietamais.dominio.pacientes.FiltroAtencao? = null, val resumos: Map<Uuid, org.br.dietamais.dominio.pacientes.ResumoPaciente>? = null, val precisamAtencao: Int = 0,
    val ativos: Int = 0, val atalhos: org.br.dietamais.dominio.pacientes.AtalhosPacientes<PacienteResposta> = org.br.dietamais.dominio.pacientes.AtalhosPacientes(emptyList(), emptyList()),
    val fixadosIds: Set<Uuid> = emptySet(), val selecionado: PacienteResposta? = null, val atualizando: Boolean = false, val envioEmAndamento: Boolean = false,
    val semConexao: Boolean = false, val erro: MensagemPacientes? = null, val aviso: MensagemPacientes? = null,
    val formulario: FormularioPaciente? = null, val errosCampos: Map<String, MensagemPacientes> = emptyMap(),
    val convite: ConviteCriadoResposta? = null, val limiteAtingido: Boolean = false,
    val assinatura: AssinaturaResumoResposta? = null, val abaFicha: AbaFicha = AbaFicha.RESUMO,
) { override fun toString() = "EstadoPacientes[conteúdo clínico protegido]" }
