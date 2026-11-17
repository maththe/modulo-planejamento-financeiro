package br.com.agricultura.planejamentofinanceiro.service;

import br.com.agricultura.planejamentofinanceiro.domain.entity.Usuario;
import br.com.agricultura.planejamentofinanceiro.dto.SyncDtos.ConflitoSyncDto;
import br.com.agricultura.planejamentofinanceiro.dto.SyncDtos.SyncItemRequest;
import br.com.agricultura.planejamentofinanceiro.dto.SyncDtos.SyncItemResultadoDto;
import br.com.agricultura.planejamentofinanceiro.dto.SyncDtos.SyncLoteRequest;
import br.com.agricultura.planejamentofinanceiro.dto.SyncDtos.SyncLoteResponse;
import br.com.agricultura.planejamentofinanceiro.repository.ConflitoSincronizacaoRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SincronizacaoService {

    private final SincronizacaoItemProcessor itemProcessor;
    private final ConflitoSincronizacaoRepository conflitoRepository;

    public SincronizacaoService(
            SincronizacaoItemProcessor itemProcessor,
            ConflitoSincronizacaoRepository conflitoRepository) {
        this.itemProcessor = itemProcessor;
        this.conflitoRepository = conflitoRepository;
    }

    @CacheEvict(cacheNames = "indicadores", allEntries = true)
    public SyncLoteResponse processarLote(Usuario produtor, SyncLoteRequest request) {
        List<SyncItemResultadoDto> resultados = new ArrayList<>();
        int sincronizados = 0;
        int idempotentes = 0;
        int conflitos = 0;
        int erros = 0;

        for (SyncItemRequest item : request.itens()) {
            try {
                SyncItemResultadoDto res = itemProcessor.processarItem(produtor, item);
                resultados.add(res);
                switch (res.status()) {
                    case "SINCRONIZADO" -> sincronizados++;
                    case "IDEMPOTENTE" -> idempotentes++;
                    case "CONFLITO" -> conflitos++;
                    default -> erros++;
                }
            } catch (RuntimeException ex) {
                erros++;
                resultados.add(new SyncItemResultadoDto(
                        item.chaveIdempotencia(),
                        "ERRO",
                        null,
                        null,
                        ex.getMessage()
                ));
            }
        }

        return new SyncLoteResponse(
                request.itens().size(),
                sincronizados,
                idempotentes,
                conflitos,
                erros,
                resultados
        );
    }

    @Transactional(readOnly = true)
    public List<ConflitoSyncDto> listarConflitos(UUID produtorId) {
        return conflitoRepository.findByProdutorIdOrderByResolvidoEmDesc(produtorId)
                .stream()
                .map(c -> new ConflitoSyncDto(
                        c.getId(),
                        c.getChaveIdempotencia(),
                        c.getLancamentoId(),
                        c.getVersaoServidor(),
                        c.getVersaoCliente(),
                        c.getRegraAplicada(),
                        c.getPayloadAnterior(),
                        c.getPayloadNovo(),
                        c.getResolvidoEm()
                ))
                .toList();
    }
}
