package br.compneusgppremium.api.leitura.dto;

/**
 * Identifica o build e a configuração de decisão vigentes quando a leitura foi executada.
 *
 * <p>Sem esses dois campos uma evidência antiga não pode ser reinterpretada: o mesmo texto extraído
 * produz decisões diferentes conforme o escore mínimo, os campos aprovados e a política de DOT em
 * vigor. O hash cobre apenas a configuração que altera a decisão, nunca endereços ou credenciais.</p>
 *
 * @param versaoAplicacao versão do artefato que executou a leitura.
 * @param hashConfiguracao resumo SHA-256 da configuração de decisão vigente.
 */
public record IdentificacaoExecucaoDTO(String versaoAplicacao, String hashConfiguracao) {
}
