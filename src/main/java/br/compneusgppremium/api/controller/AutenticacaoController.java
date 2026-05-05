package br.compneusgppremium.api.controller;

import br.compneusgppremium.api.controller.dto.RespostaDto;
import br.compneusgppremium.api.service.TokenService;
import br.compneusgppremium.api.controller.dto.TokenDto;
import br.compneusgppremium.api.controller.form.LoginForm;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticação", description = "Endpoints para autenticação de usuários")
public class AutenticacaoController {
	
	@Autowired
	private AuthenticationManager authManager;
	
	@Autowired
	private TokenService tokenService;

	@PostMapping
	@Operation(
		summary = "Autenticar usuário",
		description = "Realiza a autenticação do usuário e retorna um token JWT para acesso aos endpoints protegidos"
	)
	@ApiResponses(value = {
		@ApiResponse(
			responseCode = "200",
			description = "Autenticação realizada com sucesso",
			content = @Content(
				mediaType = "application/json",
				schema = @Schema(implementation = TokenDto.class),
				examples = @ExampleObject(
					name = "Sucesso",
					value = "{\n  \"token\": \"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...\",\n  \"tipo\": \"Bearer\",\n  \"status\": true\n}"
				)
			)
		),
		@ApiResponse(
			responseCode = "403",
			description = "Credenciais inválidas",
			content = @Content(
				mediaType = "text/plain",
				examples = @ExampleObject(
					name = "Erro de autenticação",
					value = "Verifique seu usuário e senha"
				)
			)
		),
		@ApiResponse(
			responseCode = "400",
			description = "Dados de entrada inválidos",
			content = @Content(
				mediaType = "application/json",
				examples = @ExampleObject(
					name = "Erro de validação",
					value = "{\n  \"login\": \"Campo obrigatório\",\n  \"senha\": \"Campo obrigatório\"\n}"
				)
			)
		)
	})
	public ResponseEntity autenticar(@RequestBody @Valid LoginForm form) {
		UsernamePasswordAuthenticationToken dadosLogin = form.converter();
		try {
			Authentication authentication = authManager.authenticate(dadosLogin);
			String token = tokenService.gerarToken(authentication);
			return ResponseEntity.ok(new TokenDto(token, "Bearer", true));
		} catch (AuthenticationException e) {
			System.out.println(e);
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Verifique seu usuário e senha");
		}
	}
	
}
