package br.compneusgppremium.api.controller.dto;
import org.springframework.security.core.AuthenticationException;

public class RespostaDto {

	private String mensagem;
	private String error;

	public RespostaDto(String mensagem, String error) {
		this.mensagem = mensagem;
		this.error = error;
	}

	public String getMensagem() {
		return mensagem;
	}

	public String getError() {
		return error;
	}

}
