package br.compneusgppremium.api.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Token de autenticação JWT")
public class TokenDto {

    @Schema(description = "Token JWT para autenticação", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String token;
    
    @Schema(description = "Tipo do token", example = "Bearer")
    private String tipo;
    
    @Schema(description = "Status da autenticação", example = "true")
    private Boolean status;

    public TokenDto(String token, String tipo, Boolean status) {
        this.token = token;
        this.tipo = tipo;
        this.status = status;
    }

    public String getToken() {
        return token;
    }

    public String getTipo() {
        return tipo;
    }

    public Boolean getStatus() {
        return status;
    }

}
