package br.compneusgppremium.api.leitura.controller;
import br.compneusgppremium.api.leitura.dto.*;
import br.compneusgppremium.api.leitura.service.ConfirmacaoLeituraService;
import br.compneusgppremium.api.leitura.service.SessaoLeituraService;
import br.compneusgppremium.api.leitura.service.TentativaLeituraService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
@RestController
@RequiredArgsConstructor
@Validated
public class LeituraSessaoController {
    private final SessaoLeituraService sessoes;
    private final TentativaLeituraService tentativas;
    private final ConfirmacaoLeituraService confirmacoes;
    @PostMapping("/api/v2/leitura-sessoes")
    public ResponseEntity<SessaoLeituraDTO> abrir(@RequestHeader("Idempotency-Key") final UUID chave,
            @Valid @RequestBody final AbrirSessaoDTO entrada) {
        var sessao = sessoes.abrir(chave, entrada);
        return ResponseEntity.created(URI.create("/api/v2/leitura-sessoes/" + sessao.id())).body(sessao);
    }
    @GetMapping("/api/v2/leitura-sessoes/{id}")
    public SessaoLeituraDTO consultar(@PathVariable final UUID id) {
        return sessoes.consultar(id.toString());
    }
    @PostMapping("/api/v2/leitura-sessoes/{id}/tentativas")
    public ResponseEntity<TentativaLeituraDTO> tentar(@PathVariable final UUID id,
            @NotBlank @Size(max = 128) @RequestHeader("Idempotency-Key") final String chave,
            @Valid @RequestBody final NovaTentativaDTO entrada) {
        var resultado = tentativas.tentar(id.toString(), chave, entrada);
        if ("ERRO_TECNICO".equals(resultado.estado())) {
            return ResponseEntity.status(503).body(resultado);
        }
        return ResponseEntity.ok(resultado);
    }
    @PostMapping("/api/v2/leitura-sessoes/{id}/abandono")
    public SessaoLeituraDTO abandonar(@PathVariable final UUID id,
            @Valid @RequestBody final AbandonarSessaoDTO entrada) {
        return sessoes.abandonar(id.toString(), entrada);
    }
    @PostMapping("/api/v2/leitura-sessoes/{id}/confirmacoes")
    public SessaoLeituraDTO confirmar(@PathVariable final UUID id, @Valid @RequestBody final ConfirmarCampoDTO entrada) {
        return confirmacoes.confirmar(id.toString(), entrada);
    }
    @GetMapping("/api/v2/leitura-imagens/{id}")
    public ResponseEntity<byte[]> imagem(@PathVariable final UUID id) {
        return ResponseEntity.ok().header("Cache-Control", "private, no-store")
                .header("Content-Type", "application/octet-stream").body(sessoes.imagem(id.toString()));
    }
}
