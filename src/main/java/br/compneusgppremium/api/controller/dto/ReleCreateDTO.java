package br.compneusgppremium.api.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
@Schema(description = "Dados para criação/atualização de dispositivo Rele")
public class ReleCreateDTO {

    @NotBlank(message = "IP é obrigatório")
    @Schema(description = "IP do dispositivo", example = "192.168.0.50")
    private String ip;

    @NotBlank(message = "Celular ID é obrigatório")
    @Schema(description = "ID do celular associado ao dispositivo", example = "CEL001")
    private String celularId;

    @NotNull(message = "Máquina (ID) é obrigatória")
    @Schema(description = "ID da máquina associada (FK)", example = "1")
    private Long maquinaId;
}