# MOB-09 — evidências visuais

Capturas de Compose desktop com dados fictícios do contrato oficial. O indicador offline foi conferido em 390×844, 820×1180 e 1180×820, além de 390×844 com fonte 200%. Usa a faixa offline e os tokens/componentes existentes de `docs/design/`.

`shell-390-fonte-2.0.png` usa o shell completo, DI, SQLite em memória e respostas HTTP oficiais. O teste navega para Pacientes, rola até o paciente e verifica o nome dentro do viewport acima da navegação; depois torna a ação global “Atualizar dados” visível e captura. O cabeçalho autenticado tem altura máxima e rolagem; filtros e lista compartilham a mesma rolagem para não tornar pacientes inacessíveis com fonte ampliada.

Os testes também verificam ação desabilitada durante sync e retentativa de `TAXA_EXCEDIDA`. Capturas não comprovam leitor de tela, callback nativo de rede ou agendamento do sistema operacional; esses pontos precisam de validação em dispositivo/Mac.
