package br.com.agricultura.planejamentofinanceiro.service;

import br.com.agricultura.planejamentofinanceiro.domain.entity.ConflitoSincronizacao;
import br.com.agricultura.planejamentofinanceiro.domain.entity.Cultura;
import br.com.agricultura.planejamentofinanceiro.domain.entity.Lancamento;
import br.com.agricultura.planejamentofinanceiro.domain.entity.Usuario;
import br.com.agricultura.planejamentofinanceiro.domain.enums.ClassificacaoCusto;
import br.com.agricultura.planejamentofinanceiro.dto.SyncDtos.SyncItemRequest;
import br.com.agricultura.planejamentofinanceiro.dto.SyncDtos.SyncItemResultadoDto;
import br.com.agricultura.planejamentofinanceiro.repository.ConflitoSincronizacaoRepository;
import br.com.agricultura.planejamentofinanceiro.repository.LancamentoRepository;
import br.com.agricultura.planejamentofinanceiro.service.exception.AcessoNegadoException;
import br.com.agricultura.planejamentofinanceiro.service.exception.RegraNegocioException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SincronizacaoItemProcessor {

    private final LancamentoRepository lancamentoRepository;
    private final ConflitoSincronizacaoRepository conflitoRepository;
    private final LancamentoService lancamentoService;
    private final ObjectMapper objectMapper;

    public SincronizacaoItemProcessor(
            LancamentoRepository lancamentoRepository,
            ConflitoSincronizacaoRepository conflitoRepository,
            LancamentoService lancamentoService,
            ObjectMapper objectMapper) {
        this.lancamentoRepository = lancamentoRepository;
        this.conflitoRepository = conflitoRepository;
        this.lancamentoService = lancamentoService;
        this.objectMapper = objectMapper;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SyncItemResultadoDto processarItem(Usuario produtor, SyncItemRequest item) {
        validarItem(item);
        String chave = item.chaveIdempotencia().trim();
        String anoMes = lancamentoService.formatarAnoMes(item.dataLancamento());
        lancamentoService.validarPeriodoAberto(produtor.getId(), anoMes);

        Optional<Lancamento> existenteOpt = lancamentoRepository.findByChaveIdempotencia(chave);
        if (existenteOpt.isEmpty()) {
            Cultura cultura = lancamentoService.resolverCulturaOpcional(produtor.getHorta().getId(), item.culturaId());
            ClassificacaoCusto classificacao = lancamentoService.resolverClassificacao(
                    item.tipo(), item.classificacaoCusto(), item.categoria());

            Lancamento novo = new Lancamento();
            novo.setId(UUID.randomUUID());
            novo.setChaveIdempotencia(chave);
            novo.setProdutor(produtor);
            novo.setCultura(cultura);
            novo.setTipo(item.tipo());
            novo.setClassificacaoCusto(classificacao);
            novo.setCategoria(item.categoria());
            novo.setDescricao(lancamentoService.limparDescricao(item.descricao(), item.categoria()));
            novo.setValor(item.valor().setScale(2, RoundingMode.HALF_UP));
            novo.setQuantidade(item.quantidade() != null ? item.quantidade().setScale(2, RoundingMode.HALF_UP) : null);
            novo.setDataLancamento(item.dataLancamento());
            novo.setAnoMes(anoMes);
            novo.setVersao(item.versaoCliente() != null && item.versaoCliente() > 0 ? item.versaoCliente() : 1L);
            novo.setExcluido(false);
            LocalDateTime agora = LocalDateTime.now();
            novo.setCriadoEm(agora);
            novo.setAtualizadoEm(agora);

            lancamentoService.aplicarRateios(novo, produtor.getHorta().getId(), item.rateios());
            Lancamento salvo = lancamentoRepository.save(novo);

            return new SyncItemResultadoDto(
                    chave,
                    "SINCRONIZADO",
                    salvo.getId(),
                    salvo.getVersao(),
                    "Lançamento salvo com sucesso."
            );
        }

        Lancamento existente = existenteOpt.get();
        if (!existente.getProdutor().getId().equals(produtor.getId())) {
            throw new AcessoNegadoException("Chave de idempotência pertence a outro produtor.");
        }

        if (mesmoConteudo(existente, item)) {
            return new SyncItemResultadoDto(
                    chave,
                    "IDEMPOTENTE",
                    existente.getId(),
                    existente.getVersao(),
                    "Lançamento já sincronizado anteriormente (idempotente)."
            );
        }

        // Conflito detectado: aplica regra "última escrita vence" e registra no log
        String payloadAnterior = serializarResumo(existente);
        Long versaoServidorAnterior = existente.getVersao();
        Long versaoCliente = item.versaoCliente() != null ? item.versaoCliente() : 1L;

        lancamentoService.validarPeriodoAberto(produtor.getId(), existente.getAnoMes());
        Cultura cultura = lancamentoService.resolverCulturaOpcional(produtor.getHorta().getId(), item.culturaId());
        ClassificacaoCusto classificacao = lancamentoService.resolverClassificacao(
                item.tipo(), item.classificacaoCusto(), item.categoria());

        existente.setCultura(cultura);
        existente.setTipo(item.tipo());
        existente.setClassificacaoCusto(classificacao);
        existente.setCategoria(item.categoria());
        existente.setDescricao(lancamentoService.limparDescricao(item.descricao(), item.categoria()));
        existente.setValor(item.valor().setScale(2, RoundingMode.HALF_UP));
        existente.setQuantidade(item.quantidade() != null ? item.quantidade().setScale(2, RoundingMode.HALF_UP) : null);
        existente.setDataLancamento(item.dataLancamento());
        existente.setAnoMes(anoMes);
        existente.setVersao(versaoServidorAnterior + 1);
        existente.setAtualizadoEm(LocalDateTime.now());
        lancamentoService.aplicarRateios(existente, produtor.getHorta().getId(), item.rateios());

        Lancamento atualizado = lancamentoRepository.save(existente);

        ConflitoSincronizacao conflito = new ConflitoSincronizacao();
        conflito.setId(UUID.randomUUID());
        conflito.setChaveIdempotencia(chave);
        conflito.setLancamentoId(atualizado.getId());
        conflito.setProdutorId(produtor.getId());
        conflito.setVersaoServidor(versaoServidorAnterior);
        conflito.setVersaoCliente(versaoCliente);
        conflito.setRegraAplicada("ULTIMA_ESCRITA_VENCE");
        conflito.setPayloadAnterior(payloadAnterior);
        conflito.setPayloadNovo(serializarResumo(atualizado));
        conflito.setResolvidoEm(LocalDateTime.now());
        conflitoRepository.save(conflito);

        return new SyncItemResultadoDto(
                chave,
                "CONFLITO",
                atualizado.getId(),
                atualizado.getVersao(),
                "Conflito resolvido pela regra 'última escrita vence'."
        );
    }

    private void validarItem(SyncItemRequest item) {
        if (item.chaveIdempotencia() == null || item.chaveIdempotencia().isBlank()) {
            throw new RegraNegocioException("Chave de idempotência obrigatória.");
        }
        if (item.tipo() == null || item.categoria() == null || item.dataLancamento() == null) {
            throw new RegraNegocioException("Tipo, categoria e data do lançamento são obrigatórios.");
        }
        if (item.valor() == null || item.valor().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RegraNegocioException("O valor do lançamento deve ser maior que zero.");
        }
    }

    private boolean mesmoConteudo(Lancamento existente, SyncItemRequest item) {
        BigDecimal valorItem = item.valor().setScale(2, RoundingMode.HALF_UP);
        UUID culturaExistente = existente.getCultura() != null ? existente.getCultura().getId() : null;
        String descItem = lancamentoService.limparDescricao(item.descricao(), item.categoria());
        return existente.getTipo() == item.tipo()
                && existente.getCategoria() == item.categoria()
                && existente.getValor().compareTo(valorItem) == 0
                && existente.getDataLancamento().equals(item.dataLancamento())
                && Objects.equals(culturaExistente, item.culturaId())
                && Objects.equals(existente.getDescricao(), descItem);
    }

    private String serializarResumo(Lancamento l) {
        try {
            return objectMapper.writeValueAsString(java.util.Map.of(
                    "tipo", l.getTipo().name(),
                    "categoria", l.getCategoria().name(),
                    "valor", l.getValor(),
                    "data", l.getDataLancamento().toString(),
                    "descricao", l.getDescricao() != null ? l.getDescricao() : "",
                    "versao", l.getVersao()
            ));
        } catch (JsonProcessingException ex) {
            return "{}";
        }
    }
}
