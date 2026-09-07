package br.compneusgppremium.api.controller.dto;
import java.time.Instant;
import java.util.UUID;
public record CarcacaRespostaDTO(Integer id, String numero_etiqueta, String dot, String status,
        String dados, ModeloPneuDTO modelo, CatalogoPneuDTO medida, CatalogoPneuDTO pais, String fotos,
        String leituraMetadados, CatalogoPneuDTO status_carcaca, UsuarioCadastroDTO criadoPor,
        Instant dt_create, Instant dt_update, UUID uuid) {}
