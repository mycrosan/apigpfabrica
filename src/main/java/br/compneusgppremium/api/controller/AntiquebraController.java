package br.compneusgppremium.api.controller;

import br.compneusgppremium.api.controller.model.AntiquebraModel;
import br.compneusgppremium.api.controller.model.PaisModel;
import br.compneusgppremium.api.repository.AntiquebraRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
public class AntiquebraController {

    @Autowired
    private AntiquebraRepository repository;

    @GetMapping(path = "/api/antiquebra")
    public List<AntiquebraModel> findAll() {
        var it = repository.findAll();
        var values = new ArrayList<AntiquebraModel>();
        it.forEach(e -> values.add(e));
        return values;
    }

    @GetMapping(path = "/api/antiquebra/{id}")
    public ResponseEntity consultar(@PathVariable("id") Integer id) {
        return repository.findById(id)
                .map(record -> ResponseEntity.ok().body(record))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping(path = "/api/antiquebra")
    public Object salvar(@RequestBody AntiquebraModel obj) {
        try {
            return repository.save(obj);
        } catch (Exception ex) {
            return ex;
        }
    }
    @PutMapping(path = "/api/antiquebra/{id}")
    public ResponseEntity atualizar(@PathVariable("id") Integer id, @RequestBody AntiquebraModel antiquebra) {
        return repository.findById(id)
                .map(record -> {
                    record.setDescricao(antiquebra.getDescricao());
                    AntiquebraModel updated = repository.save(record);
                    return ResponseEntity.ok().body(updated);
                }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping(path = "/api/antiquebra/{id}")
    public ResponseEntity<?> delete(@PathVariable("id") Integer id) {
        return repository.findById(id)
                .map(record -> {
                    repository.deleteById(id);
                    return ResponseEntity.ok().build();
                }).orElse(ResponseEntity.notFound().build());
    }
}
