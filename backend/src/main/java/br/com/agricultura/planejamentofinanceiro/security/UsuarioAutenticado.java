package br.com.agricultura.planejamentofinanceiro.security;

import br.com.agricultura.planejamentofinanceiro.domain.enums.PerfilUsuario;
import java.util.UUID;

public record UsuarioAutenticado(
        UUID id,
        String login,
        String nome,
        PerfilUsuario perfil,
        UUID hortaId
) {
}
