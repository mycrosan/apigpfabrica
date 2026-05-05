package br.compneusgppremium.api.controller;
import br.compneusgppremium.api.controller.model.MedidaModel;
import br.compneusgppremium.api.controller.model.PaisModel;
import br.compneusgppremium.api.repository.MedidaRepository;
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
@Tag(name = "Medida", description = "Operações relacionadas ao gerenciamento de medidas")
@SecurityRequirement(name = "Bearer Authentication")
public class MedidaController {

    @Autowired
    private MedidaRepository repository;

    @Operation(summary = "Listar todas as medidas", description = "Retorna uma lista com todas as medidas de pneus cadastradas")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de medidas retornada com sucesso"),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    @GetMapping(path = "/api/medida")
    public List<MedidaModel> findAll() {
        var it = repository.findAll();
        var medidas = new ArrayList<MedidaModel>();
        it.forEach(e -> medidas.add(e));
        return medidas;
    }

    @Operation(summary = "Consultar medida por ID", description = "Retorna uma medida específica pelo seu ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Medida encontrada"),
            @ApiResponse(responseCode = "404", description = "Medida não encontrada")
    })
    @GetMapping(path = "/api/medida/{id}")
    public ResponseEntity consultar(@Parameter(description = "ID da medida") @PathVariable("id") Integer id) {
        return repository.findById(id)
                .map(record -> ResponseEntity.ok().body(record))
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Criar nova medida", description = "Cria uma nova medida de pneu no sistema")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Medida criada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos fornecidos")
    })
    @PostMapping(path = "/api/medida")
    public Object salvar(@RequestBody MedidaModel medida) {
        try {
            return repository.save(medida);
        } catch (Exception ex) {
            return ex;
        }
    }

    @Operation(summary = "Atualizar medida", description = "Atualiza uma medida existente")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Medida atualizada com sucesso"),
            @ApiResponse(responseCode = "404", description = "Medida não encontrada"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos fornecidos")
    })
    @PutMapping(path = "/api/medida/{id}")
    public ResponseEntity atualizar(@Parameter(description = "ID da medida") @PathVariable("id") Integer id, @RequestBody MedidaModel medida) {
        return repository.findById(id)
                .map(record -> {
                    record.setDescricao(medida.getDescricao());
                    MedidaModel updated = repository.save(record);
                    return ResponseEntity.ok().body(updated);
                }).orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Deletar medida", description = "Remove uma medida do sistema")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Medida deletada com sucesso"),
            @ApiResponse(responseCode = "404", description = "Medida não encontrada")
    })
    @DeleteMapping(path = "/api/medida/{id}")
    public ResponseEntity<?> delete(@Parameter(description = "ID da medida") @PathVariable("id") Integer id) {
        return repository.findById(id)
                .map(record -> {
                    repository.deleteById(id);
                    return ResponseEntity.ok().build();
                }).orElse(ResponseEntity.notFound().build());
    }
}
