package br.compneusgppremium.api.controller;

import br.compneusgppremium.api.controller.dto.ConfiguracaoMaquinaCreateDTO;
import br.compneusgppremium.api.controller.dto.ConfiguracaoMaquinaResponseDTO;
import br.compneusgppremium.api.controller.dto.ConfiguracaoMaquinaUpdateDTO;
import br.compneusgppremium.api.controller.model.ConfiguracaoMaquinaModel;
import br.compneusgppremium.api.controller.model.MatrizModel;
import br.compneusgppremium.api.controller.model.RegistroMaquinaModel;
import br.compneusgppremium.api.repository.ConfiguracaoMaquinaRepository;
import br.compneusgppremium.api.repository.SonoffRepository;
import br.compneusgppremium.api.repository.RegistroMaquinaRepository;
import br.compneusgppremium.api.repository.UsuarioRepository;
import br.compneusgppremium.api.repository.MatrizRepository;
import br.compneusgppremium.api.util.ApiError;
import br.compneusgppremium.api.util.UsuarioLogadoUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;

/**
 * Controller para gerenciamento de configurações de máquinas
 */
@RestController
@RequestMapping("/api/configuracao-maquina")
@Tag(name = "Configuração de Máquina", description = "Endpoints para gerenciamento de configurações de máquinas")
public class ConfiguracaoMaquinaController {

    @Autowired
    private ConfiguracaoMaquinaRepository configuracaoMaquinaRepository;

    @Autowired
    private SonoffRepository sonoffRepository;

    @Autowired
    private RegistroMaquinaRepository registroMaquinaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private MatrizRepository matrizRepository;

    @Autowired
    private UsuarioLogadoUtil usuarioLogadoUtil;

    /**
     * Criar nova configuração de máquina
     */
    @PostMapping
    @Operation(summary = "Criar configuração de máquina", description = "Cria uma nova configuração para uma máquina")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Configuração criada com sucesso",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ConfiguracaoMaquinaResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Máquina não encontrada",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<?> criarConfiguracao(@Valid @RequestBody ConfiguracaoMaquinaCreateDTO dto) {
        try {
            // Verificar se a máquina existe
            Optional<RegistroMaquinaModel> maquinaOpt = registroMaquinaRepository.findByIdAndDtDeleteIsNull(dto.getMaquinaId());
            if (!maquinaOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiError(HttpStatus.NOT_FOUND, "Máquina não encontrada", null, "Máquina com ID " + dto.getMaquinaId() + " não foi encontrada"));
            }

            // Verificar se a matriz existe
            Optional<MatrizModel> matrizOpt = matrizRepository.findById(dto.getMatrizId());
            if (!matrizOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiError(HttpStatus.NOT_FOUND, "Matriz não encontrada", null, "Matriz com ID " + dto.getMatrizId() + " não foi encontrada"));
            }

            // Obter o usuário logado
            Integer usuarioId = usuarioLogadoUtil.getUsuarioIdLogado();

            // Criar nova configuração
            ConfiguracaoMaquinaModel configuracao = new ConfiguracaoMaquinaModel();
            configuracao.setMaquina(maquinaOpt.get());
            configuracao.setMatriz(matrizOpt.get());
            configuracao.setCelularId(dto.getCelularId());
            configuracao.setDescricao(dto.getDescricao());
            configuracao.setAtributos(dto.getAtributos());
            configuracao.setUsuarioId(usuarioId);
            configuracao.setDtCreate(LocalDateTime.now());

            ConfiguracaoMaquinaModel savedConfiguracao = configuracaoMaquinaRepository.save(configuracao);

            // Converter para DTO de resposta
            ConfiguracaoMaquinaResponseDTO response = convertToResponseDTO(savedConfiguracao);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            String detailedError = extractRootCauseMessage(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiError(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno do servidor", e, "Erro ao criar configuração: " + detailedError));
        }
    }

