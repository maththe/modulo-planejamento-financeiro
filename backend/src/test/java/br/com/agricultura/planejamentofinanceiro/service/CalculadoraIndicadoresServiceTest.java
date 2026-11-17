package br.com.agricultura.planejamentofinanceiro.service;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.agricultura.planejamentofinanceiro.domain.entity.Cultura;
import br.com.agricultura.planejamentofinanceiro.domain.entity.Lancamento;
import br.com.agricultura.planejamentofinanceiro.domain.entity.ProducaoMensal;
import br.com.agricultura.planejamentofinanceiro.domain.entity.RateioCusto;
import br.com.agricultura.planejamentofinanceiro.domain.enums.CategoriaLancamento;
import br.com.agricultura.planejamentofinanceiro.domain.enums.ClassificacaoCusto;
import br.com.agricultura.planejamentofinanceiro.domain.enums.StatusSemaforo;
import br.com.agricultura.planejamentofinanceiro.domain.enums.TipoLancamento;
import br.com.agricultura.planejamentofinanceiro.dto.IndicadoresDtos.IndicadorCulturaDto;
import br.com.agricultura.planejamentofinanceiro.dto.IndicadoresDtos.IndicadoresPeriodoDto;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CalculadoraIndicadoresServiceTest {

    private CalculadoraIndicadoresService service;
    private Cultura alface;
    private Cultura tomate;

    @BeforeEach
    void setUp() {
        service = new CalculadoraIndicadoresService(null, null, null, null, null);

        alface = new Cultura();
        alface.setId(UUID.fromString("33333333-3333-3333-3333-333333333301"));
        alface.setNome("Alface");
        alface.setUnidade("maço");
        alface.setIcone("Leaf");
        alface.setAtivo(true);

        tomate = new Cultura();
        tomate.setId(UUID.fromString("33333333-3333-3333-3333-333333333303"));
        tomate.setNome("Tomate");
        tomate.setUnidade("kg");
        tomate.setIcone("Orange");
        tomate.setAtivo(true);
    }

    @Test
    void deveCalcularSemaforoVerdeECustoUnitarioQuandoReceitaSuperaPontoEquilibrioEmDezPorCento() {
        Lancamento vendaAlface = criarEntrada(alface, "2000.00", "400.00");
        Lancamento vendaTomate = criarEntrada(tomate, "1000.00", "100.00");
        Lancamento sementes = criarSaidaComRateio(
                ClassificacaoCusto.VARIAVEL, CategoriaLancamento.SEMENTES, "900.00",
                alface, "500.00", tomate, "400.00");
        Lancamento agua = criarSaidaComRateio(
                ClassificacaoCusto.FIXO, CategoriaLancamento.AGUA, "1000.00",
                alface, "500.00", tomate, "500.00");

        ProducaoMensal prodAlface = criarProducao(alface, "500.00");
        ProducaoMensal prodTomate = criarProducao(tomate, "150.00");

        IndicadoresPeriodoDto dto = service.calcularIndicadores(
                "2026-11",
                false,
                List.of(alface, tomate),
                List.of(vendaAlface, vendaTomate, sementes, agua),
                List.of(prodAlface, prodTomate)
        );

        assertThat(dto.receitaTotal()).isEqualByComparingTo("3000.00");
        assertThat(dto.custosVariaveis()).isEqualByComparingTo("900.00");
        assertThat(dto.custosFixos()).isEqualByComparingTo("1000.00");
        assertThat(dto.custosTotais()).isEqualByComparingTo("1900.00");
        assertThat(dto.margemContribuicao()).isEqualByComparingTo("2100.00");
        // PE = 1000 / (2100/3000) = 1000 / 0.70 = 1428.57
        assertThat(dto.pontoEquilibrio()).isEqualByComparingTo("1428.57");
        assertThat(dto.resultado()).isEqualByComparingTo("1100.00");
        assertThat(dto.margemNulaOuNegativa()).isFalse();
        assertThat(dto.avisoMargem()).isNull();
        assertThat(dto.statusSemaforo()).isEqualTo(StatusSemaforo.VERDE);

        IndicadorCulturaDto indAlface = dto.indicadoresPorCultura().get(0);
        // Alface: CV 500 + CF 500 = 1000 / 500 = 2.00
        assertThat(indAlface.custoTotal()).isEqualByComparingTo("1000.00");
        assertThat(indAlface.custoUnitario()).isEqualByComparingTo("2.00");

        IndicadorCulturaDto indTomate = dto.indicadoresPorCultura().get(1);
        // Tomate: CV 400 + CF 500 = 900 / 150 = 6.00
        assertThat(indTomate.custoTotal()).isEqualByComparingTo("900.00");
        assertThat(indTomate.custoUnitario()).isEqualByComparingTo("6.00");
    }

    @Test
    void deveCalcularSemaforoAmareloQuandoResultadoPositivoMasPertoDoPontoDeEquilibrio() {
        // Receita 2150, CV 650, CF 1400 -> MC 1500, PE = 2006.67, 1.10 * PE = 2207.34 > 2150
        Lancamento venda = criarEntrada(alface, "2150.00", "300.00");
        Lancamento cv = criarSaidaComRateio(ClassificacaoCusto.VARIAVEL, CategoriaLancamento.ADUBO, "650.00",
                alface, "650.00", tomate, "0.00");
        Lancamento cf = criarSaidaComRateio(ClassificacaoCusto.FIXO, CategoriaLancamento.MAO_DE_OBRA, "1400.00",
                alface, "1400.00", tomate, "0.00");

        IndicadoresPeriodoDto dto = service.calcularIndicadores(
                "2026-10",
                false,
                List.of(alface, tomate),
                List.of(venda, cv, cf),
                List.of()
        );

        assertThat(dto.resultado()).isEqualByComparingTo("100.00");
        assertThat(dto.pontoEquilibrio()).isEqualByComparingTo("2006.67");
        assertThat(dto.statusSemaforo()).isEqualTo(StatusSemaforo.AMARELO);
    }

    @Test
    void deveCalcularSemaforoVermelhoQuandoResultadoNegativo() {
        Lancamento venda = criarEntrada(alface, "1200.00", "200.00");
        Lancamento cv = criarSaidaComRateio(ClassificacaoCusto.VARIAVEL, CategoriaLancamento.SEMENTES, "500.00",
                alface, "500.00", tomate, "0.00");
        Lancamento cf = criarSaidaComRateio(ClassificacaoCusto.FIXO, CategoriaLancamento.AGUA, "900.00",
                alface, "900.00", tomate, "0.00");

        IndicadoresPeriodoDto dto = service.calcularIndicadores(
                "2026-10",
                false,
                List.of(alface, tomate),
                List.of(venda, cv, cf),
                List.of()
        );

        assertThat(dto.resultado()).isEqualByComparingTo("-200.00");
        assertThat(dto.statusSemaforo()).isEqualTo(StatusSemaforo.VERMELHO);
    }

    @Test
    void deveRetornarAvisoSemDivisaoInvalidaQuandoMargemContribuicaoNula() {
        // Receita 800 == Custos Variaveis 800 => Margem de Contribuicao = 0.00
        Lancamento venda = criarEntrada(alface, "800.00", "100.00");
        Lancamento cv = criarSaidaComRateio(ClassificacaoCusto.VARIAVEL, CategoriaLancamento.ADUBO, "800.00",
                alface, "800.00", tomate, "0.00");
        Lancamento cf = criarSaidaComRateio(ClassificacaoCusto.FIXO, CategoriaLancamento.ENERGIA, "300.00",
                alface, "300.00", tomate, "0.00");

        IndicadoresPeriodoDto dto = service.calcularIndicadores(
                "2026-11",
                false,
                List.of(alface, tomate),
                List.of(venda, cv, cf),
                List.of()
        );

        assertThat(dto.margemContribuicao()).isEqualByComparingTo("0.00");
        assertThat(dto.margemNulaOuNegativa()).isTrue();
        assertThat(dto.pontoEquilibrio()).isNull();
        assertThat(dto.avisoMargem()).contains("Margem de contribuição nula ou negativa");
        assertThat(dto.statusSemaforo()).isEqualTo(StatusSemaforo.VERMELHO);
    }

    @Test
    void deveRetornarAvisoSemDivisaoInvalidaQuandoMargemContribuicaoNegativa() {
        // Receita 500 < Custos Variaveis 750 => Margem de Contribuicao = -250.00
        Lancamento venda = criarEntrada(tomate, "500.00", "60.00");
        Lancamento cv = criarSaidaComRateio(ClassificacaoCusto.VARIAVEL, CategoriaLancamento.SEMENTES, "750.00",
                alface, "0.00", tomate, "750.00");
        Lancamento cf = criarSaidaComRateio(ClassificacaoCusto.FIXO, CategoriaLancamento.AGUA, "200.00",
                alface, "0.00", tomate, "200.00");

        IndicadoresPeriodoDto dto = service.calcularIndicadores(
                "2026-11",
                false,
                List.of(alface, tomate),
                List.of(venda, cv, cf),
                List.of()
        );

        assertThat(dto.margemContribuicao()).isEqualByComparingTo("-250.00");
        assertThat(dto.margemNulaOuNegativa()).isTrue();
        assertThat(dto.pontoEquilibrio()).isNull();
        assertThat(dto.avisoMargem()).isNotBlank();
        assertThat(dto.statusSemaforo()).isEqualTo(StatusSemaforo.VERMELHO);
    }

    private Lancamento criarEntrada(Cultura cultura, String valor, String quantidade) {
        Lancamento l = new Lancamento();
        l.setId(UUID.randomUUID());
        l.setCultura(cultura);
        l.setTipo(TipoLancamento.ENTRADA);
        l.setClassificacaoCusto(ClassificacaoCusto.NAO_APLICAVEL);
        l.setCategoria(CategoriaLancamento.VENDAS);
        l.setValor(new BigDecimal(valor));
        l.setQuantidade(new BigDecimal(quantidade));
        l.setDataLancamento(LocalDate.of(2026, 11, 10));
        l.setAnoMes("2026-11");
        return l;
    }

    private Lancamento criarSaidaComRateio(
            ClassificacaoCusto classificacao,
            CategoriaLancamento categoria,
            String valorTotal,
            Cultura c1, String v1,
            Cultura c2, String v2) {
        Lancamento l = new Lancamento();
        l.setId(UUID.randomUUID());
        l.setTipo(TipoLancamento.SAIDA);
        l.setClassificacaoCusto(classificacao);
        l.setCategoria(categoria);
        l.setValor(new BigDecimal(valorTotal));
        l.setDataLancamento(LocalDate.of(2026, 11, 5));
        l.setAnoMes("2026-11");

        RateioCusto r1 = new RateioCusto();
        r1.setId(UUID.randomUUID());
        r1.setLancamento(l);
        r1.setCultura(c1);
        r1.setValorRateado(new BigDecimal(v1));
        r1.setPercentual(BigDecimal.ZERO);

        RateioCusto r2 = new RateioCusto();
        r2.setId(UUID.randomUUID());
        r2.setLancamento(l);
        r2.setCultura(c2);
        r2.setValorRateado(new BigDecimal(v2));
        r2.setPercentual(BigDecimal.ZERO);

        l.setRateios(List.of(r1, r2));
        return l;
    }

    private ProducaoMensal criarProducao(Cultura cultura, String qtd) {
        ProducaoMensal pm = new ProducaoMensal();
        pm.setId(UUID.randomUUID());
        pm.setCultura(cultura);
        pm.setAnoMes("2026-11");
        pm.setQuantidadeProduzida(new BigDecimal(qtd));
        return pm;
    }
}
