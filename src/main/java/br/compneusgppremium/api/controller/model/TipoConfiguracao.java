package br.compneusgppremium.api.controller.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Enum que representa os tipos de configuração de máquina
 */
@Schema(description = "Tipos de configuração de máquina")
public enum TipoConfiguracao {
    
    @Schema(description = "Configurações do sistema")
    SISTEMA("Sistema"),
    
    @Schema(description = "Configurações de produção")
    PRODUCAO("Produção"),
    
    @Schema(description = "Configurações de qualidade")
    QUALIDADE("Qualidade"),
    
    @Schema(description = "Configurações de rede")
    REDE("Rede"),
    
    @Schema(description = "Configurações de segurança")
    SEGURANCA("Segurança");
    
    private final String descricao;
    
    TipoConfiguracao(String descricao) {
        this.descricao = descricao;
    }
    
    public String getDescricao() {
        return descricao;
    }
}