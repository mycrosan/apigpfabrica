package br.compneusgppremium.api.revisao.service;
import br.compneusgppremium.api.exception.CadastroPneuException;
import br.compneusgppremium.api.util.UsuarioLogadoUtil;
import java.util.List;
import org.junit.jupiter.api.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class AcessoRevisaoServiceTest {
    @AfterEach void limpar() { SecurityContextHolder.clearContext(); }
    @Test void permissaoExataSemPromoverAdministradorImplicitamente() {
        var identidade = mock(UsuarioLogadoUtil.class);
        var acesso = new AcessoRevisaoService(identidade);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("1", null,
                List.of(new SimpleGrantedAuthority("ADMIN"))));
        assertThat(acesso.permitido()).isFalse();
        assertThatThrownBy(acesso::revisor).isInstanceOf(CadastroPneuException.class);
        verifyNoInteractions(identidade);
    }
}
