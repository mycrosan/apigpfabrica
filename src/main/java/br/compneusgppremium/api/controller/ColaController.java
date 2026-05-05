package br.compneusgppremium.api.controller;

import br.compneusgppremium.api.controller.dto.ColaComStatusDTO;
import br.compneusgppremium.api.controller.model.ColaModel;
import br.compneusgppremium.api.controller.model.ProducaoModel;
import br.compneusgppremium.api.controller.model.UsuarioModel;
import br.compneusgppremium.api.repository.*;
import br.compneusgppremium.api.util.ApiError;
import br.compneusgppremium.api.util.UsuarioLogadoUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/cola")
@Tag(name = "Cola", description = "Operações relacionadas ao processo de colagem de pneus")
@SecurityRequirement(name = "Bearer Authentication")
public class ColaController {
    @Autowired
    private ColaRepository colaRepository;

    @Autowired
    private ProducaoRepository producaoRepository;

    @Autowired
    private UsuarioLogadoUtil usuarioLogadoUtil;

    @Autowired
    private CoberturaRepository coberturaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;


    @Operation(summary = "Criar nova cola", description = "Cria uma nova cola para uma produção específica")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Cola criada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Produção não encontrada"),
            @ApiResponse(responseCode = "409", description = "Já existe cobertura cadastrada para esse pneu"),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    @PostMapping(produces = "application/json; charset=UTF-8")
    public ResponseEntity<?> salvar(@RequestBody ColaModel cola) {
        try {
            Optional<ProducaoModel> producaoOptional = producaoRepository.findById(cola.getProducao().getId());
            if (producaoOptional.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("mensagem", "Produção não encontrada."));
            }

            ProducaoModel producao = producaoOptional.get();

            // Verifica se já existe cobertura para esse pneu
            if (coberturaRepository.existsByColaProducaoId(producao.getId())) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("mensagem", "Já existe cobertura cadastrada para esse pneu."));
            }

            cola.setProducao(producao);

            Long userId = usuarioLogadoUtil.getUsuarioIdLogado();
            UsuarioModel usuario = usuarioRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
            cola.setUsuario(usuario);

            ColaModel saved = colaRepository.save(cola);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("mensagem", "Cola cadastrada com sucesso!", "dados", saved));

        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("mensagem", "Erro ao salvar cola"));
        }
    }


    @Operation(summary = "Listar colas sem cobertura", description = "Retorna todas as colas que ainda não possuem cobertura")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de colas sem cobertura retornada com sucesso"),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    @GetMapping(produces = "application/json; charset=UTF-8")
    public ResponseEntity<?> listar() { // O tipo de retorno agora é ResponseEntity
        try {
            // Busca a lista no repositório
            List<ColaModel> colas = colaRepository.findColasSemCobertura();

            // Retorna a lista no corpo da resposta com status 200 OK
            return ResponseEntity.ok(colas);

        } catch (Exception ex) {
            // Cria o objeto de erro padronizado
            ApiError apiError = new ApiError(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erro ao listar colas sem cobertura", ex, ex.getMessage());

            // Retorna o objeto de erro no corpo com o status 500 INTERNAL_SERVER_ERROR
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(apiError);
        }
    }

    //    // GET - Buscar por ID
//    @GetMapping(path = "/{id}", produces = "application/json; charset=UTF-8")
//    public ResponseEntity<Object> buscarPorId(@PathVariable("id") Integer id) {
//        return repository.findById(id)
//                .<ResponseEntity<Object>>map(ResponseEntity::ok)
//                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body("Cola não encontrada"));
//    }
//
    @Operation(summary = "Atualizar cola", description = "Atualiza uma cola existente, incluindo data de início e status")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cola atualizada com sucesso"),
            @ApiResponse(responseCode = "404", description = "Cola não encontrada"),
            @ApiResponse(responseCode = "409", description = "Já existe cobertura cadastrada para esse pneu"),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    @Transactional
    @PutMapping(path = "/{id}", produces = "application/json; charset=UTF-8")
    public ResponseEntity<?> atualizar(@Parameter(description = "ID da cola") @PathVariable("id") Integer id, @RequestBody ColaModel novaCola) {
        try {
            Optional<ColaModel> optionalCola = colaRepository.findById(id);
            if (optionalCola.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("mensagem", "Cola não encontrada."));
            }

            ColaModel colaExistente = optionalCola.get();

            // Valida se já existe cobertura para a produção dessa cola
            if (coberturaRepository.existsByColaProducaoId(colaExistente.getProducao().getId())) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("mensagem", "Já existe cobertura cadastrada para esse pneu."));
            }

            // Atualiza dataInicio
            colaExistente.setDataInicio(LocalDateTime.now());

            // Atualiza status, se enviado
            if (novaCola.getStatus() != null) {
                colaExistente.setStatus(novaCola.getStatus());
            }

            // Atualiza responsável
            Long userId = usuarioLogadoUtil.getUsuarioIdLogado();
            UsuarioModel usuario = usuarioRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
            colaExistente.setUsuario(usuario);

            ColaModel atualizado = colaRepository.save(colaExistente);

            return ResponseEntity.ok(Map.of("mensagem", "Cola atualizada com sucesso!", "dados", atualizado));

        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("mensagem", "Erro ao atualizar cola"));
        }
    }


    //
