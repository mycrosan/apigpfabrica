package br.compneusgppremium.api.leitura.controller;
import br.compneusgppremium.api.controller.dto.CarcacaRespostaDTO;
import br.compneusgppremium.api.leitura.dto.CadastroTecnicoV2DTO;
import br.compneusgppremium.api.leitura.service.CadastroTecnicoV2Service;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
@RestController
@RequiredArgsConstructor
@Validated
public class CadastroTecnicoV2Controller {
    private final CadastroTecnicoV2Service cadastro;
    @PostMapping("/api/v2/carcacas")
    public ResponseEntity<CarcacaRespostaDTO> criar(@NotBlank @Size(max = 128) @RequestHeader("Idempotency-Key") final String chave,
            @Valid @RequestBody final CadastroTecnicoV2DTO entrada) {
        var resposta = cadastro.salvar(chave, entrada, null);
        return ResponseEntity.created(URI.create("/api/carcaca/" + resposta.id())).body(resposta);
    }
    @PutMapping("/api/v2/carcacas/{id}")
    public CarcacaRespostaDTO atualizar(@PathVariable final Integer id,
            @NotBlank @Size(max = 128) @RequestHeader("Idempotency-Key") final String chave,
            @Valid @RequestBody final CadastroTecnicoV2DTO entrada) {
        return cadastro.salvar(chave, entrada, id);
    }
}
