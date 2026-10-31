// Regressão: as abas Avaliações e Evolução da ficha do paciente (perfil nutri) fechavam o app (app) ou ficavam em branco (web),
// porque rolavam por dentro de uma ficha que já rola (o Compose recusa rolagem dentro de rolagem).
import { expect, test } from "@playwright/test";
import { botao, clicar, entrar, NUTRI_EXEMPLO, tocar, veTexto } from "./apoio.ts";

test("nutri abre Avaliações e Evolução na ficha e vê o conteúdo das abas", async ({ page }) => {
  await entrar(page, NUTRI_EXEMPLO);
  await expect(page.getByRole("heading", { name: "Início" })).toBeAttached({ timeout: 30_000 });
  await tocar(page, "Pacientes");
  await clicar(page, page.getByRole("button", { name: /Maria Exemplo/ }));
  await expect(page.getByRole("heading", { name: "Maria Exemplo" })).toBeAttached();

  await tocar(page, "Avaliações");
  await expect(botao(page, "Nova avaliação")).toBeAttached({ timeout: 20_000 });

  await tocar(page, "Evolução");
  await expect(page.getByText("Ainda não há medidas para mostrar.").or(botao(page, "Peso")).first()).toBeAttached({ timeout: 20_000 });

  await tocar(page, "Avaliações");
  await expect(botao(page, "Nova avaliação")).toBeAttached();
});
