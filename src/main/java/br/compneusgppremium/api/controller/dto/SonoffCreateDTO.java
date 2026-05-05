package br.compneusgppremium.api.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
@Schema(description = "Dados para criação/atualização de dispositivo Sonoff")
public class SonoffCreateDTO {

    @NotBlank(message = "Nome é obrigatório")
    @Schema(description = "Nome do dispositivo", example = "Sonoff Sala")
    private String nome;

    @NotBlank(message = "IP é obrigatório")
    @Schema(description = "IP do dispositivo", example = "192.168.0.50")
    private String ip;

    @NotBlank(message = "Máquina é obrigatória")
    @Schema(description = "Nome da máquina associada", example = "Maquina 1")
    private String maquina;
}