package br.com.agricultura.planejamentofinanceiro.repository;

import br.com.agricultura.planejamentofinanceiro.domain.entity.Cultura;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CulturaRepository extends JpaRepository<Cultura, UUID> {

    List<Cultura> findByHortaIdAndAtivoTrueOrderByNomeAsc(UUID hortaId);
}
