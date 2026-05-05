package br.compneusgppremium.api.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Resposta para dispositivo Sonoff")
public class SonoffResponseDTO {
    @Schema(description = "ID do dispositivo", example = "1")
    private Long id;
    @Schema(description = "Nome do dispositivo", example = "Sonoff Sala")
    private String nome;
    @Schema(description = "IP do dispositivo", example = "192.168.0.50")
    private String ip;
    @Schema(description = "Máquina associada", example = "Maquina 1")
    private String maquina;
    @Schema(description = "Data de criação")
    private LocalDateTime dtCreate;
    @Schema(description = "Data de atualização")
    private LocalDateTime dtUpdate;
}