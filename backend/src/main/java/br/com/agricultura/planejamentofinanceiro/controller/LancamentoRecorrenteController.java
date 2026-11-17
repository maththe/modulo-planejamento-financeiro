package br.com.agricultura.planejamentofinanceiro.controller;

import br.com.agricultura.planejamentofinanceiro.domain.entity.Usuario;
import br.com.agricultura.planejamentofinanceiro.dto.LancamentoDtos.LancamentoRecorrenteRequest;
import br.com.agricultura.planejamentofinanceiro.dto.LancamentoDtos.LancamentoRecorrenteResponse;
import br.com.agricultura.planejamentofinanceiro.dto.LancamentoDtos.LancamentoResponse;
import br.com.agricultura.planejamentofinanceiro.security.AutorizacaoService;
import br.com.agricultura.planejamentofinanceiro.security.UsuarioAutenticado;
import br.com.agricultura.planejamentofinanceiro.service.LancamentoRecorrenteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/lancamentos-recorrentes")
@Tag(name = "Lançamentos Recorrentes", description = "API de modelos recorrentes (sem tela de atalhos no MVP)")
public class LancamentoRecorrenteController {

    private final LancamentoRecorrenteService recorrenteService;
    private final AutorizacaoService autorizacaoService;

    public LancamentoRecorrenteController(
            LancamentoRecorrenteService recorrenteService,
            AutorizacaoService autorizacaoService) {
        this.recorrenteService = recorrenteService;
        this.autorizacaoService = autorizacaoService;
    }

    @GetMapping
    @Operation(summary = "Lista modelos de lançamentos recorrentes ativos")
    public List<LancamentoRecorrenteResponse> listar(
            @AuthenticationPrincipal UsuarioAutenticado auth,
            @RequestParam(required = false) UUID produtorId) {
        Usuario produtor = autorizacaoService.resolverProdutorParaLeitura(auth, produtorId);
        return recorrenteService.listar(produtor.getId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastra um modelo de lançamento recorrente")
    public LancamentoRecorrenteResponse criar(
            @AuthenticationPrincipal UsuarioAutenticado auth,
            @Valid @RequestBody LancamentoRecorrenteRequest request) {
        Usuario produtor = autorizacaoService.resolverProdutorParaEscrita(auth);
        return recorrenteService.criar(produtor, request);
    }

    @PostMapping("/{id}/gerar")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Gera um lançamento real a partir do modelo recorrente")
    public LancamentoResponse gerar(
            @AuthenticationPrincipal UsuarioAutenticado auth,
            @PathVariable UUID id,
            @RequestParam(required = false) LocalDate data) {
        Usuario produtor = autorizacaoService.resolverProdutorParaEscrita(auth);
        return recorrenteService.gerarLancamento(produtor, id, data);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Desativa um lançamento recorrente")
    public void desativar(
            @AuthenticationPrincipal UsuarioAutenticado auth,
            @PathVariable UUID id) {
        Usuario produtor = autorizacaoService.resolverProdutorParaEscrita(auth);
        recorrenteService.desativar(produtor, id);
    }
}
