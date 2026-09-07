package br.compneusgppremium.api.leitura.dto;
import br.compneusgppremium.api.controller.dto.ResultadoLeituraDTO;
public record TentativaLeituraDTO(String tentativaId, String imagemId, String campo, String estado,
        Long versaoSessao, ResultadoLeituraDTO resultado) {}
