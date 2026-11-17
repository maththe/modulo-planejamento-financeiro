package br.com.agricultura.planejamentofinanceiro.controller;

import br.com.agricultura.planejamentofinanceiro.domain.entity.Usuario;
import br.com.agricultura.planejamentofinanceiro.dto.SyncDtos.ConflitoSyncDto;
import br.com.agricultura.planejamentofinanceiro.dto.SyncDtos.SyncLoteRequest;
import br.com.agricultura.planejamentofinanceiro.dto.SyncDtos.SyncLoteResponse;
import br.com.agricultura.planejamentofinanceiro.security.AutorizacaoService;
import br.com.agricultura.planejamentofinanceiro.security.UsuarioAutenticado;
import br.com.agricultura.planejamentofinanceiro.service.SincronizacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sync")
@Tag(name = "Sincronização Offline", description = "Envio em lote com idempotência, lote parcial e resolução de conflitos")
public class SincronizacaoController {

    private final SincronizacaoService sincronizacaoService;
    private final AutorizacaoService autorizacaoService;

    public SincronizacaoController(
            SincronizacaoService sincronizacaoService,
            AutorizacaoService autorizacaoService) {
        this.sincronizacaoService = sincronizacaoService;
        this.autorizacaoService = autorizacaoService;
    }

    @PostMapping("/lotes")
    @Operation(summary = "Sincroniza um lote de lançamentos offline com chave de idempotência")
    public SyncLoteResponse sincronizarLote(
            @AuthenticationPrincipal UsuarioAutenticado auth,
            @Valid @RequestBody SyncLoteRequest request) {
        Usuario produtor = autorizacaoService.resolverProdutorParaEscrita(auth);
        return sincronizacaoService.processarLote(produtor, request);
    }

    @GetMapping("/conflitos")
    @Operation(summary = "Lista o histórico de conflitos resolvidos pela regra 'última escrita vence'")
    public List<ConflitoSyncDto> listarConflitos(
            @AuthenticationPrincipal UsuarioAutenticado auth,
            @RequestParam(required = false) UUID produtorId) {
        Usuario produtor = autorizacaoService.resolverProdutorParaLeitura(auth, produtorId);
        return sincronizacaoService.listarConflitos(produtor.getId());
    }
}
