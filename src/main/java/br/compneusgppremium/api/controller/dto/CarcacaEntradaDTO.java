package br.compneusgppremium.api.controller.dto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CarcacaEntradaDTO {
    @NotBlank @Size(max = 255) private String numero_etiqueta;
    @NotBlank @Pattern(regexp = "[0-9]{4}") private String dot;
    @Valid private ReferenciaPneuDTO marca;
    @NotNull @Valid private ReferenciaPneuDTO modelo;
    @NotNull @Valid private ReferenciaPneuDTO medida;
    @NotNull @Valid private ReferenciaPneuDTO pais;
    @Size(max = 65535) private String fotos;
    @Size(max = 65535) private String leituraMetadados;
}
