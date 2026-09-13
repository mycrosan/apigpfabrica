package br.compneusgppremium.api.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Resultado do reconhecimento automático de UM campo do pneu (marca, modelo, medida, "
        + "país ou DOT), a partir de uma foto de perto daquela parte específica")
public class LeituraCampoDTO {

    @Schema(description = "ID do item no catálogo (marca/modelo/medida/país), ou null se não reconhecido. "
            + "Não se aplica ao campo DOT.")
    private Integer id;
    @Schema(description = "Texto reconhecido na foto, ou null se ilegível")
    private String texto;

    @Schema(description = "4 últimos dígitos do DOT (semana + ano), só preenchido quando campo=DOT", example = "2323")
    private String dot;
    @Schema(description = "Código DOT completo lido, só preenchido quando campo=DOT", example = "DOT B94W 00RX 2323")
    private String dotCompleto;

    @Schema(description = "Confiança da leitura: ALTA, MEDIA ou BAIXA")
    private String confianca;

    @Schema(description = "Motivo da ausência de sugestão e orientação para o operador")
    private String mensagem;

    @Schema(description = "Estado da decisão: SUGESTAO, AMBIGUA, ILEGIVEL, FORA_CATALOGO, "
            + "CONTEXTO_INVALIDO ou ERRO_TECNICO")
    private String estado;

    @Schema(description = "Itens do catálogo compatíveis com a leitura, para o operador ESCOLHER. "
            + "Vem preenchido mesmo quando não há sugestão; escolher é ação do operador, nunca do sistema.")
    private java.util.List<CandidatoLeituraDTO> candidatos = java.util.List.of();

    @Schema(description = "Itens PRÓXIMOS do que foi lido, quando nada bateu exatamente — a leitura "
            + "saiu com um ou dois caracteres trocados. Lista à parte de propósito: item daqui nunca "
            + "vira sugestão automática, só aparece para o operador conferir na foto e escolher.")
    private java.util.List<CandidatoLeituraDTO> candidatosAproximados = java.util.List.of();
}
