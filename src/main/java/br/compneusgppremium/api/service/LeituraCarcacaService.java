package br.compneusgppremium.api.service;
import br.compneusgppremium.api.config.OcrLocalProperties;
import br.compneusgppremium.api.controller.dto.LeituraCampoDTO;
import br.compneusgppremium.api.controller.dto.ResultadoLeituraDTO;
import br.compneusgppremium.api.exception.CadastroPneuException;
import br.compneusgppremium.api.leitura.FalhaLeituraComEvidencia;
import br.compneusgppremium.api.leitura.dto.ExecucaoLeituraDTO;
import br.compneusgppremium.api.leitura.service.IdentificacaoExecucaoService;
import br.compneusgppremium.api.ocr.OcrLocalClient;
import br.compneusgppremium.api.ocr.OcrResposta;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
@Service
@RequiredArgsConstructor
@Slf4j
public class LeituraCarcacaService {
    public enum Campo { DOT, MARCA, MODELO, MEDIDA, PAIS }
    private static final String PREPROCESSAMENTO = "exif-transpose-rgb-v1";
    private static final String NORMALIZACAO = "catalogo-exato-v1";
    private static final String POLITICA = "decisao-conservadora-v1";
    private final OcrLocalClient ocr;
    private final OcrLocalProperties configuracao;
    private final ImagemLeituraService imagens;
    private final ResolverCatalogoPneuService catalogo;
    private final IdentificacaoExecucaoService identificacao;
    public ResultadoLeituraDTO analisar(final Campo campo, final String foto, final Integer marcaId,
            final Integer modeloId) {
        return analisarComEvidencia(campo, foto, marcaId, modeloId).resultado();
    }
    public ExecucaoLeituraDTO analisarComEvidencia(final Campo campo, final String foto, final Integer marcaId,
            final Integer modeloId) {
        log.info("leitura campo_recebido id={} campo={} marcaContexto={} modeloContexto={}",
                MDC.get("leituraId"), campo, marcaId, modeloId);
        var snapshot = catalogo.snapshot(campo.name(), marcaId, modeloId);
        byte[] bytes = imagens.validar(foto);
        OcrResposta leitura = extrair(campo, bytes, snapshot);
        try {
            var resultado = decidir(campo, leitura, snapshot);
            log.info("leitura decisao id={} campo={} estado={} motivos={} candidatos={} campoAprovado={} escoreMinimo={} politica={}",
                    MDC.get("leituraId"), campo, resultado.estado(), resultado.motivos(), resultado.candidatos().size(),
                    configuracao.camposAprovados().contains(campo.name()), configuracao.escoreMinimo(), POLITICA);
            return montar(campo, resultado, leitura, snapshot);
        } catch (RuntimeException erro) {
            // A extração já custou foto e inferência: preservá-la separa falha de resolução de falha de leitura.
            throw falha(campo, snapshot, leitura, "FALHA_RESOLUCAO",
                    "Não foi possível interpretar a leitura; informe manualmente.", erro);
        }
    }
    private OcrResposta extrair(final Campo campo, final byte[] bytes, final Map<Integer, String> snapshot) {
        try {
            return ocr.ler(campo.name(), Base64.getEncoder().encodeToString(bytes));
        } catch (RuntimeException erro) {
            throw falha(campo, snapshot, null, "OCR_INDISPONIVEL",
                    "Não foi possível ler; informe manualmente.", erro);
        }
    }
    private ResultadoLeituraDTO decidir(final Campo campo, final OcrResposta leitura,
            final Map<Integer, String> snapshot) {
        String texto = transcrever(leitura);
        var candidatos = catalogo.resolverSnapshot(campo.name(), leitura.linhas(), snapshot);
        String estado = "AMBIGUA";
        String motivo = "MULTIPLOS_CANDIDATOS";
        String mensagem = "Não consegui distinguir o campo. Tire outra foto ou informe manualmente.";
        Integer id = null;
        String valor = null;
        Double escore = null;
        if (texto.isBlank()) {
            estado = "ILEGIVEL"; motivo = "TEXTO_AUSENTE"; mensagem = "Não consegui ler. Tire outra foto mais próxima.";
        } else if (candidatos.isEmpty()) {
            estado = "FORA_CATALOGO"; motivo = "SEM_CORRESPONDENCIA";
            mensagem = "Lido, mas não encontrado no catálogo. Solicite revisão do catálogo.";
            if (campo == Campo.DOT) {
                estado = "ILEGIVEL"; motivo = "FORMATO_INVALIDO";
                mensagem = "Não consegui ler a data DOT. Tire outra foto ou informe manualmente.";
            }
        } else if (candidatos.size() == 1) {
            var candidato = candidatos.get(0);
            escore = candidato.escore();
            motivo = configuracao.camposAprovados().contains(campo.name()) ? "BAIXA_CONFIANCA" : "MODELO_NAO_APROVADO";
            if (configuracao.camposAprovados().contains(campo.name()) && escore >= configuracao.escoreMinimo()) {
                estado = "SUGESTAO"; motivo = "CONFIRMACAO_OBRIGATORIA";
                id = candidato.id(); valor = candidato.texto(); mensagem = "Confira a foto e confirme o valor.";
            }
        }
        return new ResultadoLeituraDTO(campo.name(), estado, List.of(motivo), texto, id, valor, candidatos,
                escore, null, mensagem, leitura.versaoModelo());
    }
    private ExecucaoLeituraDTO montar(final Campo campo, final ResultadoLeituraDTO resultado,
            final OcrResposta leitura, final Map<Integer, String> snapshot) {
        String preprocessamento = leitura != null && leitura.versaoPreprocessamento() != null
                && !leitura.versaoPreprocessamento().isBlank() ? leitura.versaoPreprocessamento() : PREPROCESSAMENTO;
        return new ExecucaoLeituraDTO(resultado, leitura, snapshot, preprocessamento, NORMALIZACAO, POLITICA,
                configuracao.escoreMinimo(), configuracao.camposAprovados().contains(campo.name()),
                identificacao.atual());
    }
    private FalhaLeituraComEvidencia falha(final Campo campo, final Map<Integer, String> snapshot,
            final OcrResposta leitura, final String codigoPadrao, final String mensagem, final RuntimeException erro) {
        String codigo = codigoPadrao;
        HttpStatus status = HttpStatus.SERVICE_UNAVAILABLE;
        if (erro instanceof CadastroPneuException negocio) {
            codigo = negocio.getCodigo();
            status = negocio.getStatus();
        }
        String texto = leitura == null ? "" : transcrever(leitura);
        String versaoModelo = leitura == null ? null : leitura.versaoModelo();
        var resultado = new ResultadoLeituraDTO(campo.name(), "ERRO_TECNICO", List.of(codigo), texto,
                null, null, List.of(), null, null, mensagem, versaoModelo);
        return new FalhaLeituraComEvidencia(codigo, mensagem, status,
                montar(campo, resultado, leitura, snapshot), erro);
    }
    private String transcrever(final OcrResposta leitura) {
        return leitura.linhas().stream().map(linha -> linha.texto() == null ? "" : linha.texto())
                .collect(Collectors.joining("\n"));
    }
    public LeituraCampoDTO lerCampo(final Campo campo, final String foto, final Integer marcaId,
            final Integer modeloId) {
        ResultadoLeituraDTO resultado = analisar(campo, foto, marcaId, modeloId);
        LeituraCampoDTO legado = new LeituraCampoDTO();
        legado.setTexto(resultado.textoOriginal());
        legado.setConfianca("BAIXA");
        legado.setMensagem(mensagemLegada(resultado));
        // Só o estado SUGESTAO devolve valor: campo aprovado em configuração e escore acima do mínimo.
        // A tela preenche o campo com esse valor, mas a etapa só fecha com confirmação do operador —
        // ambiguidade e leitura fraca continuam sem preenchimento, como exige a política de decisão.
        if ("SUGESTAO".equals(resultado.estado())) {
            legado.setConfianca("ALTA");
            if (campo == Campo.DOT) {
                legado.setDot(resultado.valorSugerido());
                legado.setDotCompleto(resultado.textoOriginal());
            } else {
                legado.setId(resultado.itemSugeridoId());
            }
        }
        return legado;
    }

    private String mensagemLegada(final ResultadoLeituraDTO resultado) {
        if (resultado.motivos().contains("MODELO_NAO_APROVADO")) {
            return "A foto foi processada, mas as sugestões deste campo ainda não estão liberadas. "
                    + "Confira a foto e informe o valor manualmente.";
        }
        if ("SUGESTAO".equals(resultado.estado())) {
            return "Leitura automática. Confira a foto e confirme o valor.";
        }
        return resultado.mensagem();
    }
}
