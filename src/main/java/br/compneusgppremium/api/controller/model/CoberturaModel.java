package br.compneusgppremium.api.controller.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "cobertura")
@Data
@Schema(description = "Modelo representando o processo de cobertura de pneus")
public class CoberturaModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "ID único do processo de cobertura", example = "1")
    private Integer id;

    @Column(columnDefinition = "json")
    @Schema(description = "URLs das fotos do processo de cobertura em formato JSON")
    private String fotos;

    @Column(name = "dt_create", insertable = false, updatable = false)
    @Schema(description = "Data e hora de criação do registro")
    private LocalDateTime dtCreate;

    @Column(name = "dt_update")
    @Schema(description = "Data e hora da última atualização")
    private LocalDateTime dtUpdate;

    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    @Schema(description = "Usuário responsável pelo processo de cobertura")
    private UsuarioModel usuario;

    @ManyToOne
    @JoinColumn(name = "cola_id", nullable = false)
    @Schema(description = "Processo de cola associado à cobertura")
    private ColaModel cola;
}
