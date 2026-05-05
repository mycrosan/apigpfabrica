package br.compneusgppremium.api.controller.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Status possíveis para uma máquina registrada")
public enum StatusMaquina {
    @Schema(description = "Máquina ativa e operacional")
    Ativa,
    
    @Schema(description = "Máquina inativa ou desligada")
    Inativa,
    
    @Schema(description = "Máquina em manutenção")
    Manutencao
}