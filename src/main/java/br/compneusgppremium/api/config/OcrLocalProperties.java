package br.compneusgppremium.api.config;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
@Validated
@ConfigurationProperties("pneus.ocr")
public record OcrLocalProperties(@NotNull URI url, String token, @Min(1) @Max(60) int timeoutSegundos,
        boolean habilitado, @NotNull Set<String> camposAprovados,
        @jakarta.validation.constraints.DecimalMin("0.0") @jakarta.validation.constraints.DecimalMax("1.0") double escoreMinimo,
        @Min(1024) int limiteBytes, @Min(1024) long limitePixels,
        @Min(1) int falhasParaAbrir, @Min(1) int aberturaSegundos) {
    public OcrLocalProperties {
        camposAprovados = Set.copyOf(camposAprovados);
    }
}
