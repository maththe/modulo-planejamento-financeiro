package br.com.agricultura.planejamentofinanceiro.service;

import br.com.agricultura.planejamentofinanceiro.domain.entity.Cultura;
import br.com.agricultura.planejamentofinanceiro.domain.entity.FechamentoPeriodo;
import br.com.agricultura.planejamentofinanceiro.domain.entity.Lancamento;
import br.com.agricultura.planejamentofinanceiro.domain.entity.ProducaoMensal;
import br.com.agricultura.planejamentofinanceiro.domain.entity.RateioCusto;
import br.com.agricultura.planejamentofinanceiro.domain.entity.Usuario;
import br.com.agricultura.planejamentofinanceiro.domain.enums.ClassificacaoCusto;
import br.com.agricultura.planejamentofinanceiro.domain.enums.StatusSemaforo;
import br.com.agricultura.planejamentofinanceiro.domain.enums.TipoLancamento;
import br.com.agricultura.planejamentofinanceiro.dto.IndicadoresDtos.IndicadorCulturaDto;
import br.com.agricultura.planejamentofinanceiro.dto.IndicadoresDtos.IndicadoresPeriodoDto;
import br.com.agricultura.planejamentofinanceiro.dto.IndicadoresDtos.SerieHistoricaItemDto;
import br.com.agricultura.planejamentofinanceiro.repository.CulturaRepository;
import br.com.agricultura.planejamentofinanceiro.repository.FechamentoPeriodoRepository;
import br.com.agricultura.planejamentofinanceiro.repository.LancamentoRepository;
import br.com.agricultura.planejamentofinanceiro.repository.ProducaoMensalRepository;
import br.com.agricultura.planejamentofinanceiro.repository.UsuarioRepository;
import br.com.agricultura.planejamentofinanceiro.service.exception.RecursoNaoEncontradoException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;
import java.util.UUID;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CalculadoraIndicadoresService {

    private static final BigDecimal MARGEM_SEGURANCA_VERDE = new BigDecimal("1.10");
    private static final String AVISO_MARGEM_INVALIDA =
            "Margem de contribuição nula ou negativa: Ponto de Equilíbrio não aplicável.";

    private final UsuarioRepository usuarioRepository;
    private final CulturaRepository culturaRepository;
    private final LancamentoRepository lancamentoRepository;
    private final ProducaoMensalRepository producaoMensalRepository;
    private final FechamentoPeriodoRepository fechamentoPeriodoRepository;

    public CalculadoraIndicadoresService(
            UsuarioRepository usuarioRepository,
            CulturaRepository culturaRepository,
            LancamentoRepository lancamentoRepository,
            ProducaoMensalRepository producaoMensalRepository,
            FechamentoPeriodoRepository fechamentoPeriodoRepository) {
        this.usuarioRepository = usuarioRepository;
        this.culturaRepository = culturaRepository;
        this.lancamentoRepository = lancamentoRepository;
        this.producaoMensalRepository = producaoMensalRepository;
        this.fechamentoPeriodoRepository = fechamentoPeriodoRepository;
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "indicadores", key = "#produtorId.toString() + ':' + #anoMes")
    public IndicadoresPeriodoDto obterIndicadoresPeriodo(UUID produtorId, String anoMes) {
        Usuario produtor = usuarioRepository.findById(produtorId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Produtor não encontrado."));

        List<Cultura> culturas = culturaRepository.findByHortaIdAndAtivoTrueOrderByNomeAsc(produtor.getHorta().getId());
        List<Lancamento> lancamentos = lancamentoRepository
                .findByProdutorIdAndAnoMesAndExcluidoFalseOrderByDataLancamentoAscCriadoEmAsc(produtorId, anoMes);
        List<ProducaoMensal> producoes = producaoMensalRepository.findByProdutorIdAndAnoMes(produtorId, anoMes);

        Optional<FechamentoPeriodo> fechamentoOpt = fechamentoPeriodoRepository.findByProdutorIdAndAnoMes(produtorId, anoMes);
        IndicadoresPeriodoDto calculado = calcularIndicadores(anoMes, fechamentoOpt.isPresent(), culturas, lancamentos, producoes);

        if (fechamentoOpt.isPresent()) {
            FechamentoPeriodo fechamento = fechamentoOpt.get();
            boolean margemInvalida = fechamento.getMargemContribuicao().compareTo(BigDecimal.ZERO) <= 0
                    || fechamento.getReceitaTotal().compareTo(BigDecimal.ZERO) <= 0;
            return new IndicadoresPeriodoDto(
                    anoMes,
                    true,
                    escala2(fechamento.getReceitaTotal()),
                    escala2(fechamento.getCustosFixos()),
                    escala2(fechamento.getCustosVariaveis()),
                    escala2(fechamento.getCustoTotal()),
                    escala2(fechamento.getMargemContribuicao()),
                    fechamento.getPontoEquilibrio() != null ? escala2(fechamento.getPontoEquilibrio()) : null,
                    escala2(fechamento.getResultado()),
                    margemInvalida,
                    margemInvalida ? AVISO_MARGEM_INVALIDA : null,
                    fechamento.getStatusSemaforo(),
                    calculado.indicadoresPorCultura()
            );
        }
        return calculado;
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "indicadores", key = "'serie:' + #produtorId.toString()")
    public List<SerieHistoricaItemDto> obterSerieHistorica(UUID produtorId) {
        TreeSet<String> meses = new TreeSet<>(lancamentoRepository.findDistinctAnoMesByProdutorId(produtorId));
        fechamentoPeriodoRepository.findByProdutorIdOrderByAnoMesAsc(produtorId)
                .forEach(f -> meses.add(f.getAnoMes()));

        List<SerieHistoricaItemDto> serie = new ArrayList<>();
        for (String mes : meses) {
            IndicadoresPeriodoDto ind = obterIndicadoresPeriodo(produtorId, mes);
            serie.add(new SerieHistoricaItemDto(
                    ind.anoMes(),
                    ind.periodoFechado(),
                    ind.receitaTotal(),
                    ind.custosFixos(),
                    ind.custosVariaveis(),
                    ind.custosTotais(),
                    ind.margemContribuicao(),
                    ind.pontoEquilibrio(),
                    ind.resultado(),
                    ind.margemNulaOuNegativa(),
                    ind.statusSemaforo()
            ));
        }
        return serie;
    }

    public IndicadoresPeriodoDto calcularIndicadores(
            String anoMes,
            boolean periodoFechado,
            List<Cultura> culturas,
            List<Lancamento> lancamentos,
            List<ProducaoMensal> producoes) {

        BigDecimal receitaTotal = BigDecimal.ZERO;
        BigDecimal custosFixos = BigDecimal.ZERO;
        BigDecimal custosVariaveis = BigDecimal.ZERO;

        Map<UUID, AcumuladorCultura> acumuladores = new LinkedHashMap<>();
        for (Cultura c : culturas) {
            acumuladores.put(c.getId(), new AcumuladorCultura(c));
        }

        Map<UUID, BigDecimal> producaoMap = new HashMap<>();
        for (ProducaoMensal pm : producoes) {
            producaoMap.put(pm.getCultura().getId(), escala2(pm.getQuantidadeProduzida()));
        }

        for (Lancamento l : lancamentos) {
            if (l.isExcluido()) {
                continue;
            }
            BigDecimal valor = escala2(l.getValor());
            if (l.getTipo() == TipoLancamento.ENTRADA) {
                receitaTotal = receitaTotal.add(valor);
                if (l.getCultura() != null && acumuladores.containsKey(l.getCultura().getId())) {
                    AcumuladorCultura ac = acumuladores.get(l.getCultura().getId());
                    ac.receita = ac.receita.add(valor);
                    if (l.getQuantidade() != null) {
                        ac.quantidadeVendida = ac.quantidadeVendida.add(l.getQuantidade());
                    }
                }
            } else {
                boolean isFixo = l.getClassificacaoCusto() == ClassificacaoCusto.FIXO;
                if (isFixo) {
                    custosFixos = custosFixos.add(valor);
                } else {
                    custosVariaveis = custosVariaveis.add(valor);
                }

                if (l.getRateios() != null && !l.getRateios().isEmpty()) {
                    for (RateioCusto r : l.getRateios()) {
                        AcumuladorCultura ac = acumuladores.get(r.getCultura().getId());
                        if (ac != null) {
                            BigDecimal vr = escala2(r.getValorRateado());
                            if (isFixo) {
                                ac.custosFixosRateados = ac.custosFixosRateados.add(vr);
                            } else {
                                ac.custosVariaveis = ac.custosVariaveis.add(vr);
                            }
                        }
                    }
                } else if (l.getCultura() != null && acumuladores.containsKey(l.getCultura().getId())) {
                    AcumuladorCultura ac = acumuladores.get(l.getCultura().getId());
                    if (isFixo) {
                        ac.custosFixosRateados = ac.custosFixosRateados.add(valor);
                    } else {
                        ac.custosVariaveis = ac.custosVariaveis.add(valor);
                    }
                }
            }
        }

        receitaTotal = escala2(receitaTotal);
        custosFixos = escala2(custosFixos);
        custosVariaveis = escala2(custosVariaveis);
        BigDecimal custosTotais = escala2(custosFixos.add(custosVariaveis));
        BigDecimal resultado = escala2(receitaTotal.subtract(custosTotais));
        BigDecimal margemContribuicao = escala2(receitaTotal.subtract(custosVariaveis));

        boolean margemNulaOuNegativa = receitaTotal.compareTo(BigDecimal.ZERO) <= 0
                || margemContribuicao.compareTo(BigDecimal.ZERO) <= 0;
        String avisoMargem = margemNulaOuNegativa ? AVISO_MARGEM_INVALIDA : null;

        BigDecimal pontoEquilibrio = null;
        if (!margemNulaOuNegativa) {
            if (custosFixos.compareTo(BigDecimal.ZERO) == 0) {
                pontoEquilibrio = escala2(BigDecimal.ZERO);
            } else {
                BigDecimal indiceMargem = margemContribuicao.divide(receitaTotal, 8, RoundingMode.HALF_UP);
                pontoEquilibrio = custosFixos.divide(indiceMargem, 2, RoundingMode.HALF_UP);
            }
        }

        StatusSemaforo semaforo = calcularSemaforo(resultado, receitaTotal, pontoEquilibrio);

        List<IndicadorCulturaDto> porCultura = new ArrayList<>();
        for (AcumuladorCultura ac : acumuladores.values()) {
            BigDecimal qtdProd = producaoMap.getOrDefault(ac.cultura.getId(), BigDecimal.ZERO);
            if (qtdProd.compareTo(BigDecimal.ZERO) <= 0 && ac.quantidadeVendida.compareTo(BigDecimal.ZERO) > 0) {
                qtdProd = ac.quantidadeVendida;
            }
            qtdProd = escala2(qtdProd);

            BigDecimal cv = escala2(ac.custosVariaveis);
            BigDecimal cf = escala2(ac.custosFixosRateados);
            BigDecimal ct = escala2(cv.add(cf));
            BigDecimal rec = escala2(ac.receita);
            BigDecimal mc = escala2(rec.subtract(cv));
            BigDecimal res = escala2(rec.subtract(ct));
            BigDecimal custoUnitario = qtdProd.compareTo(BigDecimal.ZERO) > 0
                    ? ct.divide(qtdProd, 2, RoundingMode.HALF_UP)
                    : null;

            porCultura.add(new IndicadorCulturaDto(
                    ac.cultura.getId(),
                    ac.cultura.getNome(),
                    ac.cultura.getUnidade(),
                    qtdProd,
                    rec,
                    cv,
                    cf,
                    ct,
                    custoUnitario,
                    mc,
                    res
            ));
        }

        return new IndicadoresPeriodoDto(
                anoMes,
                periodoFechado,
                receitaTotal,
                custosFixos,
                custosVariaveis,
                custosTotais,
                margemContribuicao,
                pontoEquilibrio,
                resultado,
                margemNulaOuNegativa,
                avisoMargem,
                semaforo,
                porCultura
        );
    }

    public StatusSemaforo calcularSemaforo(BigDecimal resultado, BigDecimal receitaTotal, BigDecimal pontoEquilibrio) {
        if (resultado.compareTo(BigDecimal.ZERO) <= 0) {
            return StatusSemaforo.VERMELHO;
        }
        if (pontoEquilibrio != null) {
            BigDecimal limiteVerde = pontoEquilibrio.multiply(MARGEM_SEGURANCA_VERDE).setScale(2, RoundingMode.HALF_UP);
            if (receitaTotal.compareTo(limiteVerde) >= 0) {
                return StatusSemaforo.VERDE;
            }
        }
        return StatusSemaforo.AMARELO;
    }

    private BigDecimal escala2(BigDecimal valor) {
        return (valor == null ? BigDecimal.ZERO : valor).setScale(2, RoundingMode.HALF_UP);
    }

    private static final class AcumuladorCultura {
        final Cultura cultura;
        BigDecimal receita = BigDecimal.ZERO;
        BigDecimal custosVariaveis = BigDecimal.ZERO;
        BigDecimal custosFixosRateados = BigDecimal.ZERO;
        BigDecimal quantidadeVendida = BigDecimal.ZERO;

        AcumuladorCultura(Cultura cultura) {
            this.cultura = cultura;
        }
    }
}
