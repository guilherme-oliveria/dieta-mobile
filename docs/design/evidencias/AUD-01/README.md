# Evidência AUD-01: artboard (esquerda) × app (direita), 390 dp

Gerado pelos testes de evidência (`./gradlew :shared:jvmTest`, PNGs em `shared/build/reports/`). Estado após as PRs #73 a #77.

- `avaliacao-infantil.png`: barra superior, unidade dentro do campo e rodapé fixo batem; falta o painel "Resultados ao vivo" (depende de BE-55b).
- `anamnese.png`: o app mostra a anamnese do paciente (barra e rodapé fixo); o artboard é o passo de hábitos da nutri.
- `dieta-macros.png` e `lista-substituicao.png`: as capturas de teste usam a tela interna sem a navegação inferior; a lista ainda não é acordeão como no artboard.
