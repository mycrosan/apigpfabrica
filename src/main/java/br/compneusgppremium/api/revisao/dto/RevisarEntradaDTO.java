package br.compneusgppremium.api.revisao.dto;
import br.compneusgppremium.api.service.LeituraCarcacaService.Campo;
import jakarta.validation.constraints.*;
import java.util.List;
public record RevisarEntradaDTO(@NotNull @PositiveOrZero Long versao, Campo campo,
        @NotNull Legibilidade legibilidade, @Size(max = 512) String transcricao,
        @NotNull @Size(max = 4) List<@NotNull @Size(min = 2, max = 2) List<@NotNull @PositiveOrZero Integer>> regiao,
        @Size(max = 1024) String motivo) {
    public enum Legibilidade { LEGIVEL, ILEGIVEL, CAMPO_NAO_APARECE }
}
