package br.compneusgppremium.api.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "Resultado do reconhecimento automático do pneu pela foto da lateral")
public class LeituraLateralDTO {

    @Schema(description = "De 1 a 4 candidatos de identificação do pneu, do mais provável para o menos provável. "
            + "Com 1 item só, o app pode pré-preencher direto; com mais de 1, o operador escolhe.")
    private List<CandidatoPneu> candidatos;

    @Schema(description = "ID do país no catálogo (null se não reconhecido)")
    private Integer paisId;
    @Schema(description = "Texto do país lido na lateral", example = "BRASIL")
    private String paisTexto;
    @Schema(description = "Confiança da leitura do país: ALTA, MEDIA ou BAIXA")
    private String confiancaPais;

    @Schema(description = "4 últimos dígitos do DOT (semana + ano)", example = "2323")
    private String dot;
    @Schema(description = "Código DOT completo lido", example = "DOT B94W 00RX 2323")
    private String dotCompleto;
    @Schema(description = "Confiança da leitura do DOT: ALTA, MEDIA ou BAIXA")
    private String confiancaDot;

    @Data
    @Schema(description = "Um candidato de identificação do pneu (marca + modelo + medida)")
    public static class CandidatoPneu {
        @Schema(description = "ID da marca no catálogo (null se não reconhecida)")
        private Integer marcaId;
        @Schema(description = "Texto da marca reconhecido", example = "MICHELIN")
        private String marcaTexto;

        @Schema(description = "ID do modelo no catálogo (null se não reconhecido)")
        private Integer modeloId;
        @Schema(description = "Texto do modelo reconhecido", example = "X MULTI D")
        private String modeloTexto;

        @Schema(description = "ID da medida no catálogo (null se não reconhecida)")
        private Integer medidaId;
        @Schema(description = "Texto da medida lido na lateral", example = "205/55R16")
        private String medidaTexto;

        @Schema(description = "Confiança deste candidato: ALTA, MEDIA ou BAIXA")
        private String confianca;
    }
}
