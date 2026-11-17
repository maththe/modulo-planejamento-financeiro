package br.com.agricultura.planejamentofinanceiro.repository;

import br.com.agricultura.planejamentofinanceiro.domain.entity.LancamentoRecorrente;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LancamentoRecorrenteRepository extends JpaRepository<LancamentoRecorrente, UUID> {

    List<LancamentoRecorrente> findByProdutorIdAndAtivoTrueOrderByDiaVencimentoAsc(UUID produtorId);
}
