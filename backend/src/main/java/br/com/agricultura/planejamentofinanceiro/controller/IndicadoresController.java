package br.com.agricultura.planejamentofinanceiro.controller;

import br.com.agricultura.planejamentofinanceiro.domain.entity.Usuario;
import br.com.agricultura.planejamentofinanceiro.dto.IndicadoresDtos.IndicadoresPeriodoDto;
import br.com.agricultura.planejamentofinanceiro.dto.IndicadoresDtos.SerieHistoricaItemDto;
import br.com.agricultura.planejamentofinanceiro.security.AutorizacaoService;
import br.com.agricultura.planejamentofinanceiro.security.UsuarioAutenticado;
import br.com.agricultura.planejamentofinanceiro.service.CalculadoraIndicadoresService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/indicadores")
@Tag(name = "Indicadores", description = "Motor de indicadores financeiros e saúde financeira com cache Caffeine")
public class IndicadoresController {

    private final CalculadoraIndicadoresService calculadoraService;
    private final AutorizacaoService autorizacaoService;

    public IndicadoresController(
            CalculadoraIndicadoresService calculadoraService,
            AutorizacaoService autorizacaoService) {
        this.calculadoraService = calculadoraService;
        this.autorizacaoService = autorizacaoService;
    }

    @GetMapping
    @Operation(summary = "Calcula indicadores do mês, por cultura e semáforo de saúde")
    public IndicadoresPeriodoDto obterIndicadores(
            @AuthenticationPrincipal UsuarioAutenticado auth,
            @RequestParam(required = false) String anoMes,
            @RequestParam(required = false) UUID produtorId) {
        Usuario produtor = autorizacaoService.resolverProdutorParaLeitura(auth, produtorId);
        String mes = (anoMes == null || anoMes.isBlank()) ? YearMonth.now().toString() : anoMes;
        return calculadoraService.obterIndicadoresPeriodo(produtor.getId(), mes);
    }

    @GetMapping("/serie-historica")
    @Operation(summary = "Retorna série histórica mensal em tabela")
    public List<SerieHistoricaItemDto> obterSerieHistorica(
            @AuthenticationPrincipal UsuarioAutenticado auth,
            @RequestParam(required = false) UUID produtorId) {
        Usuario produtor = autorizacaoService.resolverProdutorParaLeitura(auth, produtorId);
        return calculadoraService.obterSerieHistorica(produtor.getId());
    }
}
