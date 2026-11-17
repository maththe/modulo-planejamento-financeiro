package br.com.agricultura.planejamentofinanceiro.service;

import br.com.agricultura.planejamentofinanceiro.domain.entity.Cultura;
import br.com.agricultura.planejamentofinanceiro.domain.entity.LancamentoRecorrente;
import br.com.agricultura.planejamentofinanceiro.domain.entity.Usuario;
import br.com.agricultura.planejamentofinanceiro.domain.enums.ClassificacaoCusto;
import br.com.agricultura.planejamentofinanceiro.dto.LancamentoDtos.LancamentoRecorrenteRequest;
import br.com.agricultura.planejamentofinanceiro.dto.LancamentoDtos.LancamentoRecorrenteResponse;
import br.com.agricultura.planejamentofinanceiro.dto.LancamentoDtos.LancamentoRequest;
import br.com.agricultura.planejamentofinanceiro.dto.LancamentoDtos.LancamentoResponse;
import br.com.agricultura.planejamentofinanceiro.repository.LancamentoRecorrenteRepository;
import br.com.agricultura.planejamentofinanceiro.service.exception.AcessoNegadoException;
import br.com.agricultura.planejamentofinanceiro.service.exception.RecursoNaoEncontradoException;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LancamentoRecorrenteService {

    private final LancamentoRecorrenteRepository recorrenteRepository;
    private final LancamentoService lancamentoService;

    public LancamentoRecorrenteService(
            LancamentoRecorrenteRepository recorrenteRepository,
            LancamentoService lancamentoService) {
        this.recorrenteRepository = recorrenteRepository;
        this.lancamentoService = lancamentoService;
    }

    @Transactional(readOnly = true)
    public List<LancamentoRecorrenteResponse> listar(UUID produtorId) {
        return recorrenteRepository.findByProdutorIdAndAtivoTrueOrderByDiaVencimentoAsc(produtorId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public LancamentoRecorrenteResponse criar(Usuario produtor, LancamentoRecorrenteRequest req) {
        Cultura cultura = lancamentoService.resolverCulturaOpcional(produtor.getHorta().getId(), req.culturaId());
        ClassificacaoCusto classificacao = lancamentoService.resolverClassificacao(
                req.tipo(), req.classificacaoCusto(), req.categoria());

        LancamentoRecorrente rec = new LancamentoRecorrente();
        rec.setId(UUID.randomUUID());
        rec.setProdutor(produtor);
        rec.setCultura(cultura);
        rec.setTipo(req.tipo());
        rec.setClassificacaoCusto(classificacao);
        rec.setCategoria(req.categoria());
        rec.setDescricao(lancamentoService.limparDescricao(req.descricao(), req.categoria()));
        rec.setValorPadrao(req.valorPadrao().setScale(2, RoundingMode.HALF_UP));
        rec.setDiaVencimento(req.diaVencimento());
        rec.setAtivo(true);
        rec.setCriadoEm(LocalDateTime.now());

        return toResponse(recorrenteRepository.save(rec));
    }

    @Transactional
    public LancamentoResponse gerarLancamento(Usuario produtor, UUID recorrenteId, LocalDate dataReferencia) {
        LancamentoRecorrente rec = recorrenteRepository.findById(recorrenteId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Lançamento recorrente não encontrado."));
        if (!rec.getProdutor().getId().equals(produtor.getId())) {
            throw new AcessoNegadoException("Lançamento recorrente não pertence ao produtor.");
        }
        LocalDate data = dataReferencia != null ? dataReferencia : LocalDate.now();
        String anoMes = lancamentoService.formatarAnoMes(data);
        String chave = "recorrente-" + rec.getId() + "-" + anoMes;

        LancamentoRequest req = new LancamentoRequest(
                chave,
                rec.getCultura() != null ? rec.getCultura().getId() : null,
                rec.getTipo(),
                rec.getClassificacaoCusto(),
                rec.getCategoria(),
                rec.getDescricao(),
                rec.getValorPadrao(),
                null,
                data,
                null
        );
        return lancamentoService.criar(produtor, req);
    }

    @Transactional
    public void desativar(Usuario produtor, UUID recorrenteId) {
        LancamentoRecorrente rec = recorrenteRepository.findById(recorrenteId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Lançamento recorrente não encontrado."));
        if (!rec.getProdutor().getId().equals(produtor.getId())) {
            throw new AcessoNegadoException("Lançamento recorrente não pertence ao produtor.");
        }
        rec.setAtivo(false);
        recorrenteRepository.save(rec);
    }

    private LancamentoRecorrenteResponse toResponse(LancamentoRecorrente rec) {
        return new LancamentoRecorrenteResponse(
                rec.getId(),
                rec.getCultura() != null ? rec.getCultura().getId() : null,
                rec.getCultura() != null ? rec.getCultura().getNome() : "Geral da Horta",
                rec.getTipo(),
                rec.getClassificacaoCusto(),
                rec.getCategoria(),
                rec.getDescricao(),
                rec.getValorPadrao(),
                rec.getDiaVencimento(),
                rec.isAtivo()
        );
    }
}
