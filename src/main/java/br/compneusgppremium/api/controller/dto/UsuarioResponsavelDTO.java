package br.compneusgppremium.api.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "DTO com informações do usuário responsável")
public class UsuarioResponsavelDTO {
    @Schema(description = "ID do usuário", example = "1")
    private Long id;
    
    @Schema(description = "Nome do usuário", example = "Maria Santos")
    private String nome;

    public UsuarioResponsavelDTO(Long id, String nome) {
        this.id = id;
        this.nome = nome;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
}
