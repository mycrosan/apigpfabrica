package br.compneusgppremium.api.controller.model;

import lombok.Data;

import jakarta.persistence.*;

@Entity(name = "modelo")
@Data
public class ModeloModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column
    private String descricao;
    @ManyToOne
    private MarcaModel marca;
}
