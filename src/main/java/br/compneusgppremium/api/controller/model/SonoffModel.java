package br.compneusgppremium.api.controller.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity(name = "rele")
@Table(name = "rele")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Modelo representando um dispositivo Sonoff")
public class SonoffModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "ID único do dispositivo", example = "1")
    private Long id;

    // Removido campo 'nome' conforme novo requisito

    @Column(name = "ip", nullable = false, unique = true)
    @Schema(description = "IP do dispositivo", example = "192.168.0.50")
    private String ip;

    @Column(name = "celular_id", nullable = false, length = 100)
    @Schema(description = "ID do celular associado", example = "CEL001")
    private String celularId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "maquina_registro_id", nullable = false)
    @Schema(description = "Máquina associada (FK)")
    private RegistroMaquinaModel maquina;

    @Column(name = "dt_create", nullable = false)
    @Schema(description = "Data e hora de criação do registro")
    private LocalDateTime dtCreate;

    @Column(name = "dt_update")
    @Schema(description = "Data e hora da última atualização")
    private LocalDateTime dtUpdate;

    @Column(name = "dt_delete")
    @Schema(description = "Data e hora de exclusão lógica (soft delete)")
    private LocalDateTime dtDelete;

    @PrePersist
    protected void onCreate() {
        dtCreate = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        dtUpdate = LocalDateTime.now();
    }

    public void softDelete() {
        this.dtDelete = LocalDateTime.now();
    }
}