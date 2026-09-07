package br.compneusgppremium.api.revisao.service;
import br.compneusgppremium.api.exception.CadastroPneuException;
import br.compneusgppremium.api.util.UsuarioLogadoUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
@Service
@RequiredArgsConstructor
public class AcessoRevisaoService {
    private final UsuarioLogadoUtil usuario;
    public boolean permitido() { return possui("REVISAR_LEITURA"); }
    public int revisor() { exigir("REVISAR_LEITURA"); return usuario.getUsuarioIdLogado(); }
    public void importar() { exigir("IMPORTAR_LEITURA"); }
    private boolean possui(final String permissao) {
        var autenticacao = SecurityContextHolder.getContext().getAuthentication();
        return autenticacao != null && autenticacao.isAuthenticated()
                && autenticacao.getAuthorities().stream().anyMatch(a -> permissao.equals(a.getAuthority()));
    }
    private void exigir(final String permissao) {
        if (!possui(permissao)) {
            throw new CadastroPneuException("REVISAO_SEM_PERMISSAO",
                    "Seu usuário não tem permissão para esta ação de revisão.", HttpStatus.FORBIDDEN);
        }
    }
}
