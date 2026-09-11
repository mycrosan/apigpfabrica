package br.compneusgppremium.api.config;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LogLeituraFilterTest {
    @Test
    void propagaCorrelacaoERestauraContextoMesmoComFalha() {
        var request = new MockHttpServletRequest("POST", "/api/carcaca/leitura-campo");
        request.addHeader("X-Leitura-Id", "campo-123");
        var response = new MockHttpServletResponse();
        MDC.put("leituraId", "anterior");
        try {
            assertThatThrownBy(() -> new LogLeituraFilter().doFilter(request, response, (entrada, saida) -> {
                assertThat(MDC.get("leituraId")).isEqualTo("campo-123");
                throw new IllegalStateException();
            })).isInstanceOf(IllegalStateException.class);
            assertThat(response.getHeader("X-Leitura-Id")).isEqualTo("campo-123");
            assertThat(MDC.get("leituraId")).isEqualTo("anterior");
        } finally {
            MDC.remove("leituraId");
        }
    }

    @Test
    void substituiCabecalhoInseguroSemVazarContexto() throws Exception {
        var request = new MockHttpServletRequest("POST", "/api/v2/leitura-sessoes/sessao/tentativas");
        request.addHeader("X-Leitura-Id", "invalido\nconteudo");
        var response = new MockHttpServletResponse();
        new LogLeituraFilter().doFilter(request, response, (entrada, saida) -> {
            assertThat(MDC.get("leituraId")).matches("[a-f0-9-]{36}");
        });
        assertThat(MDC.get("leituraId")).isNull();
        assertThat(response.getHeader("X-Leitura-Id")).doesNotContain("conteudo");
    }
}
