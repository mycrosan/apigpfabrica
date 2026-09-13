package br.compneusgppremium.api.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(description = "Item do catálogo compatível com o texto lido na foto. É uma opção para o operador "
        + "escolher, não uma decisão do sistema: só a lista com um único item e campo aprovado vira sugestão.")
public class CandidatoLeituraDTO {

    @Schema(description = "ID do item no catálogo. Nulo no campo DOT, que não tem catálogo.")
    private Integer id;

    @Schema(description = "Descrição cadastrada do item, exatamente como aparece no catálogo")
    private String descricao;

    @Schema(description = "Escore bruto do OCR na linha que originou o candidato. "
            + "Não é probabilidade de acerto do campo.")
    private Double escore;
}
