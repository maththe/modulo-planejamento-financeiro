package br.com.agricultura.planejamentofinanceiro.controller;

import br.com.agricultura.planejamentofinanceiro.domain.entity.Usuario;
import br.com.agricultura.planejamentofinanceiro.dto.IndicadoresDtos.FechamentoPeriodoResponse;
import br.com.agricultura.planejamentofinanceiro.security.AutorizacaoService;
import br.com.agricultura.planejamentofinanceiro.security.UsuarioAutenticado;
import br.com.agricultura.planejamentofinanceiro.service.PeriodoFechamentoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/periodos")
@Tag(name = "Períodos", description = "Fechamento mensal que congela o resultado pela API")
public class PeriodoController {

    private final PeriodoFechamentoService fechamentoService;
    private final AutorizacaoService autorizacaoService;

    public PeriodoController(
            PeriodoFechamentoService fechamentoService,
            AutorizacaoService autorizacaoService) {
        this.fechamentoService = fechamentoService;
        this.autorizacaoService = autorizacaoService;
    }

    @PostMapping("/{anoMes}/fechar")
    @Operation(summary = "Fecha o período mensal e congela os indicadores do mês")
    public FechamentoPeriodoResponse fechar(
            @AuthenticationPrincipal UsuarioAutenticado auth,
            @PathVariable String anoMes) {
        Usuario produtor = autorizacaoService.resolverProdutorParaEscrita(auth);
        return fechamentoService.fecharPeriodo(produtor, anoMes);
    }

    @GetMapping("/{anoMes}")
    @Operation(summary = "Consulta o fechamento congelado de um período")
    public FechamentoPeriodoResponse consultar(
            @AuthenticationPrincipal UsuarioAutenticado auth,
            @PathVariable String anoMes,
            @RequestParam(required = false) UUID produtorId) {
        Usuario produtor = autorizacaoService.resolverProdutorParaLeitura(auth, produtorId);
        return fechamentoService.consultarFechamento(produtor.getId(), anoMes);
    }
}
