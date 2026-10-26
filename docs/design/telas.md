# Mapa de telas

Pacotes em `shared/src/commonMain/kotlin/org/br/dietamais/`, conforme o contrato combinado com a arquitetura. Artboard = arquivo no canvas https://claude.ai/artifact/6thej5zGsTo81ZosiigGxC. Fase = ver [plano-de-acao-ui.md](plano-de-acao-ui.md).

Status: **desenhada** (seguir o artboard com as correções de [validacao.md](validacao.md)), **faltando** (seguir a descrição abaixo e os componentes de [componentes.md](componentes.md)).

## Entrada (`ui/auth`)

| Tela | Composable | Artboard | Status | Fase |
| --- | --- | --- | --- | --- |
| Entrar | `EntrarTela` | `Entrar.dc.html` | desenhada; implementada MOB-06 | 1 |
| Cadastro do paciente por convite (código de 8 caracteres ou link, validade 7 dias) | `CadastroPacienteScreen` | — | faltando | 1 |
| Consentimento LGPD e termos (no aceite do convite) | `ConsentimentoScreen` | — | faltando | 1 |
| Cadastro da nutri (nome, CRN, e-mail, senha; começa no teste) | `CadastroNutriTela` | — | artboard faltando; implementada pelos componentes (MOB-06) | 1 |
| Recuperar senha | `RecuperarSenhaTela` | — | artboard faltando; implementada pelos componentes (MOB-06) | 1 |
| Escolher perfil (conta que é nutri e paciente) | `EscolherPerfilTela` | — | artboard faltando; implementada pelos componentes (MOB-06) | 1 |

## Paciente

| Tela | Pacote / composable | Artboard | Status | Fase |
| --- | --- | --- | --- | --- |
| Hoje (dieta do dia, água, próxima refeição, check-in) | `ui/paciente/hoje` · `HojeScreen` | `Main.dc.html` | desenhada | 2 |
| Sem dieta publicada ainda | `ui/paciente/hoje` · estado de `HojeScreen` | — | faltando | 2 |
| Dieta / refeição detalhada | `ui/paciente/dieta` · `RefeicaoScreen` | `P-Refeicao.dc.html` | desenhada | 2 |
| Orientações gerais | `ui/paciente/dieta` · `OrientacoesScreen` | — | faltando | 2 |
| PDF da dieta | `ui/paciente/dieta` · `PdfDietaScreen` | — | faltando | 4 |
| Trocar alimento | `ui/paciente/substituicao` · `SubstituirScreen` | `P-Substituir.dc.html` | desenhada | 2 |
| Lista de compras (3, 7, 15 dias) | `ui/paciente/compras` · `ComprasScreen` | `P-Compras.dc.html` | desenhada | 3 |
| Evolução | `ui/paciente/evolucao` · `EvolucaoScreen` | `P-Progresso.dc.html` | desenhada | 3 |
| Registrar peso (folha inferior) | `ui/paciente/evolucao` · `RegistrarPesoSheet` | — | faltando | 3 |
| Água (somar copo, meta 35 ml/kg) | `ui/paciente/hoje` · `AguaSheet` | — | faltando | 3 |
| Anamnese (wizard de 12 passos) | `ui/paciente/anamnese` · `AnamneseScreen` | `P-Anamnese.dc.html` (passo 7) | desenhada em parte: os outros 11 passos seguem o mesmo molde, blocos do doc do MVP | 3 |
| Perfil | `ui/paciente/perfil` · `PerfilScreen` | — | faltando | 2 |
| Lembretes e agenda (.ics) | `ui/paciente/perfil` · `LembretesScreen` | `P-Lembretes.dc.html` | desenhada | 3 |
| Minhas nutris (vínculos, trocar nutri ativa, autorizar cópia do histórico) | `ui/paciente/perfil` · `MinhasNutrisScreen` | — | faltando | 4 |
| Dados e privacidade (exportar, excluir conta) | `ui/paciente/perfil` · `PrivacidadeScreen` | — | faltando | 4 |

Bottom nav do paciente: Hoje · Dieta · Compras · Evolução · Perfil.

## Nutri

| Tela | Pacote / composable | Artboard | Status | Fase |
| --- | --- | --- | --- | --- |
| Painel / Início (tablet e desktop) | `ui/nutri/painel` · `PainelScreen` | `T-Painel.dc.html` | desenhada | 2 |
| Painel / Início (celular) | `ui/nutri/painel` · `PainelScreen` compacto | — | faltando (coluna do tablet) | 2 |
| Pacientes | `ui/nutri/pacientes` · `PacientesScreen` | `N-Pacientes.dc.html` | desenhada | 2 |
| Convidar paciente | `ui/nutri/pacientes` · `ConvidarSheet` | — | faltando | 2 |
| Limite do plano (`LIMITE_PACIENTES_ATINGIDO`) | `ui/comum` · `FolhaLimitePlano` | — | faltando | 2 |
| Ficha do paciente (celular) | `ui/nutri/paciente` · `FichaScreen` | `N-Paciente.dc.html` | desenhada | 2 |
| Ficha do paciente (tablet/desktop, lista-detalhe) | `ui/nutri/paciente` · `FichaScreen` expandida | — | faltando (compor Pacientes + Ficha) | 2 |
| Nova avaliação | `ui/nutri/avaliacao` · `AvaliacaoScreen` | `T-Avaliacao.dc.html` | desenhada (tablet); celular faltando | 3 |
| Histórico e comparação de avaliações | `ui/nutri/avaliacao` · `HistoricoAvaliacoesScreen` | — | faltando | 4 |
| Montar dieta (estados RASCUNHO, PUBLICADA, SUBSTITUIDA) | `ui/nutri/dieta` · `MontarDietaScreen` | `T-Dieta.dc.html` | desenhada (tablet); celular faltando | 3 |
| Publicar dieta (diálogo com resumo da mudança) | `ui/nutri/dieta` · `PublicarDialog` | — | faltando | 3 |
| Agenda de consultas | `ui/nutri/agenda` · `AgendaScreen` | — | faltando | 4 |
| Alimentos (lista de substituição da nutri) | `ui/nutri/alimentos` · `AlimentosScreen` | — | faltando | 3 |
| Anamnese: modelo e respostas | `ui/nutri/anamnese` · `ModeloAnamneseScreen`, `RespostasAnamneseScreen` | — | faltando | 4 |
| Conta e assinatura (TESTE, ATIVA, EM_ATRASO, CANCELADA, EXPIRADA) | `ui/nutri/assinatura` · `ContaScreen` | — | faltando | 4 |
| Modo leitura (EM_ATRASO depois de 7 dias de carência) | `ui/nutri/assinatura` · `AvisoSomenteLeitura` | — | faltando | 4 |

Navegação da nutri (igual no celular e no trilho): Início · Pacientes · Agenda · Alimentos · Conta.

## Comum

| Tela | Pacote | Status | Fase |
| --- | --- | --- | --- |
| Central de avisos (sino) | `ui/notificacoes` · `AvisosScreen` | faltando | 3 |
| Diálogo de conflito de sincronização | `ui/comum` · `DialogoConflito` | faltando | 4 |
| Primeira sincronização do desktop | `ui/comum` · `PrimeiraSyncScreen` | faltando | 4 |
