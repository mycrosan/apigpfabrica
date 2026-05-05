package br.compneusgppremium.api.controller.model;

import br.compneusgppremium.api.util.JpaConverterJson;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.persistence.*;

import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.UUID;

@Entity(name = "carcaca")
@Data
@Schema(description = "Modelo representando uma carcaça de pneu")
public class CarcacaModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "ID único da carcaça", example = "1")
    private Integer id;

    @Column(unique = true)
    @Schema(description = "Número da etiqueta da carcaça", example = "CAR001")
    private String numero_etiqueta;

    @Column
    @Schema(description = "Código DOT da carcaça", example = "DOT1234")
    private String dot;

    @Column
    @Schema(description = "Status da carcaça", example = "Ativo")
    private String status;

    @Column
    @Convert(converter = JpaConverterJson.class)
    @Schema(description = "Dados adicionais da carcaça em formato JSON")
    private String dados;

    @ManyToOne
    @Schema(description = "Modelo da carcaça")
    private ModeloModel modelo;

    @ManyToOne
    @Schema(description = "Medida da carcaça")
    public MedidaModel medida;

    @ManyToOne
    @Schema(description = "País de origem da carcaça")
    public PaisModel pais;

    @Column
    @Convert(converter = JpaConverterJson.class)
    @Schema(description = "URLs das fotos da carcaça em formato JSON")
    public String fotos;

    @ManyToOne
    @Schema(description = "Status específico da carcaça")
    public StatusCarcacaModel status_carcaca;

    @Column
    @Schema(description = "Data de criação da carcaça")
    private Date dt_create;

    @Column
    @Schema(description = "Data de última atualização da carcaça")
    private Date dt_update;

    @Column
    @Schema(description = "UUID único da carcaça")
    private UUID uuid;
}
