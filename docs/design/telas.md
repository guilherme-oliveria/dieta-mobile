# Mapa de telas

Pacotes em `shared/src/commonMain/kotlin/org/br/dietamais/`, conforme o contrato combinado com a arquitetura. Artboard = arquivo no canvas https://claude.ai/artifact/6thej5zGsTo81ZosiigGxC. Fase = ver [plano-de-acao-ui.md](plano-de-acao-ui.md).

Status: **desenhada** (seguir o artboard com as correções de [validacao.md](validacao.md)), **faltando** (seguir a descrição abaixo e os componentes de [componentes.md](componentes.md)).

## Entrada (`ui/auth`)

| Tela | Composable | Artboard | Status | Fase |
| --- | --- | --- | --- | --- |
| Entrar | `EntrarTela` | `Entrar.dc.html` | desenhada; implementada MOB-06 | 1 |
| Cadastro do paciente por convite (código de 8 caracteres ou link, validade 7 dias) | `ConviteTela` | — | artboard faltando; implementada pelos componentes (MOB-08), cadastro condicionado aos termos publicados | 1 |
| Consentimento clínico e termos de uso separados (no aceite do convite) | `ConviteTela` + `TermosUsoParte` | — | artboard faltando; implementada pelos componentes (MOB-08), texto oficial do backend | 1 |
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

## Melhorias com as planilhas da nutri (ondas 17 a 23)

Desenhadas em 01/11/2026 (DES-08 a DES-10). Os artboards estão no canvas e em [artboards/](artboards/README.md); o padrão é [padrao-ui.md](padrao-ui.md) e os gráficos, [graficos.md](graficos.md).

| Tela | Pacote / composable | Artboard | Status | MOB |
| --- | --- | --- | --- | --- |
| Avaliação, passo 1 Medidas (celular) | `ui/nutri/avaliacao` · `AvaliacaoTela` | `A-Avaliacao-Medidas.dc.html` | desenhada; implementada MOB-50 | 50 |
| Avaliação, passo 2 Composição (celular) | idem | `A-Avaliacao-Composicao.dc.html` | desenhada; implementada MOB-50 | 50 |
| Avaliação, passo 3 Necessidades (rosca das metas) | `ui/nutri/avaliacao` · `PassoNecessidades` | `A-Avaliacao-Necessidades.dc.html` | desenhada; implementada MOB-52b | 52 |
| Avaliação, passo 4 Resumo (compartilhar por seção, PDF) | idem | `A-Avaliacao-Resumo.dc.html` | desenhada; implementada sem "Gerar PDF" (D-05) | 50 |
| Avaliação em passos com painel de resultados (tablet e desktop) | `AvaliacaoTela` expandida · `PainelResultados` | `T-Avaliacao-Passos.dc.html` | desenhada; implementada MOB-50 | 50 |
| Meus resultados (semáforos, composição, meta) | `ui/paciente/evolucao` · `MeusResultadosTela` | `P-Resultados.dc.html` | desenhada; implementada MOB-51 com divergência D-02 | 51 |
| Meus resultados, evolução com faixa | idem | `P-Resultados-Evolucao.dc.html` | desenhada; implementada MOB-51 (D-03) | 51 |
| Meus resultados, nada compartilhado | idem | `P-Resultados-Vazio.dc.html` | desenhada; implementada MOB-51 | 51 |
| Meus protocolos | `ui/nutri/protocolos` · `ProtocolosNutriCard` | `N-Protocolos.dc.html` | desenhada; implementada MOB-53 como cartão | 53 |
| Planejar dieta por porções (celular, uma refeição por vez) | `ui/nutri/dieta` · `PlanejadorPorcoesTela` | `A-Planejador-Porcoes.dc.html` | **desenhada**, falta implementar | 54 |
| Planejar dieta por porções (tablet, grade e metas) | idem expandida | `T-Planejador-Porcoes.dc.html` | **desenhada**, falta implementar | 54 |
| Biblioteca de modelos de dieta | `ui/nutri/dieta` · `ModelosDietaTela` | `A-Modelos-Biblioteca.dc.html` | desenhada; implementada MOB-56a (editar o conteúdo fica na 56b) | 56 |
| Aplicar modelo ao paciente | `ui/nutri/dieta` · `AplicarModeloTela` | `A-Modelo-Aplicar.dc.html` | **implementada** (MOB-57) | 57 |
| Avaliação infantil e adolescente | `ui/nutri/avaliacao` (campos por faixa) | `A-Avaliacao-Infantil.dc.html` | **desenhada**, falta implementar | 58 |
| Curva de crescimento (nutri e responsável) | `ui/comum/graficos` · `CurvaCrescimento` | `P-Curva-Crescimento.dc.html` | **desenhada**, falta implementar | 59 |
| Exames: lançar (nutri) | `ui/nutri/avaliacao` · `ExamesTela` | `A-Exames-Nutri.dc.html` | **implementada** (MOB-60b; divergência: a classificação de um valor novo só aparece depois de salvar, vem do servidor) | 60 |
| Meus exames (paciente, barra de faixa) | `ui/paciente/evolucao` · `MeusExamesTela` | `P-Exames.dc.html` | **implementada** (MOB-60a) | 60 |
| Anamnese estruturada (álcool, sal, óleo) | `ui/paciente/anamnese` e `ui/nutri/anamnese` | `A-Anamnese-Estruturada.dc.html` | **desenhada**, falta implementar | 61 |
| Catálogo de gráficos | `ui/comum/graficos` | `G-Graficos.dc.html` | referência | 47 |
| PDF da avaliação (A4) | backend (BE-46) | `PDF-Avaliacao.dc.html` | referência de layout | BE-46 |

Ainda **sem artboard** (pedir ao Design antes de implementar): lista de substituição para consulta do paciente (MOB-55) e rosca de macros do dia e da refeição do paciente (MOB-55), micronutrientes e adequação à DRI (aba do planejador, MOB-54), suplementos e produtos no planejador (MOB-54), "salvar dieta como modelo" e editar modelo (MOB-56), PDFs da dieta e das listas de substituição (BE-50).
