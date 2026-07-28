package br.compneusgppremium.api.controller.form;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Foto da lateral do pneu para leitura automática por IA")
public class LeituraLateralForm {
    @Schema(description = "Foto em base64 (com ou sem prefixo data:image/...;base64,)", required = true)
    private String foto_base64;
}
