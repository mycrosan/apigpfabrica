package br.compneusgppremium.api.controller;

import br.compneusgppremium.api.controller.model.MarcaModel;
import br.compneusgppremium.api.controller.model.PaisModel;
import br.compneusgppremium.api.repository.MarcaRepository;
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
@Tag(name = "Marca", description = "Operações relacionadas ao gerenciamento de marcas")
@SecurityRequirement(name = "Bearer Authentication")
public class MarcaController {

    @Autowired
    private MarcaRepository repository;

    @Operation(summary = "Listar todas as marcas", description = "Retorna uma lista com todas as marcas cadastradas")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de marcas retornada com sucesso"),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    @GetMapping(path = "/api/marca")
    public List<MarcaModel> findAll() {
        var it = repository.findAll();
        var marcas = new ArrayList<MarcaModel>();
        it.forEach(e -> marcas.add(e));
        return marcas;
    }

    @Operation(summary = "Consultar marca por ID", description = "Retorna uma marca específica pelo seu ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Marca encontrada"),
            @ApiResponse(responseCode = "404", description = "Marca não encontrada")
    })
    @GetMapping(path = "/api/marca/{id}")
    public ResponseEntity consultar(@Parameter(description = "ID da marca") @PathVariable("id") Integer id) {
        return repository.findById(id)
                .map(record -> ResponseEntity.ok().body(record))
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Criar nova marca", description = "Cria uma nova marca no sistema")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Marca criada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos fornecidos")
    })
    @PostMapping(path = "/api/marca")
    public Object salvar(@RequestBody MarcaModel marca) {
        try {
            return repository.save(marca);
        } catch (Exception ex) {
            return ex;
        }
    }
    
    @Operation(summary = "Atualizar marca", description = "Atualiza uma marca existente")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Marca atualizada com sucesso"),
            @ApiResponse(responseCode = "404", description = "Marca não encontrada"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos fornecidos")
    })
    @PutMapping(path = "/api/marca/{id}")
    public ResponseEntity atualizar(@Parameter(description = "ID da marca") @PathVariable("id") Integer id, @RequestBody MarcaModel marca) {
        return repository.findById(id)
                .map(record -> {
                    record.setDescricao(marca.getDescricao());
                    MarcaModel updated = repository.save(record);
                    return ResponseEntity.ok().body(updated);
                }).orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Deletar marca", description = "Remove uma marca do sistema")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Marca deletada com sucesso"),
            @ApiResponse(responseCode = "404", description = "Marca não encontrada")
    })
    @DeleteMapping(path = "/api/marca/{id}")
    public ResponseEntity<?> delete(@Parameter(description = "ID da marca") @PathVariable("id") Integer id) {
        return repository.findById(id)
                .map(record -> {
                    repository.deleteById(id);
                    return ResponseEntity.ok().build();
                }).orElse(ResponseEntity.notFound().build());
    }
}
