package br.compneusgppremium.api.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Correlaciona uma requisição por campo, sem ler ou registrar a foto. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class LogLeituraFilter extends OncePerRequestFilter {
    @Override
    protected boolean shouldNotFilter(final HttpServletRequest request) {
        String rota = request.getRequestURI();
        return !"POST".equals(request.getMethod()) || !(rota.endsWith("/carcaca/leitura-campo")
                || rota.matches(".*/v2/leitura-sessoes/[^/]+/tentativas"));
    }

    @Override
    protected void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response,
            final FilterChain chain) throws ServletException, IOException {
        String recebido = request.getHeader("X-Leitura-Id");
        String id = recebido != null && recebido.matches("[a-zA-Z0-9-]{1,64}")
                ? recebido : UUID.randomUUID().toString();
        long inicio = System.nanoTime();
        String anterior = MDC.get("leituraId");
        MDC.put("leituraId", id);
        response.setHeader("X-Leitura-Id", id);
        log.info("leitura recebida id={} fluxo={}", id,
                request.getRequestURI().endsWith("/tentativas") ? "v2" : "legado");
        try {
            chain.doFilter(request, response);
            log.info("leitura resposta_app id={} http={} duracaoMs={}", id, response.getStatus(),
                    (System.nanoTime() - inicio) / 1_000_000);
        } catch (IOException | ServletException | RuntimeException erro) {
            log.warn("leitura falha_api id={} tipo={}", id, erro.getClass().getSimpleName());
            throw erro;
        } finally {
            if (anterior == null) MDC.remove("leituraId");
            else MDC.put("leituraId", anterior);
        }
    }
}
