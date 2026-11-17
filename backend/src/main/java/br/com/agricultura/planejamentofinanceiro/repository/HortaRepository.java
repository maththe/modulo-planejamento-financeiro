package br.com.agricultura.planejamentofinanceiro.repository;

import br.com.agricultura.planejamentofinanceiro.domain.entity.Horta;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HortaRepository extends JpaRepository<Horta, UUID> {
}
