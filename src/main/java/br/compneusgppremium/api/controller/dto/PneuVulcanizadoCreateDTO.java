package br.compneusgppremium.api.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.constraints.NotNull;

/**
 * DTO para criação de pneu vulcanizado
 */
@Schema(description = "Dados para criação de pneu vulcanizado")
public class PneuVulcanizadoCreateDTO {

    @NotNull(message = "ID da produção é obrigatório")
    @Schema(description = "ID da produção relacionada", example = "1", required = true)
    private Integer producaoId;

    // Construtores
    public PneuVulcanizadoCreateDTO() {}

    public PneuVulcanizadoCreateDTO(Integer producaoId) {
        this.producaoId = producaoId;
    }

    // Getters e Setters

    public Integer getProducaoId() {
        return producaoId;
    }

    public void setProducaoId(Integer producaoId) {
        this.producaoId = producaoId;
    }
}