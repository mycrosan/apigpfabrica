package br.compneusgppremium.api.controller;

import br.compneusgppremium.api.controller.model.TipoClassificacaoModel;
import br.compneusgppremium.api.repository.TipoClassificacaoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
public class TipoClassificacaoController {

    @Autowired
    private TipoClassificacaoRepository repository;

    @GetMapping(path = "/api/tipoclassificacao")
    public List<TipoClassificacaoModel> findAll() {
        var it = repository.findAll();
        var values = new ArrayList<TipoClassificacaoModel>();
        it.forEach(e -> values.add(e));
        return values;
    }

    @GetMapping(path = "/api/tipoclassificacao/{id}")
    public ResponseEntity consultar(@PathVariable("id") Integer id) {
        return repository.findById(id)
                .map(record -> ResponseEntity.ok().body(record))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping(path = "/api/tipoclassificacao")
    public Object salvar(@RequestBody TipoClassificacaoModel value) {
        try {
            return repository.save(value);
        } catch (Exception ex) {
            return ex;
        }
    }

    @PutMapping(path = "/api/tipoclassificacao/{id}")
    public ResponseEntity atualizar(@PathVariable("id") Integer id, @RequestBody TipoClassificacaoModel value) {
        return repository.findById(id)
                .map(record -> {
                    record.setDescricao(value.getDescricao());
                    TipoClassificacaoModel updated = repository.save(record);
                    return ResponseEntity.ok().body(updated);
                }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping(path = "/api/tipoclassificacao/{id}")
    public ResponseEntity<?> delete(@PathVariable("id") Integer id) {
        return repository.findById(id)
                .map(record -> {
                    repository.deleteById(id);
                    return ResponseEntity.ok().build();
                }).orElse(ResponseEntity.notFound().build());
    }

}
