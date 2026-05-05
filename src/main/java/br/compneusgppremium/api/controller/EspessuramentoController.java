package br.compneusgppremium.api.controller;

import br.compneusgppremium.api.controller.model.EspessuramentoModel;
import br.compneusgppremium.api.repository.EspessuramentoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
public class EspessuramentoController {

    @Autowired
    private EspessuramentoRepository repository;

    @GetMapping(path = "/api/espessuramento")
    public List<EspessuramentoModel> findAll() {
        var it = repository.findAll();
        var values = new ArrayList<EspessuramentoModel>();
        it.forEach(e -> values.add(e));
        return values;
    }

    @GetMapping(path = "/api/espessuramento/{id}")
    public ResponseEntity consultar(@PathVariable("id") Integer id) {
        return repository.findById(id)
                .map(record -> ResponseEntity.ok().body(record))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping(path = "/api/espessuramento")
    public Object salvar(@RequestBody EspessuramentoModel obj) {
        try {
            return repository.save(obj);
        } catch (Exception ex) {
            return ex;
        }
    }
    @PutMapping(path = "/api/espessuramento/{id}")
    public ResponseEntity atualizar(@PathVariable("id") Integer id, @RequestBody EspessuramentoModel espessuramento) {
        return repository.findById(id)
                .map(record -> {
                    record.setDescricao(espessuramento.getDescricao());
                    EspessuramentoModel updated = repository.save(record);
                    return ResponseEntity.ok().body(updated);
                }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping(path = "/api/espessuramento/{id}")
    public ResponseEntity<?> delete(@PathVariable("id") Integer id) {
        return repository.findById(id)
                .map(record -> {
                    repository.deleteById(id);
                    return ResponseEntity.ok().build();
                }).orElse(ResponseEntity.notFound().build());
    }
}
