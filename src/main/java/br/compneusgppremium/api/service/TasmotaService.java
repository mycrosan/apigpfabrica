package br.compneusgppremium.api.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
public class TasmotaService {

    private final RestTemplate restTemplate;

    public TasmotaService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Configura o dispositivo Tasmota via HTTP, aplicando nome amigável, hostname e tópico.
     * Usa comando Backlog para aplicar múltiplos comandos em uma única chamada.
     * Executa de forma assíncrona para não bloquear o request.
     */
    @Async("tasmotaExecutor")
    public void configurarAsync(String ip, String nome, String maquina) {
        try {
            String backlog = String.format("Backlog FriendlyName %s; Hostname %s; Topic %s", nome, maquina, maquina);
            String encodedCmd = URLEncoder.encode(backlog, StandardCharsets.UTF_8);
            String url = String.format("http://%s/cm?cmnd=%s", ip, encodedCmd);
            restTemplate.getForObject(url, String.class);
        } catch (Exception e) {
            // Evita travar o fluxo principal; falhas de configuração serão tratadas fora da requisição
        }
    }
}