package br.com.agricultura.planejamentofinanceiro.dto;

import br.com.agricultura.planejamentofinanceiro.domain.enums.PerfilUsuario;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record LoginRequest(
            @NotBlank(message = "Informe o login.") String login,
            @NotBlank(message = "Informe a senha.") String senha,
            String codigoTotp
    ) {
    }

    public record UsuarioResumoDto(
            UUID id,
            String nome,
            String login,
            PerfilUsuario perfil,
            UUID hortaId,
            String hortaNome,
            boolean totpHabilitado
    ) {
    }

    public record LoginResponse(
            String token,
            UsuarioResumoDto usuario
    ) {
    }

    public record ProdutorHortaDto(
            UUID id,
            String nome,
            String login
    ) {
    }

    public record CulturaDto(
            UUID id,
            String nome,
            String icone,
            String unidade
    ) {
    }
}
