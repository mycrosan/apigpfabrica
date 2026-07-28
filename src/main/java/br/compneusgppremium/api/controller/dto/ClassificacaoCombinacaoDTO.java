package br.compneusgppremium.api.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Classificação da combinação modelo+medida+país de uma carcaça, "
        + "usada para impedir cadastro de um pneu que a fábrica não sabe produzir")
public class ClassificacaoCombinacaoDTO {

    @Schema(description = "VERDE (existe regra de produção), AMARELO (já cadastrado antes mas sem regra) "
            + "ou VERMELHO (combinação nunca vista — exige confirmação explícita)",
            example = "VERDE")
    private String classificacao;

    @Schema(description = "Mensagem explicando a classificação, para exibir ao operador")
    private String mensagem;
}
