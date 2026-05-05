package br.compneusgppremium.api.controller.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

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

