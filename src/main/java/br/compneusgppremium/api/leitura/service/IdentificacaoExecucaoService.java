package br.compneusgppremium.api.leitura.service;

import br.compneusgppremium.api.config.OcrLocalProperties;
import br.compneusgppremium.api.config.PoliticaPneuProperties;
import br.compneusgppremium.api.leitura.dto.IdentificacaoExecucaoDTO;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.TreeSet;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Service;

/**
 * Resolve, uma única vez na inicialização, a identificação de build e configuração das execuções.
 *
 * <p>A configuração de decisão é imutável após a subida da aplicação, então recalcular o resumo a
 * cada leitura só produziria custo. Guardar a identificação junto da evidência é o que permite
 * distinguir, meses depois, um erro de leitura de uma mudança de política ou de versão.</p>
 */
@Service
public class IdentificacaoExecucaoService {
    private static final String VERSAO_DESCONHECIDA = "desconhecida";
    private final IdentificacaoExecucaoDTO identificacao;

    public IdentificacaoExecucaoService(final ObjectProvider<BuildProperties> build,
            final OcrLocalProperties ocr, final PoliticaPneuProperties politica) {
        this.identificacao = new IdentificacaoExecucaoDTO(resolverVersao(build), resumirConfiguracao(ocr, politica));
    }

    /**
     * Devolve a identificação vigente desta instância.
     *
     * @return build e resumo de configuração usados por todas as execuções deste processo.
     */
    public IdentificacaoExecucaoDTO atual() {
        return identificacao;
    }

    private String resolverVersao(final ObjectProvider<BuildProperties> build) {
        BuildProperties propriedades = build.getIfAvailable();
        if (propriedades != null && propriedades.getVersion() != null) {
            return propriedades.getVersion();
        }
        // Fora do artefato empacotado (testes e execução pela IDE) o manifesto não existe.
        String manifesto = getClass().getPackage().getImplementationVersion();
        if (manifesto != null && !manifesto.isBlank()) {
            return manifesto;
        }
        return VERSAO_DESCONHECIDA;
    }

    private String resumirConfiguracao(final OcrLocalProperties ocr, final PoliticaPneuProperties politica) {
        String canonico = new StringBuilder()
                .append("ocr.habilitado=").append(ocr.habilitado())
                .append("|ocr.escoreMinimo=").append(ocr.escoreMinimo())
                .append("|ocr.camposAprovados=").append(ordenar(ocr.camposAprovados()))
                .append("|politica.anoMinimo=").append(politica.anoMinimo())
                .append("|politica.validarSemanaFutura=").append(politica.validarSemanaFutura())
                .append("|politica.toleranciaSemanas=").append(politica.toleranciaSemanas())
                .append("|politica.segmentos=").append(ordenar(politica.segmentos()))
                .append("|politica.familiasMedida=").append(ordenar(politica.familiasMedida()))
                .toString();
        return ArquivosLeituraService.hash(canonico.getBytes(StandardCharsets.UTF_8));
    }

    private String ordenar(final Set<String> valores) {
        return String.join(",", new TreeSet<>(valores));
    }
}
