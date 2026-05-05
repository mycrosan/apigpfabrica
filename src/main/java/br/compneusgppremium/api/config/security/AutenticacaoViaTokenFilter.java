package br.compneusgppremium.api.config.security;

import br.compneusgppremium.api.repository.UsuarioRepository;
import br.compneusgppremium.api.service.TokenService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class AutenticacaoViaTokenFilter extends OncePerRequestFilter {

	private final TokenService tokenService;
	private final UsuarioRepository repository;

	public AutenticacaoViaTokenFilter(TokenService tokenService, UsuarioRepository repository) {
		this.tokenService = tokenService;
		this.repository = repository;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request,
									HttpServletResponse response,
									FilterChain filterChain) throws ServletException, IOException {

		String token = recuperarToken(request);

		if (token != null && tokenService.isTokenValido(token)) {
			autenticarCliente(token);
		}

		filterChain.doFilter(request, response);
	}

	private void autenticarCliente(String token) {
		try {
			Long idUsuario = tokenService.getIdUsuario(token);

			repository.findById(idUsuario).ifPresent(usuarioModel -> {
				UsernamePasswordAuthenticationToken authentication =
						new UsernamePasswordAuthenticationToken(
								usuarioModel,
								null,
								usuarioModel.getAuthorities()
						);
				SecurityContextHolder.getContext().setAuthentication(authentication);
			});
		} catch (Exception e) {
			// Evita quebra geral da aplicação em caso de falha inesperada
			System.err.println("Erro ao autenticar usuário via token: " + e.getMessage());
		}
	}

	private String recuperarToken(HttpServletRequest request) {
		String token = request.getHeader("Authorization");
		if (token == null || token.isBlank() || !token.startsWith("Bearer ")) {
			return null;
		}
		return token.substring(7);
	}
}
