package br.com.agricultura.planejamentofinanceiro.controller;

import br.com.agricultura.planejamentofinanceiro.domain.enums.PerfilUsuario;
import br.com.agricultura.planejamentofinanceiro.dto.AuthDtos.CulturaDto;
import br.com.agricultura.planejamentofinanceiro.dto.AuthDtos.ProdutorHortaDto;
import br.com.agricultura.planejamentofinanceiro.repository.CulturaRepository;
import br.com.agricultura.planejamentofinanceiro.repository.UsuarioRepository;
import br.com.agricultura.planejamentofinanceiro.security.UsuarioAutenticado;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/horta")
@Tag(name = "Horta", description = "Culturas e produtores da horta comunitária")
public class HortaController {

    private final CulturaRepository culturaRepository;
    private final UsuarioRepository usuarioRepository;

    public HortaController(CulturaRepository culturaRepository, UsuarioRepository usuarioRepository) {
        this.culturaRepository = culturaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping("/culturas")
    @Operation(summary = "Lista as culturas ativas da horta do usuário")
    public List<CulturaDto> listarCulturas(@AuthenticationPrincipal UsuarioAutenticado auth) {
        return culturaRepository.findByHortaIdAndAtivoTrueOrderByNomeAsc(auth.hortaId())
                .stream()
                .map(c -> new CulturaDto(c.getId(), c.getNome(), c.getIcone(), c.getUnidade()))
                .toList();
    }

    @GetMapping("/produtores")
    @Operation(summary = "Lista os produtores da horta (para visão somente leitura do gestor)")
    public List<ProdutorHortaDto> listarProdutores(@AuthenticationPrincipal UsuarioAutenticado auth) {
        if (auth.perfil() == PerfilUsuario.PRODUTOR) {
            return List.of(new ProdutorHortaDto(auth.id(), auth.nome(), auth.login()));
        }
        return usuarioRepository.findByHortaIdAndPerfilAndAtivoTrueOrderByNomeAsc(auth.hortaId(), PerfilUsuario.PRODUTOR)
                .stream()
                .map(u -> new ProdutorHortaDto(u.getId(), u.getNome(), u.getLogin()))
                .toList();
    }
}
