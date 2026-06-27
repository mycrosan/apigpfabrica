package br.compneusgppremium.api.controller.dto;

import lombok.Data;

@Data

public class ProducaoFilterDTO {
    private Integer modeloId;
    private Integer marcaId;
    private Integer medidaId;
    private Integer paisId;
    private String numeroEtiqueta;

    // Getters e Setters
}
