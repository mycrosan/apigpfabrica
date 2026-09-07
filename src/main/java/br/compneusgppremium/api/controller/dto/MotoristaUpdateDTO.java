package br.compneusgppremium.api.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import lombok.Data;

@Data
public class MotoristaUpdateDTO {
    @NotBlank
    private String nome;

    private String telefone;

    @Pattern(regexp = "^[A-Z0-9]{7}$", message = "Placa deve conter 7 caracteres alfanuméricos")
    private String placaVeiculo;

    @Size(max = 1024)
    private String observacoes;
}

