package br.compneusgppremium.api.revisao.dto;
import br.compneusgppremium.api.ocr.OcrResposta;
import jakarta.validation.constraints.*;
public record ImportarRevisaoDTO(@NotBlank @Pattern(regexp = "[a-f0-9]{64}") String imagemSha256,
        @NotBlank @Pattern(regexp = "[a-f0-9]{64}") String manifestoSha256,
        @NotBlank @Size(max = 11184812) String fotoBase64, @NotNull OcrResposta extracao) { }
