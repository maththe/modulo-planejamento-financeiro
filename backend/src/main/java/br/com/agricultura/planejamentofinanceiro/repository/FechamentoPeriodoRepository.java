package br.com.agricultura.planejamentofinanceiro.repository;

import br.com.agricultura.planejamentofinanceiro.domain.entity.FechamentoPeriodo;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FechamentoPeriodoRepository extends JpaRepository<FechamentoPeriodo, UUID> {

    Optional<FechamentoPeriodo> findByProdutorIdAndAnoMes(UUID produtorId, String anoMes);

    boolean existsByProdutorIdAndAnoMes(UUID produtorId, String anoMes);

    List<FechamentoPeriodo> findByProdutorIdOrderByAnoMesAsc(UUID produtorId);
}
