package br.compneusgppremium.api.leitura.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Encerramento deliberado de uma sessão de leitura sem cadastro.
 *
 * <p>O motivo é obrigatório: uma sessão abandonada em silêncio some da fila sem explicar se o pneu
 * foi recusado, se a foto era impossível ou se o operador desistiu — justamente a informação que a
 * revisão posterior precisa. A versão evita abandonar uma sessão que outro dispositivo acabou de
 * avançar.</p>
 *
 * @param motivo justificativa do operador, preservada junto da sessão.
 * @param versaoSessao versão conhecida pelo cliente no momento do abandono.
 */
public record AbandonarSessaoDTO(@NotBlank @Size(max = 1024) String motivo, @NotNull Long versaoSessao) {
}
