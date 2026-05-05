package br.compneusgppremium.api.controller;

import br.compneusgppremium.api.controller.model.ValidaRegraModel;
import br.compneusgppremium.api.repository.ValidaRegraRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
public class ValidaRegraController {

    @Autowired
    private ValidaRegraRepository repository;

    @GetMapping(path = "/api/validaregra")
    public List<ValidaRegraModel> findAll() {
        var it = repository.findAll();
        var validaregra = new ArrayList<ValidaRegraModel>();
        it.forEach(e -> validaregra.add(e));
        return validaregra;
    }

    @GetMapping(path = "/api/validaregra/{id}")
    public ResponseEntity consultar(@PathVariable("id") Integer id) {
        return repository.findById(id)
                .map(record -> ResponseEntity.ok().body(record))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping(path = "/api/validaregra")
    public Object salvar(@RequestBody ValidaRegraModel validaRegra) {
        try {
            return repository.save(validaRegra);
        } catch (Exception ex) {
            return ex;
        }
    }

//    @PutMapping(path = "/api/pais/{id}")
//    public ResponseEntity atualizar(@PathVariable("id") Integer id, @RequestBody ValidaRegraModel validaRegra) {
//        return repository.findById(id)
//                .map(record -> {
//                    record.setDescricao(validaRegra.getDescricao());
//                    ValidaRegraModel updated = repository.save(record);
//                    return ResponseEntity.ok().body(updated);
//                }).orElse(ResponseEntity.notFound().build());
//    }

//    @DeleteMapping(path = "/api/pais/{id}")
//    public ResponseEntity<?> delete(@PathVariable("id") Integer id) {
//        return repository.findById(id)
//                .map(record -> {
//                    repository.deleteById(id);
//                    return ResponseEntity.ok().build();
//                }).orElse(ResponseEntity.notFound().build());
//    }

}
