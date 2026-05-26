package br.compneusgppremium.api.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "DTO com informações do usuário responsável")
public class UsuarioResponsavelDTO {
    @Schema(description = "ID do usuário", example = "1")
    private Long id;
    
    @Schema(description = "Nome do usuário", example = "Maria Santos")
    private String nome;

    @Schema(description = "Login do usuário", example = "maria.santos")
    private String login;

    public UsuarioResponsavelDTO(Long id, String nome) {
        this.id = id;
        this.nome = nome;
    }

    public UsuarioResponsavelDTO(Long id, String nome, String login) {
        this.id = id;
        this.nome = nome;
        this.login = login;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getLogin() { return login; }
    public void setLogin(String login) { this.login = login; }
}
