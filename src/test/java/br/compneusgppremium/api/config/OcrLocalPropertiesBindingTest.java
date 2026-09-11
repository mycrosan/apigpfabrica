package br.compneusgppremium.api.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

// O application.yml entrega campos-aprovados como texto separado por vírgula. Se esse
// texto não virar Set<String>, nenhum campo fica aprovado e a tela nunca recebe sugestão —
// foi exatamente esse o efeito da lista vazia anterior.
class OcrLocalPropertiesBindingTest {

    private OcrLocalProperties vincular(final String camposAprovados) {
        var ambiente = new StandardEnvironment();
        ambiente.getPropertySources().addFirst(new MapPropertySource("teste", Map.of(
                "pneus.ocr.url", "http://localhost:8091",
                "pneus.ocr.token", "token-interno-teste",
                "pneus.ocr.timeout-segundos", "15",
                "pneus.ocr.habilitado", "true",
                "pneus.ocr.campos-aprovados", camposAprovados,
                "pneus.ocr.escore-minimo", "0.90",
                "pneus.ocr.limite-bytes", "8388608",
                "pneus.ocr.limite-pixels", "20000000",
                "pneus.ocr.falhas-para-abrir", "3",
                "pneus.ocr.abertura-segundos", "30")));
        return Binder.get(ambiente).bind("pneus.ocr", OcrLocalProperties.class).get();
    }

    @Test
    void valorPadraoDoYamlAprovaSomenteODot() {
        assertThat(vincular("DOT").camposAprovados()).containsExactly("DOT");
    }

    @Test
    void listaSeparadaPorVirgulaAprovaCadaCampo() {
        assertThat(vincular("DOT,MEDIDA").camposAprovados()).containsExactlyInAnyOrder("DOT", "MEDIDA");
    }

    @Test
    void textoVazioMantemTodosOsCamposManuais() {
        assertThat(vincular("").camposAprovados()).isEmpty();
    }
}
