package br.compneusgppremium.api.controller.dto;

import lombok.Value;

@Value
public class MotoristaResponseDTO {
    Integer id;
    String nome;
    String cpf;
    String telefone;
    String placaVeiculo;
    String observacoes;
    Integer usuarioId;
    Boolean ativo;
}

