package br.compneusgppremium.api.controller.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity(name = "maquina_registro")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Modelo representando o registro de uma máquina no sistema")
public class RegistroMaquinaModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "ID único do registro da máquina", example = "1")
    private Long id;

    @Column(name = "nome", nullable = false, length = 100)
    @Schema(description = "Nome da máquina", example = "Máquina de Produção 01", required = true)
    private String nome;

    @Column(name = "numero_serie", unique = true, length = 100)
    @Schema(description = "Número de série único da máquina", example = "SN-001-2024")
    private String numeroSerie;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Schema(description = "Status atual da máquina", example = "Ativa", defaultValue = "Ativa")
    private StatusMaquina status = StatusMaquina.Ativa;

    @Column(length = 250)
    @Schema(description = "Descrição detalhada da máquina", example = "Máquina responsável pela produção de pneus linha A")
    private String descricao;

    @Column(name = "dt_create", nullable = false)
    @Schema(description = "Data e hora de criação do registro")
    private LocalDateTime dtCreate;

    @Column(name = "dt_update")
    @Schema(description = "Data e hora da última atualização")
    private LocalDateTime dtUpdate;

    @Column(name = "dt_delete")
    @Schema(description = "Data e hora de exclusão (soft delete)")
    private LocalDateTime dtDelete;

    @PrePersist
    protected void onCreate() {
        dtCreate = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        dtUpdate = LocalDateTime.now();
    }

    // Método para soft delete
    public void softDelete() {
        this.dtDelete = LocalDateTime.now();
        this.status = StatusMaquina.Inativa;
    }

    // Método para verificar se está deletado
    public boolean isDeleted() {
        return dtDelete != null;
    }

    // Método para colocar em manutenção
    public void colocarEmManutencao() {
        this.status = StatusMaquina.Manutencao;
        this.dtUpdate = LocalDateTime.now();
    }

    // Método para ativar máquina
    public void ativar() {
        this.status = StatusMaquina.Ativa;
        this.dtUpdate = LocalDateTime.now();
    }

    // Método para inativar máquina
    public void inativar() {
        this.status = StatusMaquina.Inativa;
        this.dtUpdate = LocalDateTime.now();
    }
}