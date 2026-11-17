package br.com.agricultura.planejamentofinanceiro.controller;

import br.com.agricultura.planejamentofinanceiro.domain.entity.Usuario;
import br.com.agricultura.planejamentofinanceiro.domain.enums.PerfilUsuario;
import br.com.agricultura.planejamentofinanceiro.dto.AuthDtos.LoginRequest;
import br.com.agricultura.planejamentofinanceiro.dto.AuthDtos.LoginResponse;
import br.com.agricultura.planejamentofinanceiro.dto.AuthDtos.UsuarioResumoDto;
import br.com.agricultura.planejamentofinanceiro.repository.UsuarioRepository;
import br.com.agricultura.planejamentofinanceiro.security.JwtService;
import br.com.agricultura.planejamentofinanceiro.security.TotpService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticação", description = "Login com JWT e verificação TOTP obrigatória para o Gestor")
public class AuthController {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final TotpService totpService;
    private final JwtService jwtService;

    public AuthController(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            TotpService totpService,
            JwtService jwtService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.totpService = totpService;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    @Transactional(readOnly = true)
    @Operation(summary = "Autentica produtor ou gestor e retorna token JWT")
    public LoginResponse login(@Valid @RequestBody LoginRequest req) {
        Usuario usuario = usuarioRepository.findByLoginAndAtivoTrue(req.login().trim())
                .orElseThrow(() -> new BadCredentialsException("Login ou senha inválidos."));

        if (!passwordEncoder.matches(req.senha(), usuario.getSenhaHash())) {
            throw new BadCredentialsException("Login ou senha inválidos.");
        }

        if (usuario.getPerfil() == PerfilUsuario.GESTOR || usuario.isTotpHabilitado()) {
            if (req.codigoTotp() == null || req.codigoTotp().isBlank()) {
                throw new BadCredentialsException("Código TOTP (Google Authenticator) é obrigatório para o Gestor.");
            }
            if (!totpService.validarCodigo(usuario.getTotpSecret(), req.codigoTotp())) {
                throw new BadCredentialsException("Código TOTP inválido ou expirado.");
            }
        }

        String token = jwtService.gerarToken(usuario);
        UsuarioResumoDto resumo = new UsuarioResumoDto(
                usuario.getId(),
                usuario.getNome(),
                usuario.getLogin(),
                usuario.getPerfil(),
                usuario.getHorta().getId(),
                usuario.getHorta().getNome(),
                usuario.isTotpHabilitado()
        );
        return new LoginResponse(token, resumo);
    }
}
