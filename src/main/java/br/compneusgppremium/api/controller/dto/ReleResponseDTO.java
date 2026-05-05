package br.compneusgppremium.api.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Resposta para dispositivo Rele")
public class ReleResponseDTO {
    @Schema(description = "ID do dispositivo", example = "1")
    private Long id;

    @Schema(description = "IP do dispositivo", example = "192.168.0.50")
    private String ip;

    @Schema(description = "ID do celular associado", example = "CEL001")
    private String celularId;

    @Schema(description = "ID da máquina associada (FK)", example = "1")
    private Long maquinaId;

    @Schema(description = "Nome da máquina associada", example = "Maquina 1")
    private String maquinaNome;

    @Schema(description = "Data de criação")
    private LocalDateTime dtCreate;

    @Schema(description = "Data de atualização")
    private LocalDateTime dtUpdate;
}