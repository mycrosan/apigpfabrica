package br.compneusgppremium.api.revisao.controller;
import br.compneusgppremium.api.revisao.dto.*;
import br.compneusgppremium.api.revisao.service.*;
import br.compneusgppremium.api.service.LeituraCarcacaService.Campo;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.Instant;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/v2/revisoes-leitura")
@RequiredArgsConstructor
public class RevisaoController {
    private final ConsultaRevisaoService consultas;
    private final RegistrarRevisaoService registros;
    private final EnfileirarRevisaoService importacao;
    private final AcessoRevisaoService acesso;
    public record PermissaoDTO(boolean podeRevisar) { }
    public record ImportacaoDTO(List<String> itens) { public ImportacaoDTO { itens = List.copyOf(itens); } }
    @GetMapping("/permissao")
    public PermissaoDTO permissao() { return new PermissaoDTO(acesso.permitido()); }
    @GetMapping
    public FilaRevisaoDTO fila(@RequestParam(defaultValue = "0") final int pagina,
            @RequestParam(required = false) final Campo campo, @RequestParam(required = false) final String estado,
            @RequestParam(required = false) final Boolean correcao, @RequestParam(required = false) final Boolean ambiguidade,
            @RequestParam(required = false) final Integer marcaId, @RequestParam(required = false) final Integer modeloId,
            @RequestParam(required = false) final Instant desde, @RequestParam(required = false) final Instant ate,
            @RequestParam(defaultValue = "true") final boolean disponiveis) {
        return consultas.fila(pagina, campo == null ? null : campo.name(), estado, correcao, ambiguidade,
                marcaId, modeloId, desde, ate, disponiveis);
    }
    @GetMapping("/{id}")
    public ItemRevisaoDTO consultar(@PathVariable final UUID id) { return consultas.consultar(id.toString()); }
    @GetMapping("/{id}/imagem")
    public ResponseEntity<byte[]> imagem(@PathVariable final UUID id) {
        return ResponseEntity.ok().header("Content-Type", "image/png").header("Cache-Control", "private, no-store")
                .header("X-Content-Type-Options", "nosniff").body(consultas.imagem(id.toString()));
    }
    @PostMapping("/{id}/respostas")
    public ResponseEntity<ItemRevisaoDTO> registrar(@PathVariable final UUID id,
            @RequestHeader("Idempotency-Key") final UUID chave, @Valid @RequestBody final RevisarEntradaDTO entrada) {
        var resposta = registros.registrar(id.toString(), chave, entrada);
        return ResponseEntity.created(URI.create("/api/v2/revisoes-leitura/" + id)).body(resposta);
    }
    @PostMapping("/importacoes")
    public ResponseEntity<ImportacaoDTO> importar(@Valid @RequestBody final ImportarRevisaoDTO entrada) {
        return ResponseEntity.created(URI.create("/api/v2/revisoes-leitura"))
                .body(new ImportacaoDTO(importacao.importar(entrada)));
    }
}
