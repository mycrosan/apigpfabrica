package br.compneusgppremium.api.controller;
import br.compneusgppremium.api.service.ResumoService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/resumo")
public class ResumoController {

    private final ResumoService resumoService;

    public ResumoController(ResumoService resumoService) {
        this.resumoService = resumoService;
    }

    @GetMapping("/{entidade}")
    public Map<String, Long> getResumo(@PathVariable String entidade) {
        return resumoService.getResumo(entidade);
    }
}
