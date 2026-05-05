package br.compneusgppremium.api.controller.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.persistence.*;

@Data
@Entity(name = "pais")
@Schema(description = "Modelo representando um país de origem")
public class PaisModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "ID único do país", example = "1")
    private Integer id;
    
    @Schema(description = "Nome do país", example = "Brasil")
    private String descricao;
}
