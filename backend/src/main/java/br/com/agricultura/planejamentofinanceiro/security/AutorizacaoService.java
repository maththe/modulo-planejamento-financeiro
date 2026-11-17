package br.com.agricultura.planejamentofinanceiro.security;

import br.com.agricultura.planejamentofinanceiro.domain.entity.Usuario;
import br.com.agricultura.planejamentofinanceiro.domain.enums.PerfilUsuario;
import br.com.agricultura.planejamentofinanceiro.repository.UsuarioRepository;
import br.com.agricultura.planejamentofinanceiro.service.exception.AcessoNegadoException;
import br.com.agricultura.planejamentofinanceiro.service.exception.RecursoNaoEncontradoException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class AutorizacaoService {

    private final UsuarioRepository usuarioRepository;

    public AutorizacaoService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public Usuario resolverProdutorParaLeitura(UsuarioAutenticado auth, UUID produtorIdSolicitado) {
        if (auth == null) {
            throw new AcessoNegadoException("Usuário não autenticado.");
        }
        if (auth.perfil() == PerfilUsuario.PRODUTOR) {
            if (produtorIdSolicitado != null && !produtorIdSolicitado.equals(auth.id())) {
                throw new AcessoNegadoException("Produtor só pode acessar os próprios dados.");
            }
            return usuarioRepository.findById(auth.id())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Produtor não encontrado."));
        }

        if (produtorIdSolicitado == null) {
            List<Usuario> produtores = usuarioRepository
                    .findByHortaIdAndPerfilAndAtivoTrueOrderByNomeAsc(auth.hortaId(), PerfilUsuario.PRODUTOR);
            if (produtores.isEmpty()) {
                throw new RecursoNaoEncontradoException("Nenhum produtor ativo encontrado na horta.");
            }
            return produtores.get(0);
        }

        Usuario produtor = usuarioRepository.findById(produtorIdSolicitado)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Produtor não encontrado."));
        if (!produtor.getHorta().getId().equals(auth.hortaId())
                || produtor.getPerfil() != PerfilUsuario.PRODUTOR) {
            throw new AcessoNegadoException("Gestor só pode visualizar produtores da própria horta.");
        }
        return produtor;
    }

    public Usuario resolverProdutorParaEscrita(UsuarioAutenticado auth) {
        if (auth == null) {
            throw new AcessoNegadoException("Usuário não autenticado.");
        }
        if (auth.perfil() != PerfilUsuario.PRODUTOR) {
            throw new AcessoNegadoException("Gestor possui acesso apenas em modo leitura.");
        }
        return usuarioRepository.findById(auth.id())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Produtor não encontrado."));
    }
}
