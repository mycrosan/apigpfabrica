package br.compneusgppremium.api.controller.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import javax.persistence.*;

@Entity(name = "marca")
@Data
@Schema(description = "Modelo representando uma marca de pneu")
public class MarcaModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "ID único da marca", example = "1")
    public Integer id;
    
    @Column
    @Schema(description = "Descrição da marca", example = "Michelin")
    public String descricao;
}
