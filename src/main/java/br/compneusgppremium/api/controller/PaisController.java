package br.compneusgppremium.api.controller;

import br.compneusgppremium.api.controller.model.PaisModel;
import br.compneusgppremium.api.repository.PaisRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Tag(name = "País", description = "Operações relacionadas ao gerenciamento de países")
@SecurityRequirement(name = "Bearer Authentication")
public class PaisController {

    @Autowired
    private PaisRepository repository;

    @GetMapping(path = "/api/pais")
    @Operation(
        summary = "Listar países",
        description = "Retorna uma lista com todos os países cadastrados"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Lista de países retornada com sucesso",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = PaisModel.class)
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Token de autenticação inválido ou expirado"
        )
    })
    public List<PaisModel> findAll() {
        var it = repository.findAll();
        var pais = new ArrayList<PaisModel>();
        it.forEach(e -> pais.add(e));
        return pais;
    }

    @GetMapping(path = "/api/pais/{id}")
    @Operation(
        summary = "Consultar país por ID",
        description = "Retorna os dados de um país específico pelo seu ID"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "País encontrado",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = PaisModel.class)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "País não encontrado"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Token de autenticação inválido ou expirado"
        )
    })
    public ResponseEntity consultar(
        @Parameter(description = "ID do país", required = true, example = "1")
        @PathVariable("id") Integer id
    ) {
        return repository.findById(id)
                .map(record -> ResponseEntity.ok().body(record))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping(path = "/api/pais")
    @Operation(
        summary = "Criar novo país",
        description = "Cria um novo país no sistema"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "País criado com sucesso",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = PaisModel.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Dados inválidos"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Token de autenticação inválido ou expirado"
        )
    })
    public Object salvar(
        @Parameter(description = "Dados do país a ser criado", required = true)
        @RequestBody PaisModel pais
    ) {
        try {
            return repository.save(pais);
        } catch (Exception ex) {
            return ex;
        }
    }

    @PutMapping(path = "/api/pais/{id}")
    @Operation(
        summary = "Atualizar país",
        description = "Atualiza os dados de um país existente"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "País atualizado com sucesso",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = PaisModel.class)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "País não encontrado"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Token de autenticação inválido ou expirado"
        )
    })
    public ResponseEntity atualizar(
        @Parameter(description = "ID do país", required = true, example = "1")
        @PathVariable("id") Integer id,
        @Parameter(description = "Dados atualizados do país", required = true)
        @RequestBody PaisModel pais
    ) {
        return repository.findById(id)
                .map(record -> {
                    record.setDescricao(pais.getDescricao());
                    PaisModel updated = repository.save(record);
                    return ResponseEntity.ok().body(updated);
                }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping(path = "/api/pais/{id}")
    @Operation(
        summary = "Excluir país",
        description = "Remove um país do sistema"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "País excluído com sucesso"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "País não encontrado"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Token de autenticação inválido ou expirado"
        )
    })
    public ResponseEntity<?> delete(
        @Parameter(description = "ID do país", required = true, example = "1")
        @PathVariable("id") Integer id
    ) {
        return repository.findById(id)
                .map(record -> {
                    repository.deleteById(id);
                    return ResponseEntity.ok().build();
                }).orElse(ResponseEntity.notFound().build());
    }

}
