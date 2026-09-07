package br.compneusgppremium.api.leitura.dto;
import br.compneusgppremium.api.service.LeituraCarcacaService.Campo;
import jakarta.validation.constraints.*;
public record ConfirmarCampoDTO(@NotNull Campo campo, String tentativaId, @Positive Integer itemFinalId,
        @Size(max = 255) String valorFinal, @NotNull Origem origem, @Size(max = 1024) String motivo,
        @NotNull @PositiveOrZero Long versaoSessao) {
    public enum Origem { IA_CONFIRMADA, CORRIGIDA, MANUAL }
}
