package br.compneusgppremium.api.controller.form;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Foto de perto de UM campo do pneu (marca, modelo, medida, país ou DOT) para leitura automática por IA")
public class LeituraCampoForm {
    @Schema(description = "Campo a reconhecer nesta foto", example = "MARCA", required = true,
            allowableValues = {"DOT", "MARCA", "MODELO", "MEDIDA", "PAIS"})
    @jakarta.validation.constraints.NotBlank
    @jakarta.validation.constraints.Pattern(regexp = "DOT|MARCA|MODELO|MEDIDA|PAIS")
    private String campo;

    @Schema(description = "Foto em base64 (com ou sem prefixo data:image/...;base64,)", required = true)
    @jakarta.validation.constraints.NotBlank
    @jakarta.validation.constraints.Size(max = 11185000)
    private String foto_base64;

    @Schema(description = "ID da marca já resolvida (IA ou manual), usado como contexto para escopar o "
            + "reconhecimento do MODELO. Ignorado para os demais campos.")
    private Integer marcaIdContexto;

    @Schema(description = "ID do modelo já resolvido (IA ou manual), usado como sinal de plausibilidade para "
            + "a MEDIDA. Ignorado para os demais campos.")
    private Integer modeloIdContexto;
}
