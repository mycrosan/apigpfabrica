package br.compneusgppremium.api.controller.dto;

import lombok.Data;

@Data

public class ProducaoFilterDTO {
    private Long modeloId;
    private Long marcaId;
    private Long medidaId;
    private Long paisId;
    private String numeroEtiqueta;

    // Getters e Setters
}

