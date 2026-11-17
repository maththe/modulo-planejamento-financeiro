package br.com.agricultura.planejamentofinanceiro.controller;

import br.com.agricultura.planejamentofinanceiro.domain.entity.Usuario;
import br.com.agricultura.planejamentofinanceiro.domain.enums.TipoLancamento;
import br.com.agricultura.planejamentofinanceiro.dto.LancamentoDtos.CadernoDigitalResponse;
import br.com.agricultura.planejamentofinanceiro.dto.LancamentoDtos.LancamentoRequest;
import br.com.agricultura.planejamentofinanceiro.dto.LancamentoDtos.LancamentoResponse;
import br.com.agricultura.planejamentofinanceiro.dto.LancamentoDtos.SugestaoCategoriaResponse;
import br.com.agricultura.planejamentofinanceiro.security.AutorizacaoService;
import br.com.agricultura.planejamentofinanceiro.security.UsuarioAutenticado;
import br.com.agricultura.planejamentofinanceiro.service.LancamentoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.YearMonth;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/lancamentos")
@Tag(name = "Lançamentos", description = "Caderno digital, registro rápido, sugestão de categoria e CRUD com exclusão lógica")
public class LancamentoController {

    private final LancamentoService lancamentoService;
    private final AutorizacaoService autorizacaoService;

    public LancamentoController(LancamentoService lancamentoService, AutorizacaoService autorizacaoService) {
        this.lancamentoService = lancamentoService;
        this.autorizacaoService = autorizacaoService;
    }

    @GetMapping
    @Operation(summary = "Consulta o caderno digital do mês com saldo acumulado")
    public CadernoDigitalResponse listarPorPeriodo(
            @AuthenticationPrincipal UsuarioAutenticado auth,
            @RequestParam(required = false) String anoMes,
            @RequestParam(required = false) UUID produtorId) {
        Usuario produtor = autorizacaoService.resolverProdutorParaLeitura(auth, produtorId);
        String mes = (anoMes == null || anoMes.isBlank()) ? YearMonth.now().toString() : anoMes;
        return lancamentoService.listarCaderno(produtor.getId(), mes);
    }

    @GetMapping("/sugestao-categoria")
    @Operation(summary = "Sugere a categoria mais frequente no histórico do produtor")
    public SugestaoCategoriaResponse sugerirCategoria(
            @AuthenticationPrincipal UsuarioAutenticado auth,
            @RequestParam TipoLancamento tipo,
            @RequestParam(required = false) UUID produtorId) {
        Usuario produtor = autorizacaoService.resolverProdutorParaLeitura(auth, produtorId);
        return lancamentoService.sugerirCategoria(produtor.getId(), tipo);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria um novo lançamento financeiro")
    public LancamentoResponse criar(
            @AuthenticationPrincipal UsuarioAutenticado auth,
            @Valid @RequestBody LancamentoRequest request) {
        Usuario produtor = autorizacaoService.resolverProdutorParaEscrita(auth);
        return lancamentoService.criar(produtor, request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza um lançamento existente (somente período aberto)")
    public LancamentoResponse atualizar(
            @AuthenticationPrincipal UsuarioAutenticado auth,
            @PathVariable UUID id,
            @Valid @RequestBody LancamentoRequest request) {
        Usuario produtor = autorizacaoService.resolverProdutorParaEscrita(auth);
        return lancamentoService.atualizar(produtor, id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Exclusão lógica de lançamento")
    public void excluir(
            @AuthenticationPrincipal UsuarioAutenticado auth,
            @PathVariable UUID id) {
        Usuario produtor = autorizacaoService.resolverProdutorParaEscrita(auth);
        lancamentoService.excluirLogicamente(produtor, id);
    }
}
