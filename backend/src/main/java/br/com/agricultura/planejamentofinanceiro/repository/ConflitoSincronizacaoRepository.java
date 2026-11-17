package br.com.agricultura.planejamentofinanceiro.repository;

import br.com.agricultura.planejamentofinanceiro.domain.entity.ConflitoSincronizacao;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConflitoSincronizacaoRepository extends JpaRepository<ConflitoSincronizacao, UUID> {

    List<ConflitoSincronizacao> findByProdutorIdOrderByResolvidoEmDesc(UUID produtorId);
}
