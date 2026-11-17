package br.com.agricultura.planejamentofinanceiro.dto;

import br.com.agricultura.planejamentofinanceiro.domain.enums.StatusSemaforo;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public final class IndicadoresDtos {

    private IndicadoresDtos() {
    }

    public record IndicadorCulturaDto(
            UUID culturaId,
            String culturaNome,
            String unidade,
            BigDecimal quantidadeProduzida,
            BigDecimal receita,
            BigDecimal custosVariaveis,
            BigDecimal custosFixosRateados,
            BigDecimal custoTotal,
            BigDecimal custoUnitario,
            BigDecimal margemContribuicao,
            BigDecimal pontoEquilibrio,
            boolean margemNulaOuNegativa,
            BigDecimal resultado
    ) {
    }

    public record IndicadoresPeriodoDto(
            String anoMes,
            boolean periodoFechado,
            BigDecimal receitaTotal,
            BigDecimal custosFixos,
            BigDecimal custosVariaveis,
            BigDecimal custosTotais,
            BigDecimal margemContribuicao,
            BigDecimal pontoEquilibrio,
            BigDecimal resultado,
            boolean margemNulaOuNegativa,
            String avisoMargem,
            StatusSemaforo statusSemaforo,
            List<IndicadorCulturaDto> indicadoresPorCultura
    ) {
    }

    public record SerieHistoricaItemDto(
            String anoMes,
            boolean periodoFechado,
            BigDecimal receitaTotal,
            BigDecimal custosFixos,
            BigDecimal custosVariaveis,
            BigDecimal custosTotais,
            BigDecimal margemContribuicao,
            BigDecimal pontoEquilibrio,
            BigDecimal resultado,
            boolean margemNulaOuNegativa,
            StatusSemaforo statusSemaforo
    ) {
    }

    public record FechamentoPeriodoResponse(
            UUID id,
            String anoMes,
            BigDecimal receitaTotal,
            BigDecimal custosFixos,
            BigDecimal custosVariaveis,
            BigDecimal custoTotal,
            BigDecimal resultado,
            BigDecimal margemContribuicao,
            BigDecimal pontoEquilibrio,
            StatusSemaforo statusSemaforo,
            LocalDateTime fechadoEm
    ) {
    }
}
