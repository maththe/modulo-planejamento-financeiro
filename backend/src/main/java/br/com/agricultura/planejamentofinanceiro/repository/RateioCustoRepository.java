package br.com.agricultura.planejamentofinanceiro.repository;

import br.com.agricultura.planejamentofinanceiro.domain.entity.RateioCusto;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RateioCustoRepository extends JpaRepository<RateioCusto, UUID> {

    List<RateioCusto> findByLancamentoId(UUID lancamentoId);
}
