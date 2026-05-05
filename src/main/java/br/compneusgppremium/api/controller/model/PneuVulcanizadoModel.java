package br.compneusgppremium.api.controller.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "pneus_vulcanizados")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Modelo representando um pneu vulcanizado")
public class PneuVulcanizadoModel {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "ID único do pneu vulcanizado", example = "1")
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    @Schema(description = "ID do usuário responsável pela vulcanização", example = "1")
    private Long usuarioId;

    @Column(name = "producao_id", nullable = false)
    @Schema(description = "ID da produção relacionada", example = "1")
    private Long producaoId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Schema(description = "Status da vulcanização", example = "INICIADO", allowableValues = {"INICIADO", "FINALIZADO"})
    private StatusVulcanizacao status = StatusVulcanizacao.INICIADO;

    @Column(name = "dt_create", nullable = false)
    @Schema(description = "Data e hora de criação do registro")
    private LocalDateTime dtCreate;

    @Column(name = "dt_update")
    @Schema(description = "Data e hora da última atualização")
    private LocalDateTime dtUpdate;

    @Column(name = "dt_delete")
    @Schema(description = "Data e hora de exclusão lógica (soft delete)")
    private LocalDateTime dtDelete;

    // Relacionamentos com outras entidades
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", insertable = false, updatable = false)
    @Schema(description = "Usuário responsável pela vulcanização")
    private UsuarioModel usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producao_id", insertable = false, updatable = false)
    @Schema(description = "Produção relacionada ao pneu vulcanizado")
    private ProducaoModel producao;

    @PrePersist
    protected void onCreate() {
        dtCreate = LocalDateTime.now();
        if (status == null) {
            status = StatusVulcanizacao.INICIADO;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        dtUpdate = LocalDateTime.now();
    }

    // Métodos de negócio
    public void softDelete() {
        this.dtDelete = LocalDateTime.now();
    }

    public boolean isDeleted() {
        return dtDelete != null;
    }

    public void finalizar() {
        this.status = StatusVulcanizacao.FINALIZADO;
        this.dtUpdate = LocalDateTime.now();
    }

    public boolean isFinalizado() {
        return status == StatusVulcanizacao.FINALIZADO;
    }

    public boolean isIniciado() {
        return status == StatusVulcanizacao.INICIADO;
    }

    /**
     * Enum para os status de vulcanização
     */
    public enum StatusVulcanizacao {
        INICIADO("Iniciado"),
        FINALIZADO("Finalizado");

        private final String descricao;

        StatusVulcanizacao(String descricao) {
            this.descricao = descricao;
        }

        public String getDescricao() {
            return descricao;
        }
    }
}