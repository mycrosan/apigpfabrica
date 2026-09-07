package br.compneusgppremium.api.leitura.dto;
import br.compneusgppremium.api.service.LeituraCarcacaService.Campo;
import jakarta.validation.constraints.*;
public record NovaTentativaDTO(@NotNull Campo campo, @NotBlank @Size(max = 11185000) String foto_base64,
        @Positive Integer marcaIdContexto, @Positive Integer modeloIdContexto, @NotNull @PositiveOrZero Long versaoSessao) {}
