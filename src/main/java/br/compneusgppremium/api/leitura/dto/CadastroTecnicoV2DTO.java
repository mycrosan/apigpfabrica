package br.compneusgppremium.api.leitura.dto;
import jakarta.validation.constraints.*;
import java.util.UUID;
public record CadastroTecnicoV2DTO(@NotNull UUID sessaoId, @NotNull @PositiveOrZero Long versaoSessao,
        @NotBlank @Size(max = 255) String numeroEtiqueta, boolean confirmarCombinacaoNova,
        @Size(max = 1024) String motivoCombinacaoNova) {}
