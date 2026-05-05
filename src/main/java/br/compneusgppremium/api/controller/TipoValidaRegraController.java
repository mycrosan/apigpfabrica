package br.compneusgppremium.api.controller;

import br.compneusgppremium.api.controller.model.PaisModel;
import br.compneusgppremium.api.controller.model.TipoValidaRegraModel;
import br.compneusgppremium.api.repository.PaisRepository;
import br.compneusgppremium.api.repository.TipoValidaRegraRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
public class TipoValidaRegraController {

    @Autowired
    private TipoValidaRegraRepository repository;

    @GetMapping(path = "/api/tipovalidaregra")
    public List<TipoValidaRegraModel> findAll() {
        var it = repository.findAll();
        var arrayList = new ArrayList<TipoValidaRegraModel>();
        it.forEach(e -> arrayList.add(e));
        return arrayList;
    }

    @GetMapping(path = "/api/tipovalidaregra/{id}")
    public ResponseEntity consultar(@PathVariable("id") Integer id) {
        return repository.findById(id)
                .map(record -> ResponseEntity.ok().body(record))
                .orElse(ResponseEntity.notFound().build());
    }

//    @PostMapping(path = "/api/pais")
//    public Object salvar(@RequestBody PaisModel pais) {
//        try {
//            return repository.save(pais);
//        } catch (Exception ex) {
//            return ex;
//        }
//    }
//
//    @PutMapping(path = "/api/pais/{id}")
//    public ResponseEntity atualizar(@PathVariable("id") Integer id, @RequestBody PaisModel pais) {
//        return repository.findById(id)
//                .map(record -> {
//                    record.setDescricao(pais.getDescricao());
//                    PaisModel updated = repository.save(record);
//                    return ResponseEntity.ok().body(updated);
//                }).orElse(ResponseEntity.notFound().build());
//    }
//
//    @DeleteMapping(path = "/api/pais/{id}")
//    public ResponseEntity<?> delete(@PathVariable("id") Integer id) {
//        return repository.findById(id)
//                .map(record -> {
//                    repository.deleteById(id);
//                    return ResponseEntity.ok().build();
//                }).orElse(ResponseEntity.notFound().build());
//    }

}
