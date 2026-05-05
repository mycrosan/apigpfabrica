package br.compneusgppremium.api.controller.model;

import br.compneusgppremium.api.util.JpaConverterJson;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.persistence.*;
import java.util.Date;
import java.util.UUID;

@Entity(name = "producao")
@Data
@Schema(description = "Modelo representando uma produção de pneu reformado")
public class ProducaoModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "ID único da produção", example = "1")
    private Integer id;

    @ManyToOne
    @Schema(description = "Carcaça utilizada na produção")
    private CarcacaModel carcaca;

    @Column
    @Schema(description = "Medida do pneu após raspagem", example = "15.5")
    private Double medida_pneu_raspado;

    @Column
    @Convert(converter = JpaConverterJson.class)
    @Schema(description = "Dados adicionais da produção em formato JSON")
    public String dados;

    @ManyToOne
    @Schema(description = "Regra aplicada na produção")
    private RegraModel regra;

    @Column
    @Convert(converter = JpaConverterJson.class)
    @Schema(description = "URLs das fotos da produção em formato JSON")
    public String fotos;

    @Column
    @Schema(description = "Data de criação da produção")
    private Date dt_create;

    @Column
    @Schema(description = "Data de última atualização da produção")
    private Date dt_update;

    @Column
    @Schema(description = "UUID único da produção")
    private UUID uuid;

    @ManyToOne
    @JoinColumn(name = "usuario_id")
    @Schema(description = "Usuário que criou a produção")
    private UsuarioModel criadoPor;
}
