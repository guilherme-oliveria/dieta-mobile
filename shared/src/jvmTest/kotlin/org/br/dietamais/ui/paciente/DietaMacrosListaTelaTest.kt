package org.br.dietamais.ui.paciente

import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import org.br.dietamais.data.remoto.*
import org.br.dietamais.data.remoto.dto.*
import org.br.dietamais.dominio.rotina.gruposListaPaciente
import org.br.dietamais.ui.paciente.dieta.*
import org.br.dietamais.ui.paciente.substituicao.*
import org.br.dietamais.ui.theme.DietaTheme
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.*
import kotlin.uuid.Uuid
import kotlinx.datetime.LocalDate

@OptIn(ExperimentalTestApi::class)
class DietaMacrosListaTelaTest {
    private fun estado(): EstadoDietaPaciente {
        val original = JsonContrato.decodeFromString<DietasPacienteResposta>(recursoContrato("fase-2/listarMinhasDietas/resposta.json")).itens.single()
        val ref = original.refeicoes.single()
        val alimento = original.listaSubstituicao.alimentos.single()
        val d = original.copy(refeicoes = listOf(ref.copy(itens = ref.itens.map { it.copy(grupoNome = "Pães") })),
            listaSubstituicao = original.listaSubstituicao.copy(alimentos = listOf(alimento, alimento.copy(id = Uuid.parse("019a0f32-5000-7000-8000-000000000012"), nome = "Substituto fictício"))))
        return EstadoDietaPaciente(carregado = true, dieta = d, refeicoes = d.refeicoes, gruposLista = gruposListaPaciente(d.listaSubstituicao, d.refeicoes), grupoListaAberto = ref.itens.single().grupoId,
            dia = LocalDate(2026, 11, 1), hoje = LocalDate(2026, 11, 1))
    }
    @Test fun macrosMostramGramasEPercentuaisETabelaSemMeta() = runSkikoComposeUiTest(size = Size(390f, 844f)) {
        setContent { DietaTheme { DietaPacienteTela(estado(), {}, {}) } }
        onNodeWithText("Macros do dia").assertIsDisplayed()
        onNodeWithText("Ver como tabela").performClick()
        onNodeWithText("25 g · 68%").assertIsDisplayed()
        onAllNodesWithText("meta", substring = true).assertCountEquals(0)
    }
    @Test fun listaVaziaExplicaQueFicaDisponivelOffline() = runSkikoComposeUiTest(size = Size(390f, 844f)) {
        setContent { DietaTheme { ListaSubstituicaoTela(estado().copy(gruposLista = emptyList(), semConexao = true), AcoesListaPaciente()) } }
        onNodeWithText("Sua nutri ainda não montou sua lista").assertIsDisplayed()
    }
    @Test fun trocarNoDetalheIdentificaOItemCorreto() = runSkikoComposeUiTest(size = Size(390f, 844f)) {
        val e = estado(); val r = e.refeicoes.single(); var alvo: Pair<Uuid, Uuid>? = null
        setContent { DietaTheme { DietaPacienteTela(e.copy(refeicaoAberta = r.id), {}, {}, aoTrocar = { ref, item -> alvo = ref to item }) } }
        onNodeWithText("Trocar").performScrollTo().performClick()
        assertEquals(r.id to r.itens.single().id, alvo)
    }
    @Test fun trocaPorGrupoNaoMostraGramasEquivalentesSemReferencia() = runSkikoComposeUiTest(size = Size(390f, 844f)) {
        val e = estado(); val r = e.refeicoes.single()
        val item = r.itens.single().copy(alimentoId = null, opcao = 2)
        val nova = r.copy(itens = listOf(item))
        val d = e.dieta!!.copy(refeicoes = listOf(nova))
        setContent { DietaTheme { TrocaTela(e.copy(dieta = d, refeicoes = listOf(nova), troca = EstadoTroca(r.id, itemId = item.id)), { _, _ -> }, {}, {}) } }
        onNodeWithText("A quantidade equivalente deste grupo não veio na dieta. Confirme a medida com a sua nutri.").assertIsDisplayed()
        onAllNodesWithText("1 unidade fictícia", substring = true).assertCountEquals(0)
    }
    @Test fun listaMostraSomenteGrupoAbertoEPermiteBuscarOffline() = runSkikoComposeUiTest(size = Size(390f, 844f)) {
        var estado by mutableStateOf(estado().copy(semConexao = true))
        setContent { DietaTheme { ListaSubstituicaoTela(estado, AcoesListaPaciente(aoBuscar = { estado = estado.copy(buscaLista = it) })) } }
        onNodeWithText("Alimento fictício").assertIsDisplayed()
        onNodeWithContentDescription("Buscar alimento").performTextInput("inexistente")
        onNodeWithText("Nenhum alimento encontrado").assertIsDisplayed()
    }
    @Test fun capturasNosTresTamanhosEFonteAmpliada() {
        listOf(Triple(390, 844, 1f), Triple(820, 1180, 1f), Triple(1180, 820, 1f), Triple(390, 844, 2f)).forEach { (w,h,f) ->
            listOf("dieta", "refeicao", "lista", "vazio").forEach { tela ->
                runSkikoComposeUiTest(size = Size(w.toFloat(), h.toFloat()), density = Density(1f, f)) {
                    val e = estado()
                    setContent { DietaTheme {
                        if (tela == "lista" || tela == "vazio") ListaSubstituicaoTela(if (tela == "vazio") e.copy(gruposLista = emptyList()) else e, AcoesListaPaciente())
                        else DietaPacienteTela(if (tela == "refeicao") e.copy(refeicaoAberta = e.refeicoes.single().id) else e, {}, {})
                    } }
                    waitForIdle()
                    val alvo = when (tela) {
                        "dieta" -> "Macros do dia"
                        "refeicao" -> "Macros desta refeição"
                        "lista" -> "Alimento fictício"
                        else -> "Sua nutri ainda não montou sua lista"
                    }
                    onNodeWithText(alvo).performScrollTo().assertIsDisplayed()
                    val arquivo = File("build/reports/MOB-55/$tela-$w-fonte-$f.png")
                    arquivo.parentFile.mkdirs(); ImageIO.write(onRoot().captureToImage().toAwtImage(), "png", arquivo)
                }
            }
        }
    }
}
