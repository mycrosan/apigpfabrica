package br.compneusgppremium.api.leitura.dto;
import br.compneusgppremium.api.controller.dto.ResultadoLeituraDTO;
import br.compneusgppremium.api.ocr.OcrResposta;
import java.util.Map;
/**
 * Snapshot interno: nunca inclui token, endereço interno ou imagem em base64.
 *
 * <p>A extração é nula quando o serviço de OCR não chegou a responder; nesse caso o restante da
 * evidência (catálogo, política e identificação) ainda é preservado para separar indisponibilidade
 * de erro de reconhecimento.</p>
 */
public record ExecucaoLeituraDTO(ResultadoLeituraDTO resultado, OcrResposta extracao,
        Map<Integer, String> catalogo, String preprocessamento, String normalizacao, String politica,
        double escoreMinimo, boolean campoAprovado, IdentificacaoExecucaoDTO identificacao) {
    public ExecucaoLeituraDTO {
        catalogo = java.util.Collections.unmodifiableMap(new java.util.TreeMap<>(catalogo));
    }
}
