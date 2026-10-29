# Dieta+ — visão geral do produto

> Documento comum aos 3 repositórios (`dieta-backend`, `dieta-mobile`, `dieta-frontend`).
> A cópia oficial fica em `dieta-backend/docs/arquitetura/`. Ao mudar, atualize as 3 cópias no mesmo dia.

## O que é

App para **nutricionistas** acompanharem seus **pacientes**. Substitui a planilha Excel + PDF que a nutri usa hoje:
a nutri cadastra o paciente, registra avaliações, monta a dieta por porções de grupos de alimentos e publica;
o paciente recebe a dieta no celular, com lembretes, troca de alimentos equivalentes e lista de compras.

Começou para uma nutricionista (a tia do Guilherme), mas é **multi-nutricionista com assinatura** desde o início.

## Aplicações

| Repositório | O que é | Quem usa | Fase |
| --- | --- | --- | --- |
| `dieta-backend` | API REST em Java 25 + Spring Boot 4.1, Postgres 18, Liquibase. Única fonte de verdade e de regras. | Todos os clientes | MVP |
| `dieta-mobile` | App Kotlin Multiplatform + Compose Multiplatform. Um app só, com perfil nutri e perfil paciente. Android, iOS e Desktop (Windows/Mac/Linux). Offline-first. | Paciente (celular), nutri (celular, tablet, desktop) | MVP |
| `dieta-frontend` | Site em Angular (**plano B**: fica parado e será arquivado depois do ponto de decisão do MOB-43). | — | Arquivado |
| web em KMP | Alvo `wasmJs` do `dieta-mobile` (Compose para web, online-only). | Nutri e paciente | P1 (MOB-40 a MOB-46) |

```
 ┌──────────────┐   HTTPS/JSON (REST /api/v1)   ┌─────────────────────┐
 │ dieta-mobile │ ───────────────────────────▶ │                     │      ┌─────────────┐
 │ Android/iOS/ │ ◀── sync incremental ─────── │   dieta-backend     │ ───▶ │ Postgres 18 │
 │ Desktop      │                               │   (monólito modular)│      └─────────────┘
 └──────┬───────┘                               │                     │ ───▶ Storage S3 (PDF, fotos)
        │ SQLite local (SQLDelight)             │                     │ ───▶ FCM (push)
        │                                       │                     │ ───▶ E-mail (SMTP)
 ┌──────────────┐   HTTPS/JSON (REST /api/v1)   │                     │ ◀─── Webhook do gateway de pagamento
 │dieta-frontend│ ───────────────────────────▶ │                     │
 │ Angular      │                               └─────────────────────┘
 └──────────────┘          Agenda Google/Apple ──▶ GET /cal/{token}.ics (feed público por token)
```

## Princípios (valem para todo código)

1. **O backend decide.** App e site só mostram. Permissão, plano, limite e regra de negócio são validados no backend em toda requisição.
2. **Dados de saúde são sensíveis (LGPD).** Consentimento no vínculo, acesso só da nutri vinculada, log de acesso, exportação e exclusão.
3. **Isolamento por nutricionista.** Todo dado clínico tem dono (`nutricionista_id`). Nenhuma consulta sem esse filtro.
4. **Offline-first no app.** O app lê do banco local e sincroniza. A rede nunca bloqueia a leitura da dieta.
5. **Simples primeiro.** Monólito modular, sem fila externa, sem microsserviços. RabbitMQ só quando o volume pedir.
6. **Tudo em português** no domínio: nomes de tabelas, classes de domínio, mensagens e docs. Termos técnicos (controller, repository, ViewModel) ficam em inglês.

## Roadmap

As tarefas detalhadas estão no `docs/arquitetura/plano-de-acao.md` de cada repositório, com prefixo `BE-`, `MOB-` e `WEB-`.

| Fase | Entrega para o usuário | Backend | Mobile | Web |
| --- | --- | --- | --- | --- |
| 0. Fundação | Projetos, CI, schema inicial | feito | feito | feito |
| 1. Contas e vínculo | Nutri cria conta (plano Teste), convida paciente; paciente aceita com consentimento | BE-01 a BE-08 | MOB-01 a MOB-08 | — |
| 2. Alimentos e dieta | Nutri monta e publica a dieta; paciente vê a dieta do dia (offline) e troca alimentos | BE-09 a BE-14 | MOB-09 a MOB-15 | — |
| 3. Rotina do paciente | Lembretes locais, check-in de refeição, água, lista de compras, agenda .ics | BE-15 a BE-18 | MOB-16 a MOB-21 | — |
| 4. Acompanhamento | Avaliação (IMC calculado), anamnese, evolução, agenda de consultas, push | BE-19 a BE-24 | MOB-22 a MOB-28 | — |
| 5. Offline da nutri | Nutri edita sem internet (desktop/tablet) com sync e conflitos | BE-25 | MOB-29 a MOB-31 | — |
| 6. Cobrança e LGPD | Assinatura paga (gateway), PDF, exportar/excluir dados, publicação nas lojas | BE-26 a BE-32 | MOB-32 a MOB-35 | — |
| P1. Web | Nutri e paciente no navegador, como alvo `wasmJs` do app (online) | — | — | BE-37, MOB-40 a MOB-46 |
| P2. Crescimento | Troca de nutri com cópia de histórico, clínicas, vitrine de nutris, consultoria paga, anúncios, fórmulas da planilha (7 dobras), RabbitMQ | a definir | a definir | a definir |

