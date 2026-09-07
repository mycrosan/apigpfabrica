package br.compneusgppremium.api.revisao.dto;
import java.time.Instant;
public record RespostaRevisaoDTO(String id, String campo, String transcricao, String legibilidade,
        boolean adjudicacao, String motivo, Instant criadaEm) { }
