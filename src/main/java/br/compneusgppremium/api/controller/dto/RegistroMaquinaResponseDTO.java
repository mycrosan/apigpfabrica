package br.compneusgppremium.api.controller.dto;

import br.compneusgppremium.api.controller.model.StatusMaquina;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "DTO de resposta para registro de máquina")
public class RegistroMaquinaResponseDTO {

    @Schema(description = "ID único do registro da máquina", example = "1")
    private Long id;

    @Schema(description = "Nome da máquina", example = "Máquina de Produção 01")
    private String nome;

    @Schema(description = "Número de série único da máquina", example = "SN-001-2024")
    private String numeroSerie;

    @Schema(description = "Status atual da máquina", example = "Ativa")
    private StatusMaquina status;

    @Schema(description = "Descrição detalhada da máquina", example = "Máquina responsável pela produção de pneus linha A")
    private String descricao;

    @Schema(description = "Data e hora de criação do registro")
    private LocalDateTime dtCreate;

    @Schema(description = "Data e hora da última atualização")
    private LocalDateTime dtUpdate;
}