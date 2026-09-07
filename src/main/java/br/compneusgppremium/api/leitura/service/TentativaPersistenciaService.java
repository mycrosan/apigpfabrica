package br.compneusgppremium.api.leitura.service;
import br.compneusgppremium.api.controller.dto.ResultadoLeituraDTO;
import br.compneusgppremium.api.leitura.dto.NovaTentativaDTO;
import br.compneusgppremium.api.leitura.dto.TentativaLeituraDTO;
import br.compneusgppremium.api.leitura.model.ImagemLeitura;
import br.compneusgppremium.api.leitura.model.TentativaLeitura;
import br.compneusgppremium.api.leitura.repository.ImagemLeituraRepository;
import br.compneusgppremium.api.leitura.repository.TentativaLeituraRepository;
import java.time.Clock;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TentativaPersistenciaService {
    private final SessaoLeituraService sessoes;
    private final TentativaLeituraRepository tentativas;
    private final ImagemLeituraRepository imagens;
    private final br.compneusgppremium.api.leitura.repository.ExecucaoLeituraRepository execucoes;
    private final LeituraJson json;
    private final Clock relogio;
    public TentativaLeituraDTO existente(final String sessaoId, final String chave, final String hash) {
        sessoes.buscar(sessaoId);
        return tentativas.findBySessaoIdAndChaveIdempotencia(sessaoId, chave).map(tentativa -> {
            if (!tentativa.getHashRequisicao().equals(hash)) {
                throw sessoes.conflito("Chave de tentativa reutilizada com outro conteúdo.");
            }
            if ("PROCESSANDO".equals(tentativa.getEstado())) {
                throw sessoes.conflito("Tentativa em processamento; consulte a sessão.");
            }
            return sessoes.respostaTentativa(tentativa);
        }).orElse(null);
    }
    @Transactional
    public String preparar(final String sessaoId, final String chave, final String hash, final NovaTentativaDTO entrada,
            final String arquivo, final String hashImagem, final long tamanho,
            final br.compneusgppremium.api.service.ImagemLeituraService.Metadados metadados) {
        var sessao = sessoes.bloquear(sessaoId, entrada.versaoSessao());
        if (tentativas.findBySessaoIdAndChaveIdempotencia(sessaoId, chave).isPresent()) {
            throw sessoes.conflito("Tentativa já registrada; consulte a sessão.");
        }
        var confirmacoes = sessoes.confirmacoes(sessao);
        Integer marca = confirmacoes.containsKey("MARCA") ? confirmacoes.get("MARCA").itemFinalId() : null;
        Integer modelo = confirmacoes.containsKey("MODELO") ? confirmacoes.get("MODELO").itemFinalId() : null;
        if (!java.util.Objects.equals(marca, entrada.marcaIdContexto())
                || !java.util.Objects.equals(modelo, entrada.modeloIdContexto())) {
            throw sessoes.conflito("Contexto não corresponde aos campos confirmados da sessão.");
        }
        ImagemLeitura imagem = new ImagemLeitura(); imagem.setId(UUID.randomUUID().toString());
        imagem.setSessaoId(sessaoId); imagem.setArquivo(arquivo); imagem.setSha256(hashImagem);
        imagem.setMime(metadados.mime()); imagem.setLargura(metadados.largura()); imagem.setAltura(metadados.altura());
        imagem.setTamanhoBytes(tamanho); imagem.setRecebidaEm(relogio.instant()); imagens.save(imagem);
        TentativaLeitura tentativa = new TentativaLeitura(); tentativa.setId(UUID.randomUUID().toString());
        tentativa.setSessaoId(sessaoId); tentativa.setImagemId(imagem.getId()); tentativa.setCampo(entrada.campo().name());
        tentativa.setChaveIdempotencia(chave); tentativa.setHashRequisicao(hash); tentativa.setEstado("PROCESSANDO");
        tentativa.setCriadaEm(relogio.instant()); tentativa.setVersaoSessao(entrada.versaoSessao());
        tentativa.setContextoJson(json.escrever(java.util.Arrays.asList(marca, modelo)));
        tentativas.saveAndFlush(tentativa); return tentativa.getId();
    }
    @Transactional
    public int recuperarInterrompidas(final java.time.Instant limite) {
        var pendentes = tentativas.findByEstadoAndCriadaEmBefore("PROCESSANDO", limite,
                org.springframework.data.domain.PageRequest.of(0, 100));
        int recuperadas = 0;
        for (var pendente : pendentes) {
            var tentativa = tentativas.bloquear(pendente.getId()).orElseThrow(sessoes::ausente);
            if (!"PROCESSANDO".equals(tentativa.getEstado())) { continue; }
            var resultado = new ResultadoLeituraDTO(tentativa.getCampo(), "ERRO_TECNICO",
                    java.util.List.of("RESULTADO_INCERTO"), "", null, null, java.util.List.of(), null, null,
                    "Leitura interrompida. Tire outra foto ou informe manualmente.", null);
            tentativa.setEstado(resultado.estado()); tentativa.setRespostaJson(json.escrever(resultado)); recuperadas++;
        }
        return recuperadas;
    }
    @Transactional
    public TentativaLeituraDTO concluir(final String id, final ResultadoLeituraDTO resultado,
            final br.compneusgppremium.api.leitura.dto.ExecucaoLeituraDTO execucao) {
        var tentativa = tentativas.bloquear(id).orElseThrow(sessoes::ausente);
        if (!"PROCESSANDO".equals(tentativa.getEstado())) {
            return sessoes.respostaTentativa(tentativa);
        }
        if (execucao != null) {
            var evidencia = new br.compneusgppremium.api.leitura.model.ExecucaoLeitura();
            evidencia.setTentativaId(id); evidencia.setSnapshotJson(json.escrever(execucao));
            evidencia.setHashCatalogo(ArquivosLeituraService.hash(json.escrever(execucao.catalogo()).getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            evidencia.setCriadaEm(relogio.instant()); execucoes.save(evidencia);
        }
        tentativa.setEstado(resultado.estado()); tentativa.setRespostaJson(json.escrever(resultado));
        return sessoes.respostaTentativa(tentativas.saveAndFlush(tentativa));
    }
}
