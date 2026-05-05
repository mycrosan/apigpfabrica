package br.compneusgppremium.api.controller.model;

import br.compneusgppremium.api.util.JpaConverterJson;
import lombok.Data;
import org.springframework.stereotype.Service;

import javax.persistence.*;
import java.util.Date;
import java.util.UUID;

@Entity(name = "carcaca_rejeitada")
@Data
@Service
public class CarcacaRejeitadaModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    private ModeloModel modelo;

    @ManyToOne
    public MedidaModel medida;

    @ManyToOne
    public PaisModel pais;

    @Column
    private String motivo;

    @Column
    private String descricao;

    @Column
    private Date dt_create;

    @Column
    private Date dt_update;

    @Column
    private UUID uuid;
}
