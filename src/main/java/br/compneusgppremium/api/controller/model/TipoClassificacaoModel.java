package br.compneusgppremium.api.controller.model;

import lombok.Data;

import javax.persistence.*;

@Data
@Entity(name = "tipo_classificacao")
public class TipoClassificacaoModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private String descricao;
}
