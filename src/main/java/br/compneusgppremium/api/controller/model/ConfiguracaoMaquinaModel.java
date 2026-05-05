package br.compneusgppremium.api.controller.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * Entidade que representa a configuração de uma máquina
 */
@Entity(name = "maquina_configuracao")
@Table(name = "maquina_configuracao")
@Schema(description = "Configuração de máquina")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConfiguracaoMaquinaModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "ID único da configuração", example = "1")
    private Long id;

    @Column(name = "celular_id", nullable = false, length = 100)
    @Schema(description = "ID do celular", example = "CEL001")
    private String celularId;

    @Column(name = "descricao", length = 150)
    @Schema(description = "Descrição da configuração", example = "Configuração de produção")
    private String descricao;

    @Column(name = "atributos", columnDefinition = "JSON")
    @Schema(description = "Atributos da configuração em formato JSON")
    private String atributos;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "matriz_id", nullable = false)
    @Schema(description = "Matriz associada à configuração")
    private MatrizModel matriz;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "maquina_id", nullable = false)
    @Schema(description = "Máquina associada à configuração")
    private RegistroMaquinaModel maquina;

    @Column(name = "dt_create", nullable = false)
    @Schema(description = "Data de criação", example = "2025-09-27T10:30:00")
    private LocalDateTime dtCreate;

    @Column(name = "dt_update")
    @Schema(description = "Data de atualização", example = "2025-09-27T15:45:00")
    private LocalDateTime dtUpdate;

    @Column(name = "dt_delete")
    @Schema(description = "Data de exclusão (soft delete)", example = "null")
    private LocalDateTime dtDelete;

    @Column(name = "usuario_id", nullable = false)
    @Schema(description = "ID do usuário que criou a configuração", example = "1")
    private Long usuarioId;



    // Métodos de ciclo de vida
    @PrePersist
    protected void onCreate() {
        dtCreate = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        dtUpdate = LocalDateTime.now();
    }

    /**
     * Realiza soft delete da configuração
     */
    public void softDelete() {
        this.dtDelete = LocalDateTime.now();
    }

    /**
     * Verifica se a configuração foi deletada
     */
    public boolean isDeleted() {
        return dtDelete != null;
    }


}