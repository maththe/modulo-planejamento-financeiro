package br.com.agricultura.planejamentofinanceiro.dto;

import br.com.agricultura.planejamentofinanceiro.domain.enums.CategoriaLancamento;
import br.com.agricultura.planejamentofinanceiro.domain.enums.ClassificacaoCusto;
import br.com.agricultura.planejamentofinanceiro.domain.enums.TipoLancamento;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public final class SyncDtos {

    private SyncDtos() {
    }

    public record SyncItemRequest(
            @NotBlank(message = "Informe a chave de idempotência.") String chaveIdempotencia,
            UUID culturaId,
            @NotNull(message = "Informe o tipo do lançamento.") TipoLancamento tipo,
            ClassificacaoCusto classificacaoCusto,
            @NotNull(message = "Informe a categoria.") CategoriaLancamento categoria,
            String descricao,
            @NotNull(message = "Informe o valor.") BigDecimal valor,
            BigDecimal quantidade,
            @NotNull(message = "Informe a data do lançamento.") LocalDate dataLancamento,
            Long versaoCliente,
            LocalDateTime clienteAtualizadoEm,
            @Valid List<LancamentoDtos.RateioItemRequest> rateios
    ) {
    }

    public record SyncLoteRequest(
            @NotEmpty(message = "O lote deve conter ao menos um item.")
            List<SyncItemRequest> itens
    ) {
    }

    public record SyncItemResultadoDto(
            String chaveIdempotencia,
            String status,
            UUID lancamentoId,
            Long versaoAtual,
            String mensagem
    ) {
    }

    public record SyncLoteResponse(
            int totalRecebidos,
            int sincronizados,
            int idempotentes,
            int conflitos,
            int erros,
            List<SyncItemResultadoDto> resultados
    ) {
    }

    public record ConflitoSyncDto(
            UUID id,
            String chaveIdempotencia,
            UUID lancamentoId,
            Long versaoServidor,
            Long versaoCliente,
            String regraAplicada,
            String payloadAnterior,
            String payloadNovo,
            LocalDateTime resolvidoEm
    ) {
    }
}
