package br.compneusgppremium.api.controller;

import br.compneusgppremium.api.controller.dto.*;
import br.compneusgppremium.api.controller.model.*;
import br.compneusgppremium.api.repository.*;
import br.compneusgppremium.api.util.ApiError;
import br.compneusgppremium.api.util.UsuarioLogadoUtil;
import org.springframework.security.access.prepost.PreAuthorize;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;
import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/registro-maquina")
@Tag(name = "Registro de Máquinas", description = "Operações relacionadas ao registro e gerenciamento de máquinas")
@SecurityRequirement(name = "Bearer Authentication")
public class RegistroMaquinaController {

    @Autowired
    private RegistroMaquinaRepository registroMaquinaRepository;



    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private UsuarioLogadoUtil usuarioLogadoUtil;

    @Operation(summary = "Listar todas as máquinas", description = "Retorna todas as máquinas registradas no sistema")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de máquinas retornada com sucesso"),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    @GetMapping(produces = "application/json; charset=UTF-8")
    public ResponseEntity<List<RegistroMaquinaResponseDTO>> listarMaquinas() {
        try {
            List<RegistroMaquinaModel> maquinas = registroMaquinaRepository.findByDtDeleteIsNullOrderByDtCreateDesc();
            List<RegistroMaquinaResponseDTO> response = maquinas.stream()
                    .map(this::convertToResponseDTO)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "Listar máquinas disponíveis para relé", description = "Retorna somente as máquinas que não estão configuradas em nenhum relé ativo. Se a configuração for removida da tabela de relé, a máquina volta a aparecer nesta lista.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de máquinas disponíveis retornada com sucesso"),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    @GetMapping(path = "/disponiveis", produces = "application/json; charset=UTF-8")
    public ResponseEntity<List<RegistroMaquinaResponseDTO>> listarMaquinasDisponiveisParaRele() {
        try {
            List<RegistroMaquinaModel> maquinas = registroMaquinaRepository.findDisponiveisParaRele();
            List<RegistroMaquinaResponseDTO> response = maquinas.stream()
                    .map(this::convertToResponseDTO)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "Buscar máquina por ID", description = "Retorna uma máquina específica pelo seu ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Máquina encontrada"),
            @ApiResponse(responseCode = "404", description = "Máquina não encontrada"),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    @GetMapping(path = "/{id}", produces = "application/json; charset=UTF-8")
    public ResponseEntity<RegistroMaquinaResponseDTO> buscarPorId(
            @Parameter(description = "ID da máquina") @PathVariable Integer id) {
        try {
            Optional<RegistroMaquinaModel> maquina = registroMaquinaRepository.findByIdAndDtDeleteIsNull(id);
            if (maquina.isPresent()) {
                return ResponseEntity.ok(convertToResponseDTO(maquina.get()));
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "Buscar máquina por nome", description = "Retorna uma máquina específica pelo seu nome")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Máquina encontrada"),
            @ApiResponse(responseCode = "404", description = "Máquina não encontrada"),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    @GetMapping(path = "/nome/{nome}", produces = "application/json; charset=UTF-8")
    public ResponseEntity<RegistroMaquinaResponseDTO> buscarPorNome(
            @Parameter(description = "Nome da máquina") @PathVariable String nome) {
        try {
            Optional<RegistroMaquinaModel> maquina = registroMaquinaRepository.findByNomeAndDtDeleteIsNull(nome);
            if (maquina.isPresent()) {
                return ResponseEntity.ok(convertToResponseDTO(maquina.get()));
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "Registrar nova máquina", description = "Registra uma nova máquina no sistema")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Máquina registrada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Device ID já existe ou dados inválidos"),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    @PostMapping(produces = "application/json; charset=UTF-8")
    @Transactional
    public ResponseEntity<?> registrarMaquina(@RequestBody RegistroMaquinaCreateDTO dto) {
        try {
            // Verificar se já existe uma máquina com o mesmo número de série
            boolean exists = registroMaquinaRepository.existsByNumeroSerieAndDtDeleteIsNull(dto.getNumeroSerie());
            
            if (exists) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ApiError(HttpStatus.BAD_REQUEST, 
                                "Número de série já existe", 
                                "Já existe uma máquina registrada com o número de série: " + dto.getNumeroSerie()));
            }

            // Criar nova máquina
            RegistroMaquinaModel novaMaquina = new RegistroMaquinaModel();
            novaMaquina.setNome(dto.getNome());
            novaMaquina.setDescricao(dto.getDescricao());
            novaMaquina.setNumeroSerie(dto.getNumeroSerie());
            novaMaquina.setStatus(StatusMaquina.Ativa); // Status padrão

            RegistroMaquinaModel maquinaSalva = registroMaquinaRepository.save(novaMaquina);
            return ResponseEntity.status(HttpStatus.CREATED).body(convertToResponseDTO(maquinaSalva));

        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // Capturar especificamente erros de violação de integridade (como duplicação de número de série)
            String rootCauseMessage = extractRootCauseMessage(e);
            
            if (rootCauseMessage.toLowerCase().contains("numero_serie") || 
                rootCauseMessage.toLowerCase().contains("unique")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ApiError(HttpStatus.BAD_REQUEST, 
                                "Número de série já existe", 
                                "O número de série '" + dto.getNumeroSerie() + "' já está sendo usado por outra máquina."));
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiError(HttpStatus.BAD_REQUEST, 
                            "Erro de integridade de dados", 
                            "Falha na validação dos dados: " + rootCauseMessage));
        } catch (Exception e) {
            String detailedError = extractRootCauseMessage(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiError(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno do servidor", detailedError));
        }
    }

    @Operation(summary = "Atualizar máquina", description = "Atualiza os dados de uma máquina existente")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Máquina atualizada com sucesso"),
            @ApiResponse(responseCode = "404", description = "Máquina não encontrada"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    @PutMapping(path = "/{id}", produces = "application/json; charset=UTF-8")
    @Transactional
    public ResponseEntity<?> atualizarMaquina(
            @Parameter(description = "ID da máquina") @PathVariable Integer id,
            @Valid @RequestBody RegistroMaquinaUpdateDTO dto) {
        try {
            Optional<RegistroMaquinaModel> maquinaOpt = registroMaquinaRepository.findByIdAndDtDeleteIsNull(id);
            if (!maquinaOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }

            RegistroMaquinaModel maquina = maquinaOpt.get();

            // Atualizar campos se fornecidos
            if (dto.getNome() != null) {
                maquina.setNome(dto.getNome());
            }
            if (dto.getDescricao() != null) {
                maquina.setDescricao(dto.getDescricao());
            }
            if (dto.getStatus() != null) {
                maquina.setStatus(dto.getStatus());
            }
            if (dto.getNumeroSerie() != null) {
                // Verificar se o número de série já existe em outra máquina
                if (registroMaquinaRepository.existsByNumeroSerieAndIdNotAndDtDeleteIsNull(dto.getNumeroSerie(), id)) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(new ApiError(HttpStatus.BAD_REQUEST, "Número de série duplicado", 
                                    "Não é possível atualizar a máquina. O número de série '" + dto.getNumeroSerie() + "' já está sendo utilizado por outra máquina no sistema. Por favor, verifique o número de série e tente novamente."));
                }
                maquina.setNumeroSerie(dto.getNumeroSerie());
            }

            RegistroMaquinaModel maquinaAtualizada = registroMaquinaRepository.save(maquina);
            return ResponseEntity.ok(convertToResponseDTO(maquinaAtualizada));

        } catch (DataIntegrityViolationException e) {
            // Capturar especificamente erros de violação de integridade (como duplicação de número de série)
            String rootCauseMessage = extractRootCauseMessage(e);
            if (rootCauseMessage.toLowerCase().contains("numero_serie") || 
                rootCauseMessage.toLowerCase().contains("unique")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ApiError(HttpStatus.BAD_REQUEST, "Número de série duplicado", 
                                "Falha ao atualizar a máquina. O número de série '" + dto.getNumeroSerie() + "' já está sendo utilizado por outra máquina no sistema. Cada máquina deve ter um número de série único. Por favor, verifique e utilize um número de série diferente."));
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiError(HttpStatus.BAD_REQUEST, 
                            "Erro de integridade de dados", 
                            e, 
                            "Falha na validação dos dados: " + rootCauseMessage));
        } catch (Exception e) {
            String detailedError = extractRootCauseMessage(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiError(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno do servidor", e, detailedError));
        }
    }

    @Operation(summary = "Deletar máquina", description = "Remove uma máquina do sistema (soft delete)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Máquina deletada com sucesso"),
            @ApiResponse(responseCode = "404", description = "Máquina não encontrada"),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    @DeleteMapping(path = "/{id}", produces = "application/json; charset=UTF-8")
    @Transactional
    public ResponseEntity<?> deletarMaquina(
            @Parameter(description = "ID da máquina") @PathVariable Integer id) {
        try {
            Optional<RegistroMaquinaModel> maquinaOpt = registroMaquinaRepository.findByIdAndDtDeleteIsNull(id);
            if (!maquinaOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }

            RegistroMaquinaModel maquina = maquinaOpt.get();
            maquina.softDelete(); // Método que define dt_delete e status INATIVA
            registroMaquinaRepository.save(maquina);

            return ResponseEntity.ok().build();

        } catch (Exception e) {
            String detailedError = extractRootCauseMessage(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiError(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno do servidor", e, detailedError));
        }
    }

    @Operation(summary = "Listar máquinas ativas", description = "Retorna apenas as máquinas com status ATIVA")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de máquinas ativas retornada com sucesso"),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    @GetMapping(path = "/ativas", produces = "application/json; charset=UTF-8")
    public ResponseEntity<List<RegistroMaquinaResponseDTO>> listarMaquinasAtivas() {
        try {
            List<RegistroMaquinaModel> maquinas = registroMaquinaRepository
                    .findByStatusAndDtDeleteIsNullOrderByDtCreateDesc(StatusMaquina.Ativa);
            List<RegistroMaquinaResponseDTO> response = maquinas.stream()
                    .map(this::convertToResponseDTO)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "Alterar status da máquina", description = "Altera o status de uma máquina específica")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Status alterado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Máquina não encontrada"),
            @ApiResponse(responseCode = "400", description = "Status inválido"),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    @PutMapping(path = "/{id}/status", produces = "application/json; charset=UTF-8")
    @Transactional
    public ResponseEntity<?> alterarStatus(
            @Parameter(description = "ID da máquina") @PathVariable Integer id,
            @Parameter(description = "Novo status da máquina") @RequestParam StatusMaquina status) {
        try {
            Optional<RegistroMaquinaModel> maquinaOpt = registroMaquinaRepository.findByIdAndDtDeleteIsNull(id);
            if (!maquinaOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }

            RegistroMaquinaModel maquina = maquinaOpt.get();
            maquina.setStatus(status);
            RegistroMaquinaModel maquinaAtualizada = registroMaquinaRepository.save(maquina);

            return ResponseEntity.ok(convertToResponseDTO(maquinaAtualizada));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiError(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno do servidor", e, "Erro ao alterar status da máquina"));
        }
    }

    @Operation(summary = "Colocar máquina em manutenção", description = "Altera o status da máquina para MANUTENCAO")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Máquina colocada em manutenção com sucesso"),
            @ApiResponse(responseCode = "404", description = "Máquina não encontrada"),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    @PutMapping(path = "/{id}/manutencao", produces = "application/json; charset=UTF-8")
    @Transactional
    public ResponseEntity<?> colocarEmManutencao(
            @Parameter(description = "ID da máquina") @PathVariable Integer id) {
        try {
            Optional<RegistroMaquinaModel> maquinaOpt = registroMaquinaRepository.findByIdAndDtDeleteIsNull(id);
            if (!maquinaOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }

            RegistroMaquinaModel maquina = maquinaOpt.get();
            maquina.colocarEmManutencao();
            RegistroMaquinaModel maquinaAtualizada = registroMaquinaRepository.save(maquina);

            return ResponseEntity.ok(convertToResponseDTO(maquinaAtualizada));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiError(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno do servidor", e, "Erro ao colocar máquina em manutenção"));
        }
    }

    @Operation(summary = "Ativar máquina", description = "Altera o status da máquina para ATIVA")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Máquina ativada com sucesso"),
            @ApiResponse(responseCode = "404", description = "Máquina não encontrada"),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    @PutMapping(path = "/{id}/ativar", produces = "application/json; charset=UTF-8")
    @Transactional
    public ResponseEntity<?> ativarMaquina(
            @Parameter(description = "ID da máquina") @PathVariable Integer id) {
        try {
            Optional<RegistroMaquinaModel> maquinaOpt = registroMaquinaRepository.findByIdAndDtDeleteIsNull(id);
            if (!maquinaOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }

            RegistroMaquinaModel maquina = maquinaOpt.get();
            maquina.ativar();
            RegistroMaquinaModel maquinaAtualizada = registroMaquinaRepository.save(maquina);

            return ResponseEntity.ok(convertToResponseDTO(maquinaAtualizada));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiError(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno do servidor", e, "Erro ao ativar máquina"));
        }
    }

    @Operation(summary = "Inativar máquina", description = "Altera o status da máquina para INATIVA")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Máquina inativada com sucesso"),
            @ApiResponse(responseCode = "404", description = "Máquina não encontrada"),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    @PutMapping(path = "/{id}/inativar", produces = "application/json; charset=UTF-8")
    @Transactional
    public ResponseEntity<?> inativarMaquina(
            @Parameter(description = "ID da máquina") @PathVariable Integer id) {
        try {
            Optional<RegistroMaquinaModel> maquinaOpt = registroMaquinaRepository.findByIdAndDtDeleteIsNull(id);
            if (!maquinaOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }

            RegistroMaquinaModel maquina = maquinaOpt.get();
            maquina.inativar();
            RegistroMaquinaModel maquinaAtualizada = registroMaquinaRepository.save(maquina);

            return ResponseEntity.ok(convertToResponseDTO(maquinaAtualizada));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiError(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno do servidor", e, "Erro ao inativar máquina"));
        }
    }

    @Operation(summary = "Listar máquinas em manutenção", description = "Retorna apenas as máquinas com status MANUTENCAO")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de máquinas em manutenção retornada com sucesso"),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    @GetMapping(path = "/manutencao", produces = "application/json; charset=UTF-8")
    public ResponseEntity<List<RegistroMaquinaResponseDTO>> listarMaquinasEmManutencao() {
        try {
            List<RegistroMaquinaModel> maquinas = registroMaquinaRepository
                    .findByStatusAndDtDeleteIsNull(StatusMaquina.Manutencao);
            List<RegistroMaquinaResponseDTO> response = maquinas.stream()
                    .map(this::convertToResponseDTO)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Método auxiliar para converter Model para ResponseDTO
    private RegistroMaquinaResponseDTO convertToResponseDTO(RegistroMaquinaModel model) {
        RegistroMaquinaResponseDTO dto = new RegistroMaquinaResponseDTO();
        dto.setId(model.getId());
        dto.setNome(model.getNome());
        dto.setDescricao(model.getDescricao());
        dto.setNumeroSerie(model.getNumeroSerie());
        dto.setStatus(model.getStatus());
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