    /**
     * Listar configurações com paginação
     */
    @GetMapping
    @Operation(summary = "Listar configurações", description = "Lista todas as configurações de máquinas com paginação")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de configurações retornada com sucesso")
    })
    public ResponseEntity<Page<ConfiguracaoMaquinaResponseDTO>> listarConfiguracoes(
            @Parameter(description = "Número da página (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamanho da página", example = "10")
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "ID da máquina para filtrar")
            @RequestParam(required = false) Long maquinaId) {

        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<ConfiguracaoMaquinaModel> configuracoes;

            if (maquinaId != null) {
                configuracoes = configuracaoMaquinaRepository.findByMaquinaIdAndDtDeleteIsNullOrderByDtCreateDesc(maquinaId, pageable);
            } else {
                configuracoes = configuracaoMaquinaRepository.findByDtDeleteIsNullOrderByDtCreateDesc(pageable);
            }

            Page<ConfiguracaoMaquinaResponseDTO> response = configuracoes.map(this::convertToResponseDTO);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Buscar configuração ativa por celular
     */
    @GetMapping("/celular/{celularId}/ativa")
    @Operation(summary = "Buscar configuração ativa por celular", description = "Retorna a configuração ativa (mais recente) para um celular específico")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Configuração ativa encontrada",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ConfiguracaoMaquinaResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Nenhuma configuração ativa encontrada para este celular",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<?> buscarConfiguracaoAtivaPorCelular(
            @Parameter(description = "ID do celular", example = "CEL001")
            @PathVariable String celularId) {
        try {
            Pageable pageable = PageRequest.of(0, 1);
            List<ConfiguracaoMaquinaModel> configuracoes = configuracaoMaquinaRepository.findActiveByCelularId(celularId, pageable);
            
            if (configuracoes.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiError(HttpStatus.NOT_FOUND, "Configuração não encontrada", null, 
                                "Nenhuma configuração ativa encontrada para o celular: " + celularId));
            }

            // Pega apenas o primeiro resultado (mais atual devido ao ORDER BY c.dtCreate DESC)
            ConfiguracaoMaquinaModel configuracao = configuracoes.get(0);
            ConfiguracaoMaquinaResponseDTO response = convertToResponseDTO(configuracao);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            String detailedError = extractRootCauseMessage(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiError(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno do servidor", e, 
                            "Erro ao buscar configuração ativa: " + detailedError));
        }
    }

    /**
     * Buscar a MÁQUINA configurada para um celular
     * Prioriza associação ativa em relés (Sonoff). Caso não exista, tenta a configuração ativa de máquina.
     */
    @GetMapping("/celular/{celularId}/maquina")
    @Operation(summary = "Buscar máquina configurada por celular", description = "Retorna exclusivamente a máquina associada a um relé (Sonoff) ativo para o celular informado. Não há fallback para configuração de máquina.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Máquina configurada encontrada",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = br.compneusgppremium.api.controller.dto.RegistroMaquinaResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Nenhuma máquina configurada para este celular",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<?> buscarMaquinaConfiguradaPorCelular(
            @Parameter(description = "ID do celular", example = "CEL001")
            @PathVariable String celularId) {
        try {
            // 1) Prioriza associação via relé (Sonoff) ativo
            List<br.compneusgppremium.api.controller.model.SonoffModel> sonoffs = sonoffRepository
                    .findByCelularIdAndDtDeleteIsNullOrderByDtCreateDesc(celularId);

            if (!sonoffs.isEmpty()) {
                RegistroMaquinaModel maquina = sonoffs.get(0).getMaquina();
                if (maquina != null) {
                    br.compneusgppremium.api.controller.dto.RegistroMaquinaResponseDTO dto = convertMaquinaToResponseDTO(maquina);
                    return ResponseEntity.ok(dto);
                }
            }

            // Nada encontrado
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiError(HttpStatus.NOT_FOUND, "Máquina não encontrada", null,
                            "Nenhuma máquina configurada encontrada para o celular: " + celularId));

        } catch (Exception e) {
            String detailedError = extractRootCauseMessage(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiError(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno do servidor", e,
                            "Erro ao buscar máquina configurada: " + detailedError));
        }
    }

    /**
     * Listar histórico de configurações por celular
     */
    @GetMapping("/celular/{celularId}/historico")
    @Operation(summary = "Listar histórico de configurações por celular", description = "Retorna todas as configurações de um celular específico ordenadas por data de criação")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de configurações retornada com sucesso")
    })
    public ResponseEntity<?> listarHistoricoConfiguracoesPorCelular(
            @Parameter(description = "ID do celular", example = "CEL001")
            @PathVariable String celularId) {
        try {
            List<ConfiguracaoMaquinaModel> configuracoes = configuracaoMaquinaRepository.findByCelularIdAndDtDeleteIsNullOrderByDtCreateDesc(celularId);
            
            List<ConfiguracaoMaquinaResponseDTO> response = configuracoes.stream()
                    .map(this::convertToResponseDTO)
                    .collect(java.util.stream.Collectors.toList());
            
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            String detailedError = extractRootCauseMessage(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiError(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno do servidor", e, 
                            "Erro ao buscar histórico de configurações: " + detailedError));
        }
    }

    /**
     * Buscar configuração por ID
     */
    @GetMapping("/{id}")
    @Operation(summary = "Buscar configuração por ID", description = "Retorna uma configuração específica pelo ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Configuração encontrada",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ConfiguracaoMaquinaResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Configuração não encontrada",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<?> buscarPorId(@PathVariable Long id) {
        try {
            Optional<ConfiguracaoMaquinaModel> configuracaoOpt = configuracaoMaquinaRepository.findByIdAndDtDeleteIsNull(id);
            if (!configuracaoOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiError(HttpStatus.NOT_FOUND, "Configuração não encontrada", null, "Configuração com ID " + id + " não foi encontrada"));
            }

            ConfiguracaoMaquinaResponseDTO response = convertToResponseDTO(configuracaoOpt.get());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            String detailedError = extractRootCauseMessage(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiError(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno do servidor", e, "Erro ao buscar configuração: " + detailedError));
        }
    }

    /**
     * Atualizar configuração
     */
    @PutMapping("/{id}")
    @Operation(summary = "Atualizar configuração", description = "Atualiza uma configuração existente")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Configuração atualizada com sucesso",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ConfiguracaoMaquinaResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Configuração não encontrada",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<?> atualizarConfiguracao(
            @PathVariable Long id,
            @Valid @RequestBody ConfiguracaoMaquinaUpdateDTO dto) {

        try {
            Optional<ConfiguracaoMaquinaModel> configuracaoOpt = configuracaoMaquinaRepository.findByIdAndDtDeleteIsNull(id);
            if (!configuracaoOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiError(HttpStatus.NOT_FOUND, "Configuração não encontrada", null, "Configuração com ID " + id + " não foi encontrada"));
            }

            ConfiguracaoMaquinaModel configuracao = configuracaoOpt.get();

            // Atualizar campos
            if (dto.getCelularId() != null) {
                configuracao.setCelularId(dto.getCelularId());
            }
            if (dto.getDescricao() != null) {
                configuracao.setDescricao(dto.getDescricao());
            }
            if (dto.getAtributos() != null) {
                configuracao.setAtributos(dto.getAtributos());
            }
            configuracao.setDtUpdate(LocalDateTime.now());

            ConfiguracaoMaquinaModel savedConfiguracao = configuracaoMaquinaRepository.save(configuracao);

            ConfiguracaoMaquinaResponseDTO response = convertToResponseDTO(savedConfiguracao);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            String detailedError = extractRootCauseMessage(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiError(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno do servidor", e, "Erro ao atualizar configuração: " + detailedError));
        }
    }

    /**
     * Deletar configuração (soft delete)
     * Após a exclusão, automaticamente ativa a configuração anterior mais recente para a mesma máquina
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Deletar configuração", 
               description = "Remove uma configuração (soft delete). Automaticamente ativa a configuração anterior mais recente para a mesma máquina.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Configuração deletada com sucesso"),
            @ApiResponse(responseCode = "404", description = "Configuração não encontrada",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<?> deletarConfiguracao(@PathVariable Long id) {
        try {
            Optional<ConfiguracaoMaquinaModel> configuracaoOpt = configuracaoMaquinaRepository.findByIdAndDtDeleteIsNull(id);
            if (!configuracaoOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiError(HttpStatus.NOT_FOUND, "Configuração não encontrada", null, "Configuração com ID " + id + " não foi encontrada"));
            }

            ConfiguracaoMaquinaModel configuracao = configuracaoOpt.get();
            Long maquinaId = configuracao.getMaquina().getId();
            
            // Realiza o soft delete da configuração atual
            configuracao.softDelete();
            configuracaoMaquinaRepository.save(configuracao);

            // Busca a configuração anterior ativa para a mesma máquina
            Optional<ConfiguracaoMaquinaModel> configuracaoAnteriorOpt = 
                configuracaoMaquinaRepository.findPreviousActiveBymaquinaIdExcludingId(maquinaId, id);
            
            // Se existe uma configuração anterior, ela automaticamente se torna a ativa
            // (não precisa fazer nada, pois a lógica de "ativa" é baseada na mais recente não deletada)
            
            return ResponseEntity.noContent().build();

        } catch (Exception e) {
            String detailedError = extractRootCauseMessage(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiError(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno do servidor", e, "Erro ao deletar configuração: " + detailedError));
        }
    }



    /**
     * Converte um modelo para DTO de resposta
     */
    private ConfiguracaoMaquinaResponseDTO convertToResponseDTO(ConfiguracaoMaquinaModel configuracao) {
        return new ConfiguracaoMaquinaResponseDTO(
                configuracao.getId(),
                configuracao.getCelularId(),
                configuracao.getDescricao(),
                configuracao.getAtributos(),
                configuracao.getMatriz() != null ? configuracao.getMatriz().getId() : null,
                configuracao.getMatriz() != null ? "Matriz " + configuracao.getMatriz().getId() : null,
                configuracao.getMaquina() != null ? configuracao.getMaquina().getId() : null,
                configuracao.getMaquina() != null ? configuracao.getMaquina().getNome() : null,
                configuracao.getDtCreate(),
                configuracao.getDtUpdate(),
                configuracao.getUsuarioId()
        );
    }

    /**
     * Converte RegistroMaquinaModel para RegistroMaquinaResponseDTO
     */
    private br.compneusgppremium.api.controller.dto.RegistroMaquinaResponseDTO convertMaquinaToResponseDTO(RegistroMaquinaModel model) {
        br.compneusgppremium.api.controller.dto.RegistroMaquinaResponseDTO dto = new br.compneusgppremium.api.controller.dto.RegistroMaquinaResponseDTO();
        dto.setId(model.getId());
        dto.setNome(model.getNome());
        dto.setNumeroSerie(model.getNumeroSerie());
        dto.setStatus(model.getStatus());
        dto.setDescricao(model.getDescricao());
        dto.setDtCreate(model.getDtCreate());
        dto.setDtUpdate(model.getDtUpdate());
        return dto;
    }

    /**
     * Extrai a mensagem da causa raiz de uma exceção
     */
    private String extractRootCauseMessage(Exception e) {
        Throwable rootCause = e;
        while (rootCause.getCause() != null && rootCause.getCause() != rootCause) {
            rootCause = rootCause.getCause();
        }
        
        String message = rootCause.getMessage();
        if (message != null && !message.isEmpty()) {
            return message;
        }
        
        return e.getMessage() != null ? e.getMessage() : "Erro desconhecido";
    }
}
