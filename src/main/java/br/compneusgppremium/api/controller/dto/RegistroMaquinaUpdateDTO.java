package br.compneusgppremium.api.controller.dto;

import br.compneusgppremium.api.controller.model.StatusMaquina;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Size;

@Data
@Schema(description = "DTO para atualização de registro de máquina")
public class RegistroMaquinaUpdateDTO {

    @Size(max = 100, message = "Nome da máquina deve ter no máximo 100 caracteres")
    @Schema(description = "Nome da máquina", example = "Máquina de Produção 01")
    private String nome;

    @Size(max = 100, message = "Número de série deve ter no máximo 100 caracteres")
    @Schema(description = "Número de série único da máquina", example = "SN-001-2024")
    private String numeroSerie;

    @Schema(description = "Status atual da máquina", example = "Ativa")
    private StatusMaquina status;

    @Size(max = 250, message = "Descrição deve ter no máximo 250 caracteres")
    @Schema(description = "Descrição detalhada da máquina", example = "Máquina responsável pela produção de pneus linha A")
    private String descricao;
}