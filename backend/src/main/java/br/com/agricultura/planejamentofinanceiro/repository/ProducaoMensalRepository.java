package br.com.agricultura.planejamentofinanceiro.repository;

import br.com.agricultura.planejamentofinanceiro.domain.entity.ProducaoMensal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProducaoMensalRepository extends JpaRepository<ProducaoMensal, UUID> {

    List<ProducaoMensal> findByProdutorIdAndAnoMes(UUID produtorId, String anoMes);

    Optional<ProducaoMensal> findByProdutorIdAndCulturaIdAndAnoMes(UUID produtorId, UUID culturaId, String anoMes);
}
