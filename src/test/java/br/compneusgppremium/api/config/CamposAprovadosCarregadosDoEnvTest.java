package br.compneusgppremium.api.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * Duas vezes este projeto teve um campo funcionando na resolução do catálogo e mesmo assim
 * invisível na tela, porque {@code PNEUS_OCR_CAMPOS_APROVADOS} nunca chegava ao processo — a
 * variável ficava só documentada, sem entrar em nenhum arquivo que o Spring realmente lê. Este
 * teste carrega a configuração REAL do projeto ({@code application.yml} + {@code ocr-local/.env},
 * com {@link ConfigDataApplicationContextInitializer} — o mesmo mecanismo que o processo em
 * produção usa, resolvendo os arquivos a partir do diretório de trabalho do módulo — e falha se
 * {@code camposAprovados()} não refletir o que está gravado no arquivo.
 */
@EnableConfigurationProperties(OcrLocalProperties.class)
class CamposAprovadosCarregadosDoEnvTest {

    private final ApplicationContextRunner contexto = new ApplicationContextRunner()
            .withInitializer(new ConfigDataApplicationContextInitializer())
            .withUserConfiguration(CamposAprovadosCarregadosDoEnvTest.class,
                    ConfigurationPropertiesAutoConfiguration.class);

    @Test
    void paisEDotEstaoAprovadosPorqueOArquivoDeConfiguracaoRealDefineIsso() {
        contexto.run(ctx -> {
            assertThat(ctx).hasNotFailed();
            var propriedades = ctx.getBean(OcrLocalProperties.class);
            assertThat(propriedades.camposAprovados()).contains("DOT", "PAIS");
        });
    }
}
