package br.compneusgppremium.api.controller;

import br.compneusgppremium.api.controller.model.CamelbackModel;
import br.compneusgppremium.api.controller.model.MedidaModel;
import br.compneusgppremium.api.controller.model.PaisModel;
import br.compneusgppremium.api.repository.CamelbackRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
public class CamelbackController {

    @Autowired
    private CamelbackRepository repository;

    @GetMapping(path = "/api/camelback")
    public List<CamelbackModel> findAll() {
        var it = repository.findAll();
        var values = new ArrayList<CamelbackModel>();
        it.forEach(e -> values.add(e));
        return values;
    }

    @GetMapping(path = "/api/camelback/{id}")
    public ResponseEntity consultar(@PathVariable("id") Integer id) {
        return repository.findById(id)
                .map(record -> ResponseEntity.ok().body(record))
                .orElse(ResponseEntity.notFound().build());
    }
    @PostMapping(path = "/api/camelback")
    public Object salvar(@RequestBody CamelbackModel obj) {
        try {
            return repository.save(obj);
        } catch (Exception ex) {
            return ex;
        }
    }
    @PutMapping(path = "/api/camelback/{id}")
    public ResponseEntity atualizar(@PathVariable("id") Integer id, @RequestBody CamelbackModel camelback ) {
        return repository.findById(id)
                .map(record -> {
                    record.setDescricao(camelback.getDescricao());
                    CamelbackModel updated = repository.save(record);
                    return ResponseEntity.ok().body(updated);
                }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping(path = "/api/camelback/{id}")
    public ResponseEntity<?> delete(@PathVariable("id") Integer id) {
        return repository.findById(id)
                .map(record -> {
                    repository.deleteById(id);
                    return ResponseEntity.ok().build();
                }).orElse(ResponseEntity.notFound().build());
    }
}
