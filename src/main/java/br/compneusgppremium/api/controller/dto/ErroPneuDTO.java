package br.compneusgppremium.api.controller.dto;

import java.util.List;

public record ErroPneuDTO(int status, String codigo, String message, String debugMessage, List<String> erros) {
    public ErroPneuDTO {
        erros = List.copyOf(erros);
    }
}
