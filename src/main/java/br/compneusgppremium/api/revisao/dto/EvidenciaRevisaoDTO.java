package br.compneusgppremium.api.revisao.dto;
import br.compneusgppremium.api.leitura.dto.ExecucaoLeituraDTO;
import br.compneusgppremium.api.ocr.OcrResposta;
/** Snapshot interno; não deve ser usado como resposta cega da API. */
public record EvidenciaRevisaoDTO(OcrResposta extracao, ExecucaoLeituraDTO execucao, int indiceLinha,
        String manifestoSha256) { }
