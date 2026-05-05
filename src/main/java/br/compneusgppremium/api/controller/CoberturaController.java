package br.compneusgppremium.api.controller;

import br.compneusgppremium.api.controller.dto.ColaComStatusDTO;
import br.compneusgppremium.api.controller.model.CoberturaModel;
import br.compneusgppremium.api.controller.model.ColaModel;
import br.compneusgppremium.api.controller.model.ProducaoModel;
import br.compneusgppremium.api.controller.model.UsuarioModel;
import br.compneusgppremium.api.repository.CoberturaRepository;
import br.compneusgppremium.api.repository.ColaRepository;
import br.compneusgppremium.api.repository.ProducaoRepository;
import br.compneusgppremium.api.repository.UsuarioRepository;
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

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/cobertura")
@Tag(name = "Cobertura", description = "Operações relacionadas ao controle de cobertura de pneus")
@SecurityRequirement(name = "Bearer Authentication")
public class CoberturaController {

    @Autowired
    private CoberturaRepository repository;

    @Autowired
    private ProducaoRepository producaoRepository;

    @Autowired
    private ColaRepository colaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private UsuarioLogadoUtil usuarioLogadoUtil;

    @Operation(summary = "Criar nova cobertura", description = "Cria uma nova cobertura para uma cola específica")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Cobertura criada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos ou cola vencida"),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    @PostMapping(produces = "application/json; charset=UTF-8")
    public ResponseEntity<?> salvar(@RequestBody CoberturaModel cobertura) {
        try {
            if (cobertura.getCola() == null || cobertura.getCola().getId() == null) {
                return ResponseEntity.badRequest().body("Cola não informada.");
            }

            // Busca a cola vinculada
            ColaModel cola = colaRepository.findById(cobertura.getCola().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Cola não encontrada."));

            ProducaoModel producao = cola.getProducao();

            // Verifica se já existe cobertura para esta cola
            if (repository.findByColaId(cola.getId()).isPresent()) {
                return ResponseEntity.badRequest().body("Esta carcaça já possui cobertura cadastrada.");
            }

            // Verifica status e validade da cola
            LocalDateTime inicio = cola.getDataInicio();
            if (cola.getStatus() == ColaModel.StatusCola.Vencido ||
                    inicio == null ||
                    inicio.plusMinutes(120).isBefore(LocalDateTime.now())) {
                return ResponseEntity.badRequest().body("Cola está vencida ou inválida para cobertura.");
            }

            // Define usuário logado como responsável
            Long userId = usuarioLogadoUtil.getUsuarioIdLogado();
            UsuarioModel usuario = usuarioRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

            cobertura.setCola(cola);
            cobertura.setUsuario(usuario);

            CoberturaModel saved = repository.save(cobertura);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);

        } catch (Exception ex) {
            ex.printStackTrace();
            ApiError apiError = new ApiError(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erro ao salvar cobertura.",
                    ex,
                    ex.getCause() != null ? ex.getCause().toString() : "Erro interno"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(apiError);
        }
    }

    @Operation(summary = "Listar colas sem cobertura", description = "Retorna todas as colas que ainda não possuem cobertura")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de colas sem cobertura retornada com sucesso"),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    @GetMapping(produces = "application/json; charset=UTF-8")
    public ResponseEntity<?> listar() {
        try {
            List<ColaModel> colas = colaRepository.findColasSemCobertura();
            return ResponseEntity.ok(colas);
        } catch (Exception ex) {
            ApiError apiError = new ApiError(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erro ao listar colas sem cobertura", ex, ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(apiError);
        }
    }

    @Operation(summary = "Buscar cobertura por ID", description = "Retorna uma cobertura específica pelo seu ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cobertura encontrada"),
            @ApiResponse(responseCode = "404", description = "Cobertura não encontrada")
    })
    @GetMapping(path = "/{id}", produces = "application/json; charset=UTF-8")
    public ResponseEntity<Object> buscarPorId(@Parameter(description = "ID da cobertura") @PathVariable("id") Integer id) {
        Optional<CoberturaModel> cobertura = repository.findById(id);
        return cobertura.<ResponseEntity<Object>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body("Cobertura não encontrada"));
    }

    @Operation(summary = "Atualizar cobertura", description = "Atualiza uma cobertura existente")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cobertura atualizada com sucesso"),
            @ApiResponse(responseCode = "404", description = "Cobertura não encontrada")
    })
    @PutMapping(path = "/{id}", produces = "application/json; charset=UTF-8")
    public ResponseEntity<Object> atualizar(@Parameter(description = "ID da cobertura") @PathVariable("id") Integer id, @RequestBody CoberturaModel novaCobertura) {
        return repository.findById(id)
                .<ResponseEntity<Object>>map(coberturaExistente -> {
                    coberturaExistente.setFotos(novaCobertura.getFotos());
                    coberturaExistente.setCola(novaCobertura.getCola());
                    return ResponseEntity.ok(repository.save(coberturaExistente));
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body("Cobertura não encontrada"));
    }

    @Operation(summary = "Deletar cobertura", description = "Remove uma cobertura do sistema")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cobertura deletada com sucesso"),
            @ApiResponse(responseCode = "404", description = "Cobertura não encontrada")
    })
    @DeleteMapping(path = "/{id}", produces = "application/json; charset=UTF-8")
    public ResponseEntity<Object> deletar(@Parameter(description = "ID da cobertura") @PathVariable("id") Integer id) {
        return repository.findById(id).map(cobertura -> {
            repository.deleteById(id);
            return ResponseEntity.ok().build();
        }).orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body("Cobertura não encontrada"));
    }

    @Operation(summary = "Buscar cobertura por cola", description = "Retorna a cobertura associada a uma cola específica")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cobertura encontrada"),
            @ApiResponse(responseCode = "404", description = "Cobertura não encontrada para esta cola"),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    @GetMapping(path = "/cola/{colaId}", produces = "application/json; charset=UTF-8")
    public ResponseEntity<Object> buscarPorCola(@Parameter(description = "ID da cola") @PathVariable("colaId") Integer colaId) {
        try {
            Optional<CoberturaModel> cobertura = repository.findByColaId(colaId);
            return cobertura.<ResponseEntity<Object>>map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body("Cobertura não encontrada para esta cola"));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiError(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao buscar por cola", ex, ex.getMessage()));
        }
    }

    @Operation(summary = "Buscar cobertura por etiqueta", description = "Busca informações de cobertura e validação de cola por etiqueta da produção")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Informações da cobertura retornadas com sucesso"),
            @ApiResponse(responseCode = "404", description = "Produção não encontrada para a etiqueta"),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    @GetMapping(path = "/etiqueta/{etiqueta}", produces = "application/json; charset=UTF-8")
    public ResponseEntity<Object> buscarPorEtiqueta(@Parameter(description = "Etiqueta da produção") @PathVariable("etiqueta") String etiqueta) {
        try {
            Optional<ProducaoModel> producaoOpt = producaoRepository.findByEtiqueta(etiqueta);
            if (producaoOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Nenhuma produção encontrada para a etiqueta");
            }

            ProducaoModel producao = producaoOpt.get();
            Optional<ColaModel> colaOpt = colaRepository.findByProducao(producao);
            ColaModel cola = colaOpt.orElse(null);
            CoberturaModel coberturaExistente = null;

            boolean colaValida = false;
            String mensagem = "Cola não encontrada";

            if (cola != null) {
                // Verifica se já existe cobertura vinculada a essa cola
                Optional<CoberturaModel> coberturaOpt = repository.findByColaId(cola.getId());
                if (coberturaOpt.isPresent()) {
                    coberturaExistente = coberturaOpt.get();
                    mensagem = "Cobertura já cadastrada para esta carcaça.";
                    colaValida = false;
                } else {
                    LocalDateTime inicio = cola.getDataInicio();

                    if (inicio != null) {
                        long minutosDecorridos = java.time.Duration.between(inicio, LocalDateTime.now()).toMinutes();

                        if ((cola.getStatus() == ColaModel.StatusCola.Aguardando
                                || cola.getStatus() == ColaModel.StatusCola.Pronto)
                                && minutosDecorridos >= 20
                                && minutosDecorridos <= 120) {
                            colaValida = true;
                            mensagem = "Cola válida para cobertura";
                        } else {
                            colaValida = false;
                            if (minutosDecorridos < 20) {
                                mensagem = "Cola inválida aguarde atingir os 20 minutos (Tempo decorrido " + minutosDecorridos + " min).";
                            } else if (minutosDecorridos > 120) {
                                mensagem = "Cola inválida, ultrapassou os de 120 minutos (Tempo decorrido " + minutosDecorridos + " min) favor colar novamente.";
                            } else {
                                mensagem = "Cola inválida para cobertura";
                            }
                        }
                    } else {
                        mensagem = "Data de início da cola não informada";
                    }
                }
            }

            UsuarioModel usuario;
            if (coberturaExistente != null) {
                usuario = coberturaExistente.getUsuario();
            } else {
                Long userId = usuarioLogadoUtil.getUsuarioIdLogado();
                usuario = usuarioRepository.findById(userId)
                        .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
            }

            ColaComStatusDTO dto = new ColaComStatusDTO(cola, producao, colaValida, mensagem, usuario);
            dto.setCobertura(coberturaExistente);

            return ResponseEntity.ok(dto);

        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiError(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao buscar por etiqueta", ex, ex.getMessage()));
        }
    }

}
