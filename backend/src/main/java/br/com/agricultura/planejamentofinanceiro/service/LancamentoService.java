package br.com.agricultura.planejamentofinanceiro.service;

import br.com.agricultura.planejamentofinanceiro.domain.entity.Cultura;
import br.com.agricultura.planejamentofinanceiro.domain.entity.Lancamento;
import br.com.agricultura.planejamentofinanceiro.domain.entity.RateioCusto;
import br.com.agricultura.planejamentofinanceiro.domain.entity.Usuario;
import br.com.agricultura.planejamentofinanceiro.domain.enums.CategoriaLancamento;
import br.com.agricultura.planejamentofinanceiro.domain.enums.ClassificacaoCusto;
import br.com.agricultura.planejamentofinanceiro.domain.enums.TipoLancamento;
import br.com.agricultura.planejamentofinanceiro.dto.LancamentoDtos.AtualizarRateiosRequest;
import br.com.agricultura.planejamentofinanceiro.dto.LancamentoDtos.CadernoDigitalResponse;
import br.com.agricultura.planejamentofinanceiro.dto.LancamentoDtos.CategoriaFrequenciaDto;
import br.com.agricultura.planejamentofinanceiro.dto.LancamentoDtos.LancamentoRequest;
import br.com.agricultura.planejamentofinanceiro.dto.LancamentoDtos.LancamentoResponse;
import br.com.agricultura.planejamentofinanceiro.dto.LancamentoDtos.RateioItemRequest;
import br.com.agricultura.planejamentofinanceiro.dto.LancamentoDtos.RateioItemResponse;
import br.com.agricultura.planejamentofinanceiro.dto.LancamentoDtos.SugestaoCategoriaResponse;
import br.com.agricultura.planejamentofinanceiro.repository.CulturaRepository;
import br.com.agricultura.planejamentofinanceiro.repository.FechamentoPeriodoRepository;
import br.com.agricultura.planejamentofinanceiro.repository.LancamentoRepository;
import br.com.agricultura.planejamentofinanceiro.service.exception.AcessoNegadoException;
import br.com.agricultura.planejamentofinanceiro.service.exception.RecursoNaoEncontradoException;
import br.com.agricultura.planejamentofinanceiro.service.exception.RegraNegocioException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LancamentoService {

    private static final DateTimeFormatter ANO_MES_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    private final LancamentoRepository lancamentoRepository;
    private final CulturaRepository culturaRepository;
    private final FechamentoPeriodoRepository fechamentoPeriodoRepository;

    public LancamentoService(
            LancamentoRepository lancamentoRepository,
            CulturaRepository culturaRepository,
            FechamentoPeriodoRepository fechamentoPeriodoRepository) {
        this.lancamentoRepository = lancamentoRepository;
        this.culturaRepository = culturaRepository;
        this.fechamentoPeriodoRepository = fechamentoPeriodoRepository;
    }

    @Transactional(readOnly = true)
    public CadernoDigitalResponse listarCaderno(UUID produtorId, String anoMes) {
        List<Lancamento> lista = lancamentoRepository
                .findByProdutorIdAndAnoMesAndExcluidoFalseOrderByDataLancamentoAscCriadoEmAsc(produtorId, anoMes);
        boolean periodoFechado = fechamentoPeriodoRepository.existsByProdutorIdAndAnoMes(produtorId, anoMes);

        BigDecimal totalEntradas = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalSaidas = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal saldoAcumulado = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        List<LancamentoResponse> respostas = new ArrayList<>();
        for (Lancamento l : lista) {
            BigDecimal val = l.getValor().setScale(2, RoundingMode.HALF_UP);
            if (l.getTipo() == TipoLancamento.ENTRADA) {
                totalEntradas = totalEntradas.add(val);
                saldoAcumulado = saldoAcumulado.add(val);
            } else {
                totalSaidas = totalSaidas.add(val);
                saldoAcumulado = saldoAcumulado.subtract(val);
            }
            respostas.add(toResponse(l, saldoAcumulado));
        }

        return new CadernoDigitalResponse(
                anoMes,
                periodoFechado,
                totalEntradas,
                totalSaidas,
                saldoAcumulado,
                respostas
        );
    }

    @Transactional(readOnly = true)
    public SugestaoCategoriaResponse sugerirCategoria(UUID produtorId, TipoLancamento tipo) {
        List<Object[]> freq = lancamentoRepository.findFrequenciaCategorias(produtorId, tipo);
        List<CategoriaFrequenciaDto> ranking = new ArrayList<>();
        for (Object[] row : freq) {
            CategoriaLancamento cat = (CategoriaLancamento) row[0];
            long qtd = ((Number) row[1]).longValue();
            ranking.add(new CategoriaFrequenciaDto(cat, cat.getRotulo(), qtd));
        }

        CategoriaLancamento sugerida = ranking.isEmpty()
                ? (tipo == TipoLancamento.ENTRADA ? CategoriaLancamento.VENDAS : CategoriaLancamento.SEMENTES)
                : ranking.get(0).categoria();

        return new SugestaoCategoriaResponse(tipo, sugerida, sugerida.getRotulo(), ranking);
    }

    @Transactional
    @CacheEvict(cacheNames = "indicadores", allEntries = true)
    public LancamentoResponse criar(Usuario produtor, LancamentoRequest req) {
        String anoMes = formatarAnoMes(req.dataLancamento());
        validarPeriodoAberto(produtor.getId(), anoMes);

        String chave = (req.chaveIdempotencia() == null || req.chaveIdempotencia().isBlank())
                ? UUID.randomUUID().toString()
                : req.chaveIdempotencia().trim();

        if (lancamentoRepository.findByChaveIdempotencia(chave).isPresent()) {
            throw new RegraNegocioException("Já existe lançamento com a chave de idempotência informada.");
        }

        Cultura cultura = resolverCulturaOpcional(produtor.getHorta().getId(), req.culturaId());
        ClassificacaoCusto classificacao = resolverClassificacao(req.tipo(), req.classificacaoCusto(), req.categoria());

        Lancamento lancamento = new Lancamento();
        lancamento.setId(UUID.randomUUID());
        lancamento.setChaveIdempotencia(chave);
        lancamento.setProdutor(produtor);
        lancamento.setCultura(cultura);
        lancamento.setTipo(req.tipo());
        lancamento.setClassificacaoCusto(classificacao);
        lancamento.setCategoria(req.categoria());
        lancamento.setDescricao(limparDescricao(req.descricao(), req.categoria()));
        lancamento.setValor(req.valor().setScale(2, RoundingMode.HALF_UP));
        lancamento.setQuantidade(req.quantidade() != null ? req.quantidade().setScale(2, RoundingMode.HALF_UP) : null);
        lancamento.setDataLancamento(req.dataLancamento());
        lancamento.setAnoMes(anoMes);
        lancamento.setVersao(1L);
        lancamento.setExcluido(false);
        LocalDateTime agora = LocalDateTime.now();
        lancamento.setCriadoEm(agora);
        lancamento.setAtualizadoEm(agora);

        aplicarRateios(lancamento, produtor.getHorta().getId(), req.rateios());
        Lancamento salvo = lancamentoRepository.save(lancamento);
        return toResponse(salvo, null);
    }

    @Transactional
    @CacheEvict(cacheNames = "indicadores", allEntries = true)
    public LancamentoResponse atualizar(Usuario produtor, UUID lancamentoId, LancamentoRequest req) {
        Lancamento lancamento = buscarParaEdicao(produtor, lancamentoId);
        validarPeriodoAberto(produtor.getId(), lancamento.getAnoMes());

        String novoAnoMes = formatarAnoMes(req.dataLancamento());
        validarPeriodoAberto(produtor.getId(), novoAnoMes);

        Cultura cultura = resolverCulturaOpcional(produtor.getHorta().getId(), req.culturaId());
        ClassificacaoCusto classificacao = resolverClassificacao(req.tipo(), req.classificacaoCusto(), req.categoria());

        lancamento.setCultura(cultura);
        lancamento.setTipo(req.tipo());
        lancamento.setClassificacaoCusto(classificacao);
        lancamento.setCategoria(req.categoria());
        lancamento.setDescricao(limparDescricao(req.descricao(), req.categoria()));
        lancamento.setValor(req.valor().setScale(2, RoundingMode.HALF_UP));
        lancamento.setQuantidade(req.quantidade() != null ? req.quantidade().setScale(2, RoundingMode.HALF_UP) : null);
        lancamento.setDataLancamento(req.dataLancamento());
        lancamento.setAnoMes(novoAnoMes);
        lancamento.setVersao(lancamento.getVersao() + 1);
        lancamento.setAtualizadoEm(LocalDateTime.now());

        aplicarRateios(lancamento, produtor.getHorta().getId(), req.rateios());
        Lancamento salvo = lancamentoRepository.save(lancamento);
        return toResponse(salvo, null);
    }

    @Transactional
    @CacheEvict(cacheNames = "indicadores", allEntries = true)
    public void excluirLogicamente(Usuario produtor, UUID lancamentoId) {
        Lancamento lancamento = buscarParaEdicao(produtor, lancamentoId);
        validarPeriodoAberto(produtor.getId(), lancamento.getAnoMes());
        lancamento.setExcluido(true);
        lancamento.setExcluidoEm(LocalDateTime.now());
        lancamento.setAtualizadoEm(LocalDateTime.now());
        lancamento.setVersao(lancamento.getVersao() + 1);
        lancamentoRepository.save(lancamento);
    }

    @Transactional
    @CacheEvict(cacheNames = "indicadores", allEntries = true)
    public LancamentoResponse atualizarRateios(Usuario produtor, UUID lancamentoId, AtualizarRateiosRequest req) {
        Lancamento lancamento = buscarParaEdicao(produtor, lancamentoId);
        validarPeriodoAberto(produtor.getId(), lancamento.getAnoMes());
        if (lancamento.getTipo() != TipoLancamento.SAIDA) {
            throw new RegraNegocioException("Somente custos (SAIDA) podem ser rateados entre culturas.");
        }
        if (req.rateios() == null || req.rateios().isEmpty()) {
            throw new RegraNegocioException("Informe ao menos uma cultura para o rateio.");
        }
        aplicarRateiosExplicitos(lancamento, produtor.getHorta().getId(), req.rateios());
        lancamento.setVersao(lancamento.getVersao() + 1);
        lancamento.setAtualizadoEm(LocalDateTime.now());
        return toResponse(lancamentoRepository.save(lancamento), null);
    }

    public void aplicarRateios(Lancamento lancamento, UUID hortaId, List<RateioItemRequest> rateiosReq) {
        if (lancamento.getTipo() == TipoLancamento.ENTRADA) {
            lancamento.getRateios().clear();
            return;
        }
        if (rateiosReq != null && !rateiosReq.isEmpty()) {
            aplicarRateiosExplicitos(lancamento, hortaId, rateiosReq);
            return;
        }
        lancamento.getRateios().clear();
        if (lancamento.getCultura() != null) {
            RateioCusto r = new RateioCusto();
            r.setId(UUID.randomUUID());
            r.setLancamento(lancamento);
            r.setCultura(lancamento.getCultura());
            r.setValorRateado(lancamento.getValor());
            r.setPercentual(new BigDecimal("100.00"));
            lancamento.getRateios().add(r);
            return;
        }
        List<Cultura> culturas = culturaRepository.findByHortaIdAndAtivoTrueOrderByNomeAsc(hortaId);
        if (culturas.isEmpty()) {
            return;
        }
        BigDecimal total = lancamento.getValor();
        int n = culturas.size();
        BigDecimal base = total.divide(BigDecimal.valueOf(n), 2, RoundingMode.DOWN);
        BigDecimal acumulado = BigDecimal.ZERO;
        for (int i = 0; i < n; i++) {
            Cultura c = culturas.get(i);
            BigDecimal valorCultura = (i == n - 1) ? total.subtract(acumulado) : base;
            acumulado = acumulado.add(valorCultura);
            RateioCusto r = new RateioCusto();
            r.setId(UUID.randomUUID());
            r.setLancamento(lancamento);
            r.setCultura(c);
            r.setValorRateado(valorCultura.setScale(2, RoundingMode.HALF_UP));
            r.setPercentual(valorCultura.multiply(new BigDecimal("100"))
                    .divide(total, 2, RoundingMode.HALF_UP));
            lancamento.getRateios().add(r);
        }
    }

    private void aplicarRateiosExplicitos(Lancamento lancamento, UUID hortaId, List<RateioItemRequest> rateiosReq) {
        Map<UUID, Cultura> culturasHorta = new HashMap<>();
        for (Cultura c : culturaRepository.findByHortaIdAndAtivoTrueOrderByNomeAsc(hortaId)) {
            culturasHorta.put(c.getId(), c);
        }

        Set<UUID> vistas = new HashSet<>();
        BigDecimal soma = BigDecimal.ZERO;
        List<RateioCusto> novos = new ArrayList<>();

        for (RateioItemRequest item : rateiosReq) {
            Cultura c = culturasHorta.get(item.culturaId());
            if (c == null) {
                throw new RegraNegocioException("Cultura inválida para o rateio nesta horta.");
            }
            if (!vistas.add(item.culturaId())) {
                throw new RegraNegocioException("Cultura duplicada no rateio: " + c.getNome());
            }
            BigDecimal vr = item.valorRateado().setScale(2, RoundingMode.HALF_UP);
            soma = soma.add(vr);

            RateioCusto r = new RateioCusto();
            r.setId(UUID.randomUUID());
            r.setLancamento(lancamento);
            r.setCultura(c);
            r.setValorRateado(vr);
            r.setPercentual(vr.multiply(new BigDecimal("100"))
                    .divide(lancamento.getValor(), 2, RoundingMode.HALF_UP));
            novos.add(r);
        }

        if (soma.compareTo(lancamento.getValor()) != 0) {
            throw new RegraNegocioException(
                    "A soma dos rateios (R$ " + soma + ") deve ser igual ao custo total (R$ " + lancamento.getValor() + ").");
        }

        lancamento.getRateios().clear();
        lancamento.getRateios().addAll(novos);
    }

    public void validarPeriodoAberto(UUID produtorId, String anoMes) {
        if (fechamentoPeriodoRepository.existsByProdutorIdAndAnoMes(produtorId, anoMes)) {
            throw new RegraNegocioException("Período " + anoMes + " já está fechado e congelado.");
        }
    }

    public Cultura resolverCulturaOpcional(UUID hortaId, UUID culturaId) {
        if (culturaId == null) {
            return null;
        }
        Cultura c = culturaRepository.findById(culturaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cultura não encontrada."));
        if (!c.getHorta().getId().equals(hortaId)) {
            throw new AcessoNegadoException("Cultura não pertence à horta do produtor.");
        }
        return c;
    }

    public ClassificacaoCusto resolverClassificacao(
            TipoLancamento tipo, ClassificacaoCusto informada, CategoriaLancamento categoria) {
        if (tipo == TipoLancamento.ENTRADA) {
            return ClassificacaoCusto.NAO_APLICAVEL;
        }
        if (informada != null && informada != ClassificacaoCusto.NAO_APLICAVEL) {
            return informada;
        }
        return categoria.getClassificacaoPadrao() == ClassificacaoCusto.NAO_APLICAVEL
                ? ClassificacaoCusto.VARIAVEL
                : categoria.getClassificacaoPadrao();
    }

    private Lancamento buscarParaEdicao(Usuario produtor, UUID id) {
        Lancamento lancamento = lancamentoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Lançamento não encontrado."));
        if (!lancamento.getProdutor().getId().equals(produtor.getId())) {
            throw new AcessoNegadoException("Você não tem permissão para alterar este lançamento.");
        }
        if (lancamento.isExcluido()) {
            throw new RecursoNaoEncontradoException("Lançamento já foi excluído.");
        }
        return lancamento;
    }

    public String formatarAnoMes(LocalDate data) {
        return data.format(ANO_MES_FMT);
    }

    public String limparDescricao(String descricao, CategoriaLancamento categoria) {
        if (descricao == null || descricao.isBlank()) {
            return categoria.getRotulo();
        }
        return descricao.trim();
    }

    public LancamentoResponse toResponse(Lancamento l, BigDecimal saldoAcumulado) {
        List<RateioItemResponse> rateios = l.getRateios().stream()
                .map(r -> new RateioItemResponse(
                        r.getId(),
                        r.getCultura().getId(),
                        r.getCultura().getNome(),
                        r.getValorRateado(),
                        r.getPercentual()))
                .toList();

        return new LancamentoResponse(
                l.getId(),
                l.getChaveIdempotencia(),
                l.getProdutor().getId(),
                l.getCultura() != null ? l.getCultura().getId() : null,
                l.getCultura() != null ? l.getCultura().getNome() : "Geral da Horta",
                l.getTipo(),
                l.getClassificacaoCusto(),
                l.getCategoria(),
                l.getCategoria().getRotulo(),
                l.getDescricao(),
                l.getValor(),
                l.getQuantidade(),
                l.getDataLancamento(),
                l.getAnoMes(),
                l.getVersao(),
                saldoAcumulado,
                rateios,
                l.getAtualizadoEm()
        );
    }
}
