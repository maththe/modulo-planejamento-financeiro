package br.com.agricultura.planejamentofinanceiro.controller;

import br.com.agricultura.planejamentofinanceiro.domain.entity.Usuario;
import br.com.agricultura.planejamentofinanceiro.dto.LancamentoDtos.AtualizarRateiosRequest;
import br.com.agricultura.planejamentofinanceiro.dto.LancamentoDtos.LancamentoResponse;
import br.com.agricultura.planejamentofinanceiro.security.AutorizacaoService;
import br.com.agricultura.planejamentofinanceiro.security.UsuarioAutenticado;
import br.com.agricultura.planejamentofinanceiro.service.LancamentoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/lancamentos/{id}/rateios")
@Tag(name = "Rateios de Custos", description = "Distribuição de custos entre culturas")
public class CustoRateioController {

    private final LancamentoService lancamentoService;
    private final AutorizacaoService autorizacaoService;

    public CustoRateioController(LancamentoService lancamentoService, AutorizacaoService autorizacaoService) {
        this.lancamentoService = lancamentoService;
        this.autorizacaoService = autorizacaoService;
    }

    @PostMapping
    @Operation(summary = "Define ou atualiza o rateio do custo entre culturas (soma deve igualar o custo total)")
    public LancamentoResponse atualizarRateios(
            @AuthenticationPrincipal UsuarioAutenticado auth,
            @PathVariable UUID id,
            @Valid @RequestBody AtualizarRateiosRequest request) {
        Usuario produtor = autorizacaoService.resolverProdutorParaEscrita(auth);
        return lancamentoService.atualizarRateios(produtor, id, request);
    }
}
