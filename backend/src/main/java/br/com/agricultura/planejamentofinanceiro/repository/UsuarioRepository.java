package br.com.agricultura.planejamentofinanceiro.repository;

import br.com.agricultura.planejamentofinanceiro.domain.entity.Usuario;
import br.com.agricultura.planejamentofinanceiro.domain.enums.PerfilUsuario;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

    Optional<Usuario> findByLoginAndAtivoTrue(String login);

    List<Usuario> findByHortaIdAndPerfilAndAtivoTrueOrderByNomeAsc(UUID hortaId, PerfilUsuario perfil);
}
