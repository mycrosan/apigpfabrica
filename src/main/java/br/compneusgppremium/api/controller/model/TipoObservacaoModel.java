package br.compneusgppremium.api.controller.model;

import lombok.Data;

import javax.persistence.*;

@Data
@Entity(name = "tipo_observacao")
public class TipoObservacaoModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private String descricao;
    @ManyToOne
    private TipoClassificacaoModel tipo_classificacao;
}
