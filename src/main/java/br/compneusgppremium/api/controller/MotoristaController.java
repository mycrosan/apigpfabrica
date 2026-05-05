package br.compneusgppremium.api.controller;

import br.compneusgppremium.api.controller.dto.MotoristaCreateDTO;
import br.compneusgppremium.api.controller.dto.MotoristaResponseDTO;
import br.compneusgppremium.api.controller.dto.MotoristaUpdateDTO;
import br.compneusgppremium.api.exception.BusinessException;
import br.compneusgppremium.api.exception.NotFoundException;
import br.compneusgppremium.api.service.MotoristaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/motoristas")
@Tag(name = "Motoristas", description = "Operações de cadastro e gestão de motoristas")
@SecurityRequirement(name = "Bearer Authentication")
@Validated
@RequiredArgsConstructor
public class MotoristaController {

    private final MotoristaService motoristaService;

    @PostMapping
    @Operation(summary = "Criar motorista", description = "Cadastra um novo motorista vinculado a um usuário")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Motorista criado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = MotoristaResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos ou regra de negócio violada")
    })
    public ResponseEntity<?> criar(@Valid @RequestBody MotoristaCreateDTO dto) {
        MotoristaResponseDTO response = motoristaService.criar(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Listar motoristas", description = "Lista motoristas com filtro de ativo")
    public ResponseEntity<List<MotoristaResponseDTO>> listar(@RequestParam(required = false) Boolean ativo) {
        List<MotoristaResponseDTO> lista = motoristaService.listar(ativo != null ? ativo : true);
        return ResponseEntity.ok(lista);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar motorista", description = "Consulta motorista por ID")
    public ResponseEntity<MotoristaResponseDTO> consultar(@PathVariable Integer id) {
        MotoristaResponseDTO dto = motoristaService.consultar(id);
        return ResponseEntity.ok(dto);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar motorista", description = "Atualiza dados de motorista")
    public ResponseEntity<MotoristaResponseDTO> atualizar(@PathVariable Integer id, @Valid @RequestBody MotoristaUpdateDTO dto) {
        MotoristaResponseDTO response = motoristaService.atualizar(id, dto);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/inativar")
    @Operation(summary = "Inativar motorista", description = "Inativa motorista (soft delete)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Motorista inativado"),
            @ApiResponse(responseCode = "400", description = "Regra de negócio violada"),
            @ApiResponse(responseCode = "404", description = "Motorista não encontrado")
    })
    public ResponseEntity<?> inativar(@PathVariable Integer id) {
        motoristaService.inativar(id);
        return ResponseEntity.noContent().build();
    }
}

