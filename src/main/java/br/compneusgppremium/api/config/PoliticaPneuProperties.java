package br.compneusgppremium.api.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.ZoneId;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("pneus.politica")
public record PoliticaPneuProperties(
        @NotNull ZoneId fuso,
        @Min(2000) @Max(2099) int anoMinimo,
        boolean validarSemanaFutura,
        @Min(0) @Max(2) int toleranciaSemanas,
        @NotEmpty Set<String> segmentos,
        @NotEmpty Set<String> familiasMedida) {
    public PoliticaPneuProperties {
        segmentos = Set.copyOf(segmentos);
        familiasMedida = Set.copyOf(familiasMedida);
    }
}
