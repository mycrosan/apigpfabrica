package br.compneusgppremium.api.controller.model;

import lombok.Data;

import javax.persistence.*;
import java.util.Date;

@Entity(name = "motorista")
@Data
public class MotoristaModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false, unique = true, length = 11)
    private String cpf;

    @Column
    private String telefone;

    @Column(name = "placa_veiculo")
    private String placaVeiculo;

    @Column(length = 1024)
    private String observacoes;

    @OneToOne(optional = false)
    @JoinColumn(name = "usuario_id", unique = true)
    private UsuarioModel usuario;

    @Column(nullable = false)
    private Boolean ativo = true;

    @Column(name = "data_criacao")
    @Temporal(TemporalType.TIMESTAMP)
    private Date dataCriacao;

    @Column(name = "data_atualizacao")
    @Temporal(TemporalType.TIMESTAMP)
    private Date dataAtualizacao;
}

