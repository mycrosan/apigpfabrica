package br.compneusgppremium.api.leitura.service;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
@Component
@RequiredArgsConstructor
public class LeituraJson {
    private final ObjectMapper mapper;
    public String escrever(final Object valor) {
        try { return mapper.writeValueAsString(valor); }
        catch (JsonProcessingException erro) { throw new IllegalStateException("Falha ao serializar leitura", erro); }
    }
    public <T> T ler(final String valor, final Class<T> tipo) {
        try { return mapper.readValue(valor, tipo); }
        catch (JsonProcessingException erro) { throw new IllegalStateException("Registro de leitura inválido", erro); }
    }
    public <T> T ler(final String valor, final TypeReference<T> tipo) {
        try { return mapper.readValue(valor, tipo); }
        catch (JsonProcessingException erro) { throw new IllegalStateException("Registro de leitura inválido", erro); }
    }
}