Cada fase termina com algo que a nutri real consegue usar. As fases 1 a 3 já substituem a entrega da dieta em PDF.

## Decisões tomadas (com o Guilherme)

| Tema | Decisão |
| --- | --- |
| Repositórios | 3 repositórios separados (não gosta de monorepo) |
| Backend | Java 25, Spring Boot 4.1.2, Gradle Kotlin DSL, pacote `com.br.dietamais` |
| Banco | Postgres 18, migrações com Liquibase (não Flyway) |
| Mobile | KMP + Compose Multiplatform (template Shared UI), Android + iOS + Desktop, pacote `org.br.dietamais` |
| Web | Fora do MVP; depois como alvo `wasmJs` do KMP no `dieta-mobile` (decisão de 29/10/2026), servida na mesma origem da API |
| Firebase | Só FCM para push |
| Mensageria | Nenhuma no MVP (Spring scheduling + tabela de pendências); RabbitMQ depois; Kafka descartado |
| Plano | R$ 29,90/mês até 20 pacientes; Teste com 5 pacientes; valores em tabela do banco |
| Paciente | Conta global, pode ter várias nutris; grátis quando vem de uma nutri |
| Clínica | Modelada como `organizacao` desde já (nutri solo = organização de 1) |
| Planilha | Fórmulas (7 dobras etc.) ficam para depois; MVP calcula só IMC e guarda o resto digitado |
| Hospedagem | VPS com Docker Compose, a definir |

## Suposições do PO (a confirmar, não bloqueiam)

Decididas sem consultar para não travar o trabalho. Cada uma está marcada como `[SUPOSIÇÃO]` onde aparece.

| # | Suposição | Onde impacta |
| --- | --- | --- |
| S1 | Teste dura **30 dias** (campo `plano.duracao_dias`, configurável). Depois vira só leitura até assinar. | RN-ASS-02 |
| S2 | Carência de **7 dias** em atraso antes de bloquear edição da nutri. | RN-ASS-04 |
| S3 | Gateway de pagamento: **Asaas** (PIX, cartão, boleto recorrente, sandbox gratuito). Troca fácil porque fica atrás de uma interface. | BE-26 |
| S4 | Assinatura é contratada **fora do app** (checkout hospedado do gateway, link por e-mail/web). O app nunca vende, para evitar a taxa das lojas. Conferir regras da Apple/Google antes de publicar. | RN-ASS-06 |
| S5 | Convite: código de 8 caracteres + link, validade 7 dias, uso único. | RN-VIN-02 |
| S6 | Exclusão de prontuário pela nutri é **lógica** (soft delete) com retenção, até confirmar a exigência do CFN. | RN-LGPD-04 |
| S7 | Desktop é pensado para a **nutri** (consultório sem internet); paciente usa celular. | Mobile |
| S8 | Troca de nutri com cópia do histórico fica para P2; no MVP o modelo já permite várias nutris por paciente. | RN-VIN-06 |
| S9 | Paciente com mais de uma nutri escolhe qual dieta está seguindo ("dieta em uso"); padrão é a última publicada. | RN-DIE-07 |
| S10 | Login próprio (Spring Security + JWT); sem Keycloak e sem Firebase Auth. | integracao.md |

## Onde está cada coisa

| Documento | Conteúdo |
| --- | --- |
| `visao-geral.md` | Este arquivo |
| `integracao.md` | Contrato entre as aplicações: API, autenticação, erros, sync offline, push, agenda |
| `regras-de-negocio.md` | Regras numeradas (RN-xxx) que o backend aplica e os clientes respeitam |
| `funcionalidades-mvp.md` | Funcionalidades do MVP com critérios de aceite |
| `backend.md` / `mobile.md` / `web.md` | Arquitetura interna do repositório |
| `plano-de-acao.md` | Tarefas ordenadas do repositório, com dependências e critério de pronto |
| `plano-de-execucao.md` (backend e mobile) | Ordem conjunta: ondas, marcos/releases, Gitflow, contrato primeiro, tarefas de design e do Guilherme |
| `diagramas.md` | Diagramas Mermaid (contexto, containers, sequências, implantação) |
| `docs/design/` (só no mobile) | Telas, componentes e plano de UI (PO de design) |

Referências externas:
- Doc de arquitetura original (análise dos PDFs): https://claude.ai/artifact/SwSyenPavbbhp3q2ieGYVG
- Canvas de design: https://claude.ai/artifact/6thej5zGsTo81ZosiigGxC