//    // DELETE - Excluir
//    @DeleteMapping(path = "/{id}", produces = "application/json; charset=UTF-8")
//    public ResponseEntity<Object> deletar(@PathVariable("id") Integer id) {
//        return repository.findById(id).map(cola -> {
//            repository.deleteById(id);
//            return ResponseEntity.ok().build();
//        }).orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body("Cola não encontrada"));
//    }
//
//    // GET - Buscar por produção
//    @GetMapping(path = "/cola/{colaId}", produces = "application/json; charset=UTF-8")
//    public ResponseEntity<Object> buscarPorCola(@PathVariable("colaId") Integer colaId) {
//        try {
//            Optional<ColaModel> cola = repository.findByColaId(colaId);
//            return cola
//                    .<ResponseEntity<Object>>map(ResponseEntity::ok)
//                    .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
//                            .body("Cola não encontrada para a produção"));
//        } catch (Exception ex) {
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                    .body(new ApiError(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao buscar por produção", ex, ex.getMessage()));
//        }
//    }
    @Operation(summary = "Buscar cola por etiqueta", description = "Busca cola ou produção por etiqueta para verificar disponibilidade para colagem")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cola ou produção encontrada"),
            @ApiResponse(responseCode = "404", description = "Etiqueta não encontrada"),
            @ApiResponse(responseCode = "409", description = "Já existe cobertura cadastrada para esse pneu"),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    @GetMapping(path = "/etiqueta/{etiqueta}", produces = "application/json; charset=UTF-8")
    public ResponseEntity<?> buscarPorEtiqueta(@Parameter(description = "Etiqueta da produção") @PathVariable("etiqueta") String etiqueta) {
        try {
            Optional<ColaModel> cola = colaRepository.findByEtiqueta(etiqueta);

            if (cola.isPresent()) {
                ColaModel colaModel = cola.get();

                // Verifica se já existe cobertura para essa produção
                if (coberturaRepository.existsByColaProducaoId(colaModel.getProducao().getId())) {
                    return ResponseEntity.status(HttpStatus.CONFLICT)
                            .body(Map.of("mensagem", "Já existe cobertura cadastrada para esse pneu.", "dados", colaModel));
                }

                return ResponseEntity.ok(Map.of("mensagem", "Cola encontrada com sucesso!", "dados", colaModel));
            } else {
                Optional<ProducaoModel> producao = producaoRepository.findByEtiqueta(etiqueta);

                if (producao.isPresent()) {
                    ProducaoModel producaoModel = producao.get();

                    // Verifica se já existe cobertura para essa produção
                    if (coberturaRepository.existsByColaProducaoId(producaoModel.getId())) {
                        return ResponseEntity.status(HttpStatus.CONFLICT)
                                .body(Map.of("mensagem", "Já existe cobertura cadastrada para esse pneu.", "dados", producaoModel));
                    }

                    return ResponseEntity.ok(Map.of("mensagem", "Produção encontrada, pronta para criar cola.", "dados", producaoModel));
                } else {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(Map.of("mensagem", "Etiqueta não encontrada."));
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("mensagem", "Erro ao buscar por etiqueta " + ex.getMessage()));
        }
    }



}
