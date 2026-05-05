package br.compneusgppremium.api.controller;

import br.compneusgppremium.api.controller.dto.PneuVulcanizadoCreateDTO;
import br.compneusgppremium.api.controller.dto.PneuVulcanizadoResponseDTO;
import br.compneusgppremium.api.controller.dto.PneuVulcanizadoUpdateDTO;
import br.compneusgppremium.api.service.PneuVulcanizadoService;
import br.compneusgppremium.api.util.ApiError;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.HttpStatus;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Controller para gerenciamento de pneus vulcanizados
 */
@RestController
@RequestMapping("/api/pneu-vulcanizado")
@Tag(name = "Pneu Vulcanizado", description = "Endpoints para gerenciamento de pneus vulcanizados")
public class PneuVulcanizadoController {

    @Autowired
    private PneuVulcanizadoService pneuVulcanizadoService;

    @Autowired
    private br.compneusgppremium.api.util.UsuarioLogadoUtil usuarioLogadoUtil;

    /**
     * Criar um novo pneu vulcanizado
     */
    @PostMapping
    @Operation(summary = "Criar pneu vulcanizado", description = "Cria um novo registro de pneu vulcanizado com status inicial 'INICIADO'")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Pneu vulcanizado criado com sucesso",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = PneuVulcanizadoResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Usuário ou produção não encontrados",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<?> criarPneuVulcanizado(@Valid @RequestBody PneuVulcanizadoCreateDTO dto) {
        try {
            Long usuarioId = usuarioLogadoUtil.getUsuarioIdLogado();
            PneuVulcanizadoResponseDTO response = pneuVulcanizadoService.criar(dto, usuarioId);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            ApiError error = new ApiError(HttpStatus.BAD_REQUEST, "Dados inválidos", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (RuntimeException e) {
            ApiError error = new ApiError(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno", extractRootCauseMessage(e));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Listar pneus vulcanizados com paginação
     */
    @GetMapping
    @Operation(summary = "Listar pneus vulcanizados", description = "Lista todos os pneus vulcanizados com paginação")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de pneus vulcanizados retornada com sucesso")
    })
    public ResponseEntity<Page<PneuVulcanizadoResponseDTO>> listarPneusVulcanizados(
            @Parameter(description = "Número da página (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamanho da página", example = "10")
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "ID do usuário para filtrar")
            @RequestParam(required = false) Long usuarioId,
            @Parameter(description = "Status para filtrar (INICIADO, FINALIZADO)")
            @RequestParam(required = false) String status) {
        
        Page<PneuVulcanizadoResponseDTO> pneus = pneuVulcanizadoService.listarTodos(page, size, usuarioId, status);
        return ResponseEntity.ok(pneus);
    }

    /**
     * Buscar pneu vulcanizado por ID
     */
    @GetMapping("/{id}")
    @Operation(summary = "Buscar pneu vulcanizado por ID", description = "Retorna um pneu vulcanizado específico pelo ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pneu vulcanizado encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = PneuVulcanizadoResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Pneu vulcanizado não encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<?> buscarPorId(@PathVariable Long id) {
        try {
            PneuVulcanizadoResponseDTO response = pneuVulcanizadoService.buscarPorId(id);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            ApiError error = new ApiError(HttpStatus.NOT_FOUND, "Pneu vulcanizado não encontrado", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    /**
     * Atualizar status do pneu vulcanizado
     */
    @PutMapping("/{id}")
    @Operation(summary = "Atualizar pneu vulcanizado", description = "Atualiza o status de um pneu vulcanizado")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pneu vulcanizado atualizado com sucesso",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = PneuVulcanizadoResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Pneu vulcanizado não encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<?> atualizarPneuVulcanizado(
            @PathVariable Long id,
            @Valid @RequestBody PneuVulcanizadoUpdateDTO dto) {
        try {
            PneuVulcanizadoResponseDTO response = pneuVulcanizadoService.atualizar(id, dto);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            ApiError error = new ApiError(HttpStatus.BAD_REQUEST, "Dados inválidos", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (RuntimeException e) {
            ApiError error = new ApiError(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno", extractRootCauseMessage(e));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Finalizar pneu vulcanizado
     */
    @PutMapping("/{id}/finalizar")
    @Operation(summary = "Finalizar pneu vulcanizado", description = "Marca um pneu vulcanizado como finalizado")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pneu vulcanizado finalizado com sucesso",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = PneuVulcanizadoResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Pneu vulcanizado não encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "400", description = "Pneu vulcanizado já finalizado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<?> finalizarPneuVulcanizado(@PathVariable Long id) {
        try {
            PneuVulcanizadoResponseDTO response = pneuVulcanizadoService.finalizar(id);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            ApiError error = new ApiError(HttpStatus.BAD_REQUEST, "Erro ao finalizar", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (RuntimeException e) {
            ApiError error = new ApiError(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno", extractRootCauseMessage(e));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Listar pneus vulcanizados por usuário
     */
    @GetMapping("/usuario/{usuarioId}")
    @Operation(summary = "Listar pneus vulcanizados por usuário", description = "Retorna todos os pneus vulcanizados de um usuário específico")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de pneus vulcanizados retornada com sucesso")
    })
    public ResponseEntity<List<PneuVulcanizadoResponseDTO>> listarPorUsuario(@PathVariable Long usuarioId) {
        List<PneuVulcanizadoResponseDTO> pneus = pneuVulcanizadoService.listarPorUsuario(usuarioId);
        return ResponseEntity.ok(pneus);
    }

    /**
     * Listar pneus vulcanizados por status
     */
    @GetMapping("/status/{status}")
    @Operation(summary = "Listar pneus vulcanizados por status", description = "Retorna todos os pneus vulcanizados com um status específico")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de pneus vulcanizados retornada com sucesso")
    })
    public ResponseEntity<List<PneuVulcanizadoResponseDTO>> listarPorStatus(
            @Parameter(description = "Status (INICIADO, FINALIZADO)")
            @PathVariable String status) {
        try {
            List<PneuVulcanizadoResponseDTO> pneus = pneuVulcanizadoService.listarPorStatus(status);
            return ResponseEntity.ok(pneus);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Deletar pneu vulcanizado (soft delete)
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Deletar pneu vulcanizado", description = "Remove um pneu vulcanizado (soft delete)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Pneu vulcanizado deletado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Pneu vulcanizado não encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<?> deletarPneuVulcanizado(@PathVariable Long id) {
        try {
            pneuVulcanizadoService.deletar(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            ApiError error = new ApiError(HttpStatus.NOT_FOUND, "Pneu vulcanizado não encontrado", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    /**
     * Obter estatísticas de pneus vulcanizados
     */
    @GetMapping("/estatisticas")
    @Operation(summary = "Obter estatísticas", description = "Retorna estatísticas dos pneus vulcanizados")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Estatísticas retornadas com sucesso")
    })
    public ResponseEntity<Map<String, Object>> obterEstatisticas(
            @Parameter(description = "ID do usuário para filtrar estatísticas")
            @RequestParam(required = false) Long usuarioId) {
        
        Map<String, Object> estatisticas = new HashMap<>();
        
        if (usuarioId != null) {
            estatisticas.put("totalPorUsuario", pneuVulcanizadoService.contarPorUsuario(usuarioId));
        }
        
        estatisticas.put("totalIniciados", pneuVulcanizadoService.contarPorStatus("INICIADO"));
        estatisticas.put("totalFinalizados", pneuVulcanizadoService.contarPorStatus("FINALIZADO"));
        estatisticas.put("total", pneuVulcanizadoService.contarPorStatus("INICIADO") + 
                                 pneuVulcanizadoService.contarPorStatus("FINALIZADO"));
        
        return ResponseEntity.ok(estatisticas);
    }

    /**
     * Extrai a mensagem de erro raiz de uma exceção
     */
    private String extractRootCauseMessage(Exception e) {
        Throwable cause = e;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause.getMessage();
    }
}