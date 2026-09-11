package br.compneusgppremium.api.controller.model;

import br.compneusgppremium.api.util.JpaConverterJson;
import lombok.Data;

import jakarta.persistence.*;

@Data
@Entity(name = "valida_regra")
public class ValidaRegraModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(columnDefinition = "TEXT")
    @Convert(converter = JpaConverterJson.class)
    private String dados;

    @Column
    private Boolean status;
//
    @ManyToOne
    private QualidadeModel qualidade;
//
    @ManyToOne
    private RegraModel regra;
//
    @ManyToOne
    private TipoValidaRegraModel tipo_valida_regra;
}
