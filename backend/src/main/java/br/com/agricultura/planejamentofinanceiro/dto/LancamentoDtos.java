package br.com.agricultura.planejamentofinanceiro.dto;

import br.com.agricultura.planejamentofinanceiro.domain.enums.CategoriaLancamento;
import br.com.agricultura.planejamentofinanceiro.domain.enums.ClassificacaoCusto;
import br.com.agricultura.planejamentofinanceiro.domain.enums.TipoLancamento;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public final class LancamentoDtos {

    private LancamentoDtos() {
    }

    public record RateioItemRequest(
            @NotNull(message = "Informe a cultura do rateio.") UUID culturaId,
            @NotNull(message = "Informe o valor rateado.")
            @DecimalMin(value = "0.00", inclusive = true, message = "O valor rateado não pode ser negativo.")
            BigDecimal valorRateado
    ) {
    }

    public record RateioItemResponse(
            UUID id,
            UUID culturaId,
            String culturaNome,
            BigDecimal valorRateado,
            BigDecimal percentual
    ) {
    }

    public record LancamentoRequest(
            String chaveIdempotencia,
            UUID culturaId,
            @NotNull(message = "Informe o tipo (ENTRADA ou SAIDA).") TipoLancamento tipo,
            ClassificacaoCusto classificacaoCusto,
            @NotNull(message = "Informe a categoria.") CategoriaLancamento categoria,
            String descricao,
            @NotNull(message = "Informe o valor.")
            @DecimalMin(value = "0.01", message = "O valor deve ser maior que zero.")
            BigDecimal valor,
            BigDecimal quantidade,
            @NotNull(message = "Informe a data do lançamento.") LocalDate dataLancamento,
            @Valid List<RateioItemRequest> rateios
    ) {
    }

    public record LancamentoResponse(
            UUID id,
            String chaveIdempotencia,
            UUID produtorId,
            UUID culturaId,
            String culturaNome,
            TipoLancamento tipo,
            ClassificacaoCusto classificacaoCusto,
            CategoriaLancamento categoria,
            String categoriaRotulo,
            String descricao,
            BigDecimal valor,
            BigDecimal quantidade,
            LocalDate dataLancamento,
            String anoMes,
            Long versao,
            BigDecimal saldoAcumulado,
            List<RateioItemResponse> rateios,
            LocalDateTime atualizadoEm
    ) {
    }

    public record CadernoDigitalResponse(
            String anoMes,
            boolean periodoFechado,
            BigDecimal totalEntradas,
            BigDecimal totalSaidas,
            BigDecimal saldoPeriodo,
            List<LancamentoResponse> lancamentos
    ) {
    }

    public record CategoriaFrequenciaDto(
            CategoriaLancamento categoria,
            String rotulo,
            long quantidadeUso
    ) {
    }

    public record SugestaoCategoriaResponse(
            TipoLancamento tipo,
            CategoriaLancamento categoriaSugerida,
            String rotuloSugerido,
            List<CategoriaFrequenciaDto> rankingHistorico
    ) {
    }

    public record LancamentoRecorrenteRequest(
            UUID culturaId,
            @NotNull TipoLancamento tipo,
            ClassificacaoCusto classificacaoCusto,
            @NotNull CategoriaLancamento categoria,
            String descricao,
            @NotNull @DecimalMin(value = "0.01") BigDecimal valorPadrao,
            @NotNull @Min(1) @Max(28) Integer diaVencimento
    ) {
    }

    public record LancamentoRecorrenteResponse(
            UUID id,
            UUID culturaId,
            String culturaNome,
            TipoLancamento tipo,
            ClassificacaoCusto classificacaoCusto,
            CategoriaLancamento categoria,
            String descricao,
            BigDecimal valorPadrao,
            Integer diaVencimento,
            boolean ativo
    ) {
    }

    public record AtualizarRateiosRequest(
            @NotNull @Valid List<RateioItemRequest> rateios
    ) {
    }
}
