package br.com.agricultura.planejamentofinanceiro.service;

import br.com.agricultura.planejamentofinanceiro.domain.entity.FechamentoPeriodo;
import br.com.agricultura.planejamentofinanceiro.domain.entity.Usuario;
import br.com.agricultura.planejamentofinanceiro.dto.IndicadoresDtos.FechamentoPeriodoResponse;
import br.com.agricultura.planejamentofinanceiro.dto.IndicadoresDtos.IndicadoresPeriodoDto;
import br.com.agricultura.planejamentofinanceiro.repository.FechamentoPeriodoRepository;
import br.com.agricultura.planejamentofinanceiro.service.exception.RecursoNaoEncontradoException;
import br.com.agricultura.planejamentofinanceiro.service.exception.RegraNegocioException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PeriodoFechamentoService {

    private final FechamentoPeriodoRepository fechamentoPeriodoRepository;
    private final CalculadoraIndicadoresService calculadoraIndicadoresService;
    private final ObjectMapper objectMapper;

    public PeriodoFechamentoService(
            FechamentoPeriodoRepository fechamentoPeriodoRepository,
            CalculadoraIndicadoresService calculadoraIndicadoresService,
            ObjectMapper objectMapper) {
        this.fechamentoPeriodoRepository = fechamentoPeriodoRepository;
        this.calculadoraIndicadoresService = calculadoraIndicadoresService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    @CacheEvict(cacheNames = "indicadores", allEntries = true)
    public FechamentoPeriodoResponse fecharPeriodo(Usuario produtor, String anoMes) {
        if (!anoMes.matches("\\d{4}-\\d{2}")) {
            throw new RegraNegocioException("Formato de período inválido. Use YYYY-MM.");
        }
        if (fechamentoPeriodoRepository.existsByProdutorIdAndAnoMes(produtor.getId(), anoMes)) {
            throw new RegraNegocioException("Período " + anoMes + " já foi fechado anteriormente.");
        }

        IndicadoresPeriodoDto ind = calculadoraIndicadoresService.obterIndicadoresPeriodo(produtor.getId(), anoMes);

        FechamentoPeriodo fechamento = new FechamentoPeriodo();
        fechamento.setId(UUID.randomUUID());
        fechamento.setProdutor(produtor);
        fechamento.setAnoMes(anoMes);
        fechamento.setReceitaTotal(ind.receitaTotal());
        fechamento.setCustosFixos(ind.custosFixos());
        fechamento.setCustosVariaveis(ind.custosVariaveis());
        fechamento.setCustoTotal(ind.custosTotais());
        fechamento.setResultado(ind.resultado());
        fechamento.setMargemContribuicao(ind.margemContribuicao());
        fechamento.setPontoEquilibrio(ind.pontoEquilibrio());
        fechamento.setStatusSemaforo(ind.statusSemaforo());
        fechamento.setFechadoEm(LocalDateTime.now());
        try {
            fechamento.setResumoJson(objectMapper.writeValueAsString(ind));
        } catch (JsonProcessingException ex) {
            fechamento.setResumoJson("{}");
        }

        FechamentoPeriodo salvo = fechamentoPeriodoRepository.save(fechamento);
        return toResponse(salvo);
    }

    @Transactional(readOnly = true)
    public FechamentoPeriodoResponse consultarFechamento(UUID produtorId, String anoMes) {
        FechamentoPeriodo fechamento = fechamentoPeriodoRepository.findByProdutorIdAndAnoMes(produtorId, anoMes)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Período " + anoMes + " ainda está aberto."));
        return toResponse(fechamento);
    }

    private FechamentoPeriodoResponse toResponse(FechamentoPeriodo f) {
        return new FechamentoPeriodoResponse(
                f.getId(),
                f.getAnoMes(),
                f.getReceitaTotal(),
                f.getCustosFixos(),
                f.getCustosVariaveis(),
                f.getCustoTotal(),
                f.getResultado(),
                f.getMargemContribuicao(),
                f.getPontoEquilibrio(),
                f.getStatusSemaforo(),
                f.getFechadoEm()
        );
    }
}
