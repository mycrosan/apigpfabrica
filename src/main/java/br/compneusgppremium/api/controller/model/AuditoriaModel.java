package br.compneusgppremium.api.controller.model;

import lombok.Data;

import javax.persistence.*;
import java.util.Date;

@Entity(name = "auditoria")
@Data
public class AuditoriaModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tabela_afetada", nullable = false)
    private String tabelaAfetada;

    @Column(name = "registro_id", nullable = false)
    private Long registroId;

    @Column(name = "acao", nullable = false)
    private String acao; // INSERT, UPDATE, INATIVAR

    @ManyToOne(optional = false)
    @JoinColumn(name = "usuario_id")
    private UsuarioModel usuario;

    @Column(name = "data_acao", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date dataAcao;
}

