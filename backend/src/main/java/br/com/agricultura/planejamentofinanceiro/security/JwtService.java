package br.com.agricultura.planejamentofinanceiro.security;

import br.com.agricultura.planejamentofinanceiro.domain.entity.Usuario;
import br.com.agricultura.planejamentofinanceiro.domain.enums.PerfilUsuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long expirationMs;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms}") long expirationMs) {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.expirationMs = expirationMs;
    }

    public String gerarToken(Usuario usuario) {
        Date agora = new Date();
        Date exp = new Date(agora.getTime() + expirationMs);
        return Jwts.builder()
                .subject(usuario.getId().toString())
                .claim("login", usuario.getLogin())
                .claim("nome", usuario.getNome())
                .claim("perfil", usuario.getPerfil().name())
                .claim("hortaId", usuario.getHorta().getId().toString())
                .issuedAt(agora)
                .expiration(exp)
                .signWith(signingKey)
                .compact();
    }

    public UsuarioAutenticado validarETrazerUsuario(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return new UsuarioAutenticado(
                UUID.fromString(claims.getSubject()),
                claims.get("login", String.class),
                claims.get("nome", String.class),
                PerfilUsuario.valueOf(claims.get("perfil", String.class)),
                UUID.fromString(claims.get("hortaId", String.class))
        );
    }
}
