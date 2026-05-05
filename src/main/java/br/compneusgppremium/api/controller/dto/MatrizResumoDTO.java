package br.compneusgppremium.api.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(description = "DTO com informações resumidas da matriz")
public class MatrizResumoDTO {

    @Schema(description = "ID da matriz", example = "1")
    private Integer id;
    
    @Schema(description = "Descrição da matriz", example = "Matriz Principal")
    private String descricao;
}