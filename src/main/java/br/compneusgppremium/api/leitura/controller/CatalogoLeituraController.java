package br.compneusgppremium.api.leitura.controller;
import br.compneusgppremium.api.leitura.dto.ItemCatalogoLeituraDTO;
import br.compneusgppremium.api.leitura.service.CatalogoLeituraService;
import br.compneusgppremium.api.service.LeituraCarcacaService.Campo;
import jakarta.validation.constraints.Positive;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
@RestController
@RequiredArgsConstructor
@Validated
public class CatalogoLeituraController {
    private final CatalogoLeituraService catalogo;
    @GetMapping("/api/v2/leitura-catalogo/{campo}")
    public List<ItemCatalogoLeituraDTO> listar(@PathVariable final Campo campo,
            @RequestParam(required = false) @Positive final Integer marcaId) {
        return catalogo.listar(campo, marcaId);
    }
}
