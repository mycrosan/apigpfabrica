package br.compneusgppremium.api.controller;

import br.compneusgppremium.api.controller.dto.CarcacaEntradaDTO;
import br.compneusgppremium.api.controller.dto.CarcacaRespostaDTO;
import br.compneusgppremium.api.controller.dto.CatalogoPneuDTO;
import br.compneusgppremium.api.controller.dto.ClassificacaoCombinacaoDTO;
import br.compneusgppremium.api.controller.dto.LeituraCampoDTO;
import br.compneusgppremium.api.controller.form.LeituraCampoForm;
import br.compneusgppremium.api.service.CarcacaCadastroService;
import br.compneusgppremium.api.service.CombinacaoPneuService;
import br.compneusgppremium.api.service.LeituraCarcacaService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class CarcacaController {
    private final CarcacaCadastroService cadastro;
    private final LeituraCarcacaService leitura;
    private final CombinacaoPneuService combinacoes;

    @GetMapping("/api/carcaca")
    public List<CarcacaRespostaDTO> listar() {
        return cadastro.listar();
    }
    @GetMapping("/api/carcaca/{id}")
    public CarcacaRespostaDTO consultar(@PathVariable final Integer id) {
        return cadastro.consultar(id);
    }
    // O cliente legado exige 200. A criação v2 utiliza 201 e contrato próprio.
    @PostMapping("/api/carcaca")
    public CarcacaRespostaDTO criar(@Valid @RequestBody final CarcacaEntradaDTO entrada,
            @RequestParam(defaultValue = "false") final boolean confirmarCombinacaoNova) {
        return cadastro.criar(entrada, confirmarCombinacaoNova);
    }
    @PutMapping("/api/carcaca/{id}")
    public CarcacaRespostaDTO atualizar(@PathVariable final Integer id,
            @Valid @RequestBody final CarcacaEntradaDTO entrada,
            @RequestParam(defaultValue = "false") final boolean confirmarCombinacaoNova) {
        return cadastro.atualizar(id, entrada, confirmarCombinacaoNova);
    }
    @DeleteMapping("/api/carcaca/{id}")
    public ResponseEntity<Void> excluir(@PathVariable final Integer id) {
        cadastro.excluir(id);
        return ResponseEntity.ok().build();
    }
    @GetMapping("/api/carcaca/pesquisa/{etiqueta}")
    public CarcacaRespostaDTO pesquisar(@PathVariable final String etiqueta) {
        return cadastro.pesquisar(etiqueta);
    }
    @PostMapping("/api/carcaca/leitura-campo")
    public LeituraCampoDTO ler(@Valid @RequestBody final LeituraCampoForm entrada) {
        return leitura.lerCampo(LeituraCarcacaService.Campo.valueOf(entrada.getCampo()), entrada.getFoto_base64(),
                entrada.getMarcaIdContexto(), entrada.getModeloIdContexto());
    }
    @GetMapping("/api/carcaca/classificar-combinacao")
    public ClassificacaoCombinacaoDTO classificar(@RequestParam final Integer modeloId,
            @RequestParam final Integer medidaId, @RequestParam final Integer paisId) {
        return combinacoes.classificar(modeloId, medidaId, paisId);
    }
    @GetMapping("/api/carcaca/medidas-plausiveis")
    public List<CatalogoPneuDTO> medidas(@RequestParam final Integer modeloId) {
        return combinacoes.medidasCatalogo(modeloId);
    }
}
