package br.compneusgppremium.api.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.web.servlet.MultipartProperties;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.util.unit.DataSize;
import org.yaml.snakeyaml.Yaml;

/**
 * O cadastro guiado por fotos tira uma foto por campo (até 5) e envia todas juntas num único
 * multipart em {@code POST /api/upload}, preservando também tentativas que falharam na leitura
 * como evidência. Fotos de câmera real ficam entre 3 e 6 MB cada — o limite antigo (12 MB por
 * requisição) rejeitava uma sessão comum com {@code MaxUploadSizeExceededException}, como
 * aconteceu em produção. Este teste lê o application.yml de verdade, então volta a falhar se o
 * limite for reduzido por engano numa mudança futura.
 */
class UploadMultipartLimitesTest {

    // Maior foto real observada em diagnostico-ocr/ é 5,9 MB; a margem cobre variação de aparelho.
    private static final long MAIOR_FOTO_REAL_OBSERVADA_MB = 6;
    private static final int FOTOS_MINIMAS_POR_CADASTRO = 5;

    private MultipartProperties vincular() throws IOException {
        var yaml = new Yaml();
        try (var entrada = Files.newInputStream(Path.of("src/main/resources/application.yml"))) {
            @SuppressWarnings("unchecked")
            var raiz = (java.util.Map<String, Object>) yaml.load(entrada);
            @SuppressWarnings("unchecked")
            var spring = (java.util.Map<String, Object>) raiz.get("spring");
            @SuppressWarnings("unchecked")
            var servlet = (java.util.Map<String, Object>) spring.get("servlet");
            @SuppressWarnings("unchecked")
            var multipart = (java.util.Map<String, Object>) servlet.get("multipart");
            ConfigurationPropertySource fonte = new MapConfigurationPropertySource(java.util.Map.of(
                    "max-file-size", resolverPlaceholder(multipart.get("max-file-size").toString()),
                    "max-request-size", resolverPlaceholder(multipart.get("max-request-size").toString())));
            return new Binder(fonte).bind("", MultipartProperties.class).get();
        }
    }

    // O YAML usa ${VAR:default}; sem o Environment resolvendo placeholders, extrai o default
    // literal — é exatamente o valor que vale enquanto ninguém sobrescrever a variável.
    private String resolverPlaceholder(String valor) {
        if (valor.startsWith("${") && valor.contains(":") && valor.endsWith("}")) {
            return valor.substring(valor.indexOf(':') + 1, valor.length() - 1);
        }
        return valor;
    }

    @Test
    void limiteDeArquivoComportaUmaFotoDeCameraReal() throws IOException {
        DataSize limite = vincular().getMaxFileSize();
        assertThat(limite.toMegabytes()).isGreaterThanOrEqualTo(MAIOR_FOTO_REAL_OBSERVADA_MB);
    }

    @Test
    void limiteDeRequisicaoComportaUmCadastroGuiadoCompleto() throws IOException {
        DataSize limite = vincular().getMaxRequestSize();
        long minimoNecessario = MAIOR_FOTO_REAL_OBSERVADA_MB * FOTOS_MINIMAS_POR_CADASTRO;
        assertThat(limite.toMegabytes()).isGreaterThanOrEqualTo(minimoNecessario);
    }
}
