package br.compneusgppremium.api.controller;

import br.compneusgppremium.api.controller.model.ModeloModel;
import br.compneusgppremium.api.controller.model.PaisModel;
import br.compneusgppremium.api.repository.ModeloRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@Tag(name = "Modelo", description = "Operações relacionadas ao gerenciamento de modelos")
@SecurityRequirement(name = "Bearer Authentication")
public class ModeloController {

    @Autowired
    private ModeloRepository repository;

    @Operation(summary = "Listar todos os modelos", description = "Retorna uma lista com todos os modelos cadastrados")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de modelos retornada com sucesso"),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    @GetMapping(path = "/api/modelo")
    public List<ModeloModel> findAll() {
        var it = repository.findAll();
        var modelos = new ArrayList<ModeloModel>();
        it.forEach(e -> modelos.add(e));
        return modelos;
    }

    @Operation(summary = "Consultar modelo por ID", description = "Retorna um modelo específico pelo seu ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Modelo encontrado"),
            @ApiResponse(responseCode = "404", description = "Modelo não encontrado")
    })
    @GetMapping(path = "/api/modelo/{id}")
    public ResponseEntity consultar(@Parameter(description = "ID do modelo") @PathVariable("id") Integer id) {
        return repository.findById(id)
                .map(record -> ResponseEntity.ok().body(record))
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Criar novo modelo", description = "Cria um novo modelo no sistema")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Modelo criado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos fornecidos")
    })
    @PostMapping(path = "/api/modelo")
    public Object salvar(@RequestBody ModeloModel modelo) {
        try {
            return repository.save(modelo);
        } catch (Exception ex) {
            return ex;
        }
    }
    
    @Operation(summary = "Atualizar modelo", description = "Atualiza um modelo existente")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Modelo atualizado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Modelo não encontrado"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos fornecidos")
    })
    @PutMapping(path = "/api/modelo/{id}")
    public ResponseEntity atualizar(@Parameter(description = "ID do modelo") @PathVariable("id") Integer id, @RequestBody ModeloModel modelo) {
        return repository.findById(id)
                .map(record -> {
                    record.setDescricao(modelo.getDescricao());
                    ModeloModel updated = repository.save(record);
                    return ResponseEntity.ok().body(updated);
                }).orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Deletar modelo", description = "Remove um modelo do sistema")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Modelo deletado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Modelo não encontrado")
    })
    @DeleteMapping(path = "/api/modelo/{id}")
    public ResponseEntity<?> delete(@Parameter(description = "ID do modelo") @PathVariable("id") Integer id) {
        return repository.findById(id)
                .map(record -> {
                    repository.deleteById(id);
                    return ResponseEntity.ok().build();
                }).orElse(ResponseEntity.notFound().build());
    }
}
