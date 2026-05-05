package br.compneusgppremium.api.controller;

import br.compneusgppremium.api.controller.model.PerfilModel;
import br.compneusgppremium.api.repository.PerfilRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/perfil")
public class PerfilController {

    @Autowired
    private PerfilRepository repository;

    @GetMapping
    public List<PerfilModel> findAll() {
        return repository.findAll();
    }


    @GetMapping("/{id}")
    public ResponseEntity<PerfilModel> consultar(@PathVariable Integer id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> salvar(@RequestBody PerfilModel perfil) {
        try {
            PerfilModel savedPerfil = repository.save(perfil);
            return ResponseEntity.ok(savedPerfil);
        } catch (Exception ex) {
            return ResponseEntity.status(500).body("Erro ao salvar perfil: " + ex.getMessage());
        }
    }
}
