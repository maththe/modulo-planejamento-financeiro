package br.com.agricultura.planejamentofinanceiro.repository;

import br.com.agricultura.planejamentofinanceiro.domain.entity.Lancamento;
import br.com.agricultura.planejamentofinanceiro.domain.enums.CategoriaLancamento;
import br.com.agricultura.planejamentofinanceiro.domain.enums.TipoLancamento;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LancamentoRepository extends JpaRepository<Lancamento, UUID> {

    Optional<Lancamento> findByChaveIdempotencia(String chaveIdempotencia);

    List<Lancamento> findByProdutorIdAndAnoMesAndExcluidoFalseOrderByDataLancamentoAscCriadoEmAsc(
            UUID produtorId, String anoMes);

    List<Lancamento> findByProdutorIdAndExcluidoFalseOrderByAnoMesAscDataLancamentoAsc(UUID produtorId);

    @Query("SELECT DISTINCT l.anoMes FROM Lancamento l WHERE l.produtor.id = :produtorId AND l.excluido = false ORDER BY l.anoMes ASC")
    List<String> findDistinctAnoMesByProdutorId(@Param("produtorId") UUID produtorId);

    @Query("SELECT l.categoria, COUNT(l) as total FROM Lancamento l " +
           "WHERE l.produtor.id = :produtorId AND l.tipo = :tipo AND l.excluido = false " +
           "GROUP BY l.categoria ORDER BY total DESC")
    List<Object[]> findFrequenciaCategorias(@Param("produtorId") UUID produtorId, @Param("tipo") TipoLancamento tipo);

    long countByProdutorIdAndExcluidoFalse(UUID produtorId);

    long countByProdutorIdAndTipoAndCategoriaAndExcluidoFalse(
            UUID produtorId, TipoLancamento tipo, CategoriaLancamento categoria);
}
