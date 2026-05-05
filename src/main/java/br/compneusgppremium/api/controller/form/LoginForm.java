package br.compneusgppremium.api.controller.form;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import javax.validation.constraints.NotBlank;

@Schema(description = "Dados para autenticação do usuário")
public class LoginForm {

	@Schema(description = "Email ou login do usuário", example = "usuario@gppremium.com.br", required = true)
	@NotBlank(message = "Login é obrigatório")
	private String login;
	
	@Schema(description = "Senha do usuário", example = "123456", required = true)
	@NotBlank(message = "Senha é obrigatória")
	private String senha;

	public String getLogin() {
		return login;
	}

	public String getSenha() {
		return senha;
	}

	public void setLogin(String email) {
		this.login = email;
	}

	public void setSenha(String senha) {
		this.senha = senha;
	}

	public UsernamePasswordAuthenticationToken converter() {
		return new UsernamePasswordAuthenticationToken(login, senha);
	}

}
