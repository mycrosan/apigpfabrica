package br.compneusgppremium.api.controller.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "cola")
@Data
@Schema(description = "Modelo representando o processo de colagem de pneus")
public class ColaModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "ID único do processo de cola", example = "1")
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "producao_id", nullable = false)
    @Schema(description = "Produção associada ao processo de cola")
    private ProducaoModel producao;

    @Column(name = "data_inicio", nullable = false, insertable = false, updatable = true)
    @Schema(description = "Data e hora de início do processo de cola")
    private LocalDateTime dataInicio;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Schema(description = "Status atual do processo de cola", example = "Aguardando")
    private StatusCola status = StatusCola.Aguardando;

    @ManyToOne
    @JoinColumn(name = "usuario_id")
    @Schema(description = "Usuário responsável pelo processo de cola")
    private UsuarioModel usuario;

    @Schema(description = "Status possíveis para o processo de cola")
    public enum StatusCola {
        @Schema(description = "Aguardando início do processo")
        Aguardando,
        @Schema(description = "Processo pronto para próxima etapa")
        Pronto,
        @Schema(description = "Processo vencido")
        Vencido
    }
}
