package br.compneusgppremium.api.controller.dto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
@JsonIgnoreProperties(ignoreUnknown = true)
public record ReferenciaPneuDTO(@NotNull @Positive Integer id) {}
