package br.compneusgppremium.api.leitura;

import br.compneusgppremium.api.exception.CadastroPneuException;
import br.compneusgppremium.api.leitura.dto.ExecucaoLeituraDTO;
import org.springframework.http.HttpStatus;

/**
 * Falha de leitura que carrega a evidência já produzida até o ponto do erro.
 *
 * <p>Uma extração descartada por falha posterior é a evidência mais cara do fluxo: a foto foi tirada,
 * o OCR rodou e o resultado se perdeu, deixando a auditoria sem como distinguir "o serviço caiu" de
 * "o pneu não foi reconhecido". Propagar a evidência junto da exceção mantém o mesmo código e status
 * HTTP de antes para o cliente, enquanto permite ao chamador persistir o que já existe.</p>
 */
public class FalhaLeituraComEvidencia extends CadastroPneuException {
    private static final long serialVersionUID = 1L;
    private final transient ExecucaoLeituraDTO evidencia;

    public FalhaLeituraComEvidencia(final String codigo, final String mensagem, final HttpStatus status,
            final ExecucaoLeituraDTO evidencia, final Throwable causa) {
        super(codigo, mensagem, status);
        initCause(causa);
        this.evidencia = evidencia;
    }

    /**
     * Devolve a evidência preservada da execução interrompida.
     *
     * @return execução com extração e catálogo conhecidos; a extração é nula quando o OCR não respondeu.
     */
    public ExecucaoLeituraDTO getEvidencia() {
        return evidencia;
    }
}
