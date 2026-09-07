package br.compneusgppremium.api.leitura.service;
import br.compneusgppremium.api.controller.dto.ResultadoLeituraDTO;
import br.compneusgppremium.api.exception.CadastroPneuException;
import br.compneusgppremium.api.leitura.FalhaLeituraComEvidencia;
import br.compneusgppremium.api.leitura.dto.NovaTentativaDTO;
import br.compneusgppremium.api.leitura.dto.TentativaLeituraDTO;
import br.compneusgppremium.api.service.ImagemLeituraService;
import br.compneusgppremium.api.service.LeituraCarcacaService;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
@Service
@RequiredArgsConstructor
@Slf4j
public class TentativaLeituraService {
    private final ImagemLeituraService validacaoImagem;
    private final ArquivosLeituraService arquivos;
    private final TentativaPersistenciaService persistencia;
    private final LeituraCarcacaService leitura;
    public TentativaLeituraDTO tentar(final String sessaoId, final String chave, final NovaTentativaDTO entrada) {
        byte[] bytes = validacaoImagem.validar(entrada.foto_base64());
        String hashImagem = ArquivosLeituraService.hash(bytes);
        String contexto = entrada.campo() + "|" + entrada.marcaIdContexto() + "|" + entrada.modeloIdContexto()
                + "|" + entrada.versaoSessao() + "|" + hashImagem;
        String hash = ArquivosLeituraService.hash(contexto.getBytes(StandardCharsets.UTF_8));
        var existente = persistencia.existente(sessaoId, chave, hash);
        if (existente != null) {
            return existente;
        }
        String arquivo = arquivos.salvar(bytes);
        String tentativaId;
        try {
            tentativaId = persistencia.preparar(sessaoId, chave, hash, entrada, arquivo, hashImagem, bytes.length, validacaoImagem.descrever(bytes));
        } catch (RuntimeException erro) {
            try { arquivos.removerOrfao(arquivo); }
            catch (RuntimeException reconciliacao) { erro.addSuppressed(reconciliacao); }
            throw erro;
        }
        ResultadoLeituraDTO resultado;
        br.compneusgppremium.api.leitura.dto.ExecucaoLeituraDTO execucao = null;
        try {
            execucao = leitura.analisarComEvidencia(entrada.campo(), Base64.getEncoder().encodeToString(bytes),
                    entrada.marcaIdContexto(), entrada.modeloIdContexto());
            resultado = execucao.resultado();
        } catch (FalhaLeituraComEvidencia falha) {
            // A extração e o catálogo já produzidos seguem para a evidência, mesmo com a tentativa em erro.
            log.warn("Falha na tentativa id={} codigo={} com evidência preservada", tentativaId, falha.getCodigo());
            execucao = falha.getEvidencia();
            resultado = execucao.resultado();
        } catch (RuntimeException erro) {
            String codigo = "OCR_INDISPONIVEL";
            if (erro instanceof CadastroPneuException negocio) {
                codigo = negocio.getCodigo();
            }
            log.warn("Falha na tentativa id={} tipo={}", tentativaId, erro.getClass().getSimpleName());
            boolean contextoInvalido = "CONTEXTO_INVALIDO".equals(codigo);
            String estado = contextoInvalido ? "CONTEXTO_INVALIDO" : "ERRO_TECNICO";
            String mensagem = contextoInvalido ? "Confira a marca e o modelo antes de continuar."
                    : "Não foi possível ler; informe manualmente.";
            resultado = new ResultadoLeituraDTO(entrada.campo().name(), estado, List.of(codigo), "",
                    null, null, List.of(), null, null, mensagem, null);
        }
        return persistencia.concluir(tentativaId, resultado, execucao);
    }
}
