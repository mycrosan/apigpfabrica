package br.compneusgppremium.api.controller;

import br.compneusgppremium.api.controller.dto.ReleCreateDTO;
import br.compneusgppremium.api.controller.dto.ReleResponseDTO;
import br.compneusgppremium.api.service.ReleService;
import br.compneusgppremium.api.util.ApiError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/rele")
@Tag(name = "Rele", description = "Operações para dispositivos Rele")
@SecurityRequirement(name = "Bearer Authentication")
public class ReleController {

    private final ReleService releService;

    public ReleController(ReleService releService) {
        this.releService = releService;
    }

    @PostMapping
    @Operation(summary = "Gravar dispositivo Rele", description = "Cria ou atualiza um dispositivo Rele por IP e associa à máquina e celular")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Dispositivo gravado com sucesso",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ReleResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Máquina não encontrada",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<?> gravar(@Valid @RequestBody ReleCreateDTO dto) {
        try {
            ReleResponseDTO response = releService.gravar(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            ApiError error = new ApiError(HttpStatus.NOT_FOUND, "Máquina não encontrada", e, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        } catch (Exception e) {
            ApiError error = new ApiError(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno", e, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @GetMapping("/celular/{celularId}")
    @Operation(summary = "Listar dispositivos por celular", description = "Retorna todas as configurações de dispositivos Rele associadas ao celular informado")
    public ResponseEntity<List<ReleResponseDTO>> listarPorCelular(@PathVariable String celularId) {
        List<ReleResponseDTO> lista = releService.listarPorCelular(celularId);
        return ResponseEntity.ok(lista);
    }

    @GetMapping
    @Operation(summary = "Listar todos dispositivos Rele", description = "Retorna todas as configurações de dispositivos Rele")
    public ResponseEntity<List<ReleResponseDTO>> listarTodos() {
        List<ReleResponseDTO> lista = releService.listarTodos();
        return ResponseEntity.ok(lista);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar dispositivo Rele", description = "Atualiza um dispositivo Rele existente")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Dispositivo atualizado com sucesso",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ReleResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Dispositivo ou máquina não encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<?> atualizar(@Parameter(description = "ID do dispositivo Rele", required = true, example = "1")
                                         @PathVariable("id") Integer id,
                                         @Valid @RequestBody ReleCreateDTO dto) {
        try {
            ReleResponseDTO response = releService.atualizar(id, dto);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            ApiError error = new ApiError(HttpStatus.NOT_FOUND, "Recurso não encontrado", e, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        } catch (Exception e) {
            ApiError error = new ApiError(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno", e, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir dispositivo Rele", description = "Remove fisicamente o dispositivo Rele do banco de dados")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Dispositivo removido com sucesso"),
            @ApiResponse(responseCode = "404", description = "Dispositivo não encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<?> excluir(@Parameter(description = "ID do dispositivo Rele", required = true, example = "1")
                                       @PathVariable("id") Integer id) {
        try {
            releService.excluir(id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            ApiError error = new ApiError(HttpStatus.NOT_FOUND, "Dispositivo não encontrado", e, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        } catch (Exception e) {
            ApiError error = new ApiError(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno", e, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}
