package br.compneusgppremium.api.controller.dto;

import br.compneusgppremium.api.controller.model.PneuVulcanizadoModel.StatusVulcanizacao;
import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.constraints.NotNull;

/**
 * DTO para atualização de pneu vulcanizado
 */
@Schema(description = "Dados para atualização de pneu vulcanizado")
public class PneuVulcanizadoUpdateDTO {

    @NotNull(message = "Status é obrigatório")
    @Schema(description = "Status da vulcanização", example = "FINALIZADO", required = true, allowableValues = {"INICIADO", "FINALIZADO"})
    private StatusVulcanizacao status;

    // Construtores
    public PneuVulcanizadoUpdateDTO() {}

    public PneuVulcanizadoUpdateDTO(StatusVulcanizacao status) {
        this.status = status;
    }

    // Getters e Setters
    public StatusVulcanizacao getStatus() {
        return status;
    }

    public void setStatus(StatusVulcanizacao status) {
        this.status = status;
    }
}