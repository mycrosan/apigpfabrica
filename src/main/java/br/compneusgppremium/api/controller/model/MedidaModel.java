package br.compneusgppremium.api.controller.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.persistence.*;

@Entity(name = "medida")
@Data
@Schema(description = "Modelo representando uma medida de pneu")
public class MedidaModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "ID único da medida", example = "1")
    public Integer id;
    
    @Schema(description = "Descrição da medida", example = "205/55R16")
    public String descricao;
}
