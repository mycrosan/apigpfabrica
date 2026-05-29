package br.compneusgppremium.api.controller;

import br.compneusgppremium.api.controller.dto.ProducaoDTO;
import br.compneusgppremium.api.controller.model.*;
import br.compneusgppremium.api.repository.CarcacaRepository;
import br.compneusgppremium.api.repository.ProducaoRepository;
import br.compneusgppremium.api.repository.RegraRepository;
import br.compneusgppremium.api.repository.UsuarioRepository;
import br.compneusgppremium.api.util.ApiError;
import br.compneusgppremium.api.util.UsuarioLogadoUtil;
import br.compneusgppremium.api.controller.enums.RegraStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.util.*;

import static org.apache.logging.log4j.util.Strings.isNotBlank;

@RestController
@Tag(name = "Produção", description = "Operações relacionadas ao controle de produção")
@SecurityRequirement(name = "Bearer Authentication")
public class ProducaoController {

    @Autowired
    private ProducaoRepository producaoRepository;

    @Autowired
    private CarcacaRepository carcacaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private UsuarioLogadoUtil usuarioLogadoUtil;

    @Autowired
    private RegraRepository regraRepository;

    @PersistenceContext
    EntityManager entityManager;

    @GetMapping(path = "/api/producao")
    @Operation(
        summary = "Listar produções",
        description = "Retorna uma lista das últimas 100 produções ordenadas por data de criação"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Lista de produções retornada com sucesso",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ProducaoDTO.class)
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Token de autenticação inválido ou expirado"
        )
    })
    public List<ProducaoDTO> findAll() {
        var sql = "SELECT p FROM producao p ORDER BY p.dt_create DESC";
        List<ProducaoModel> producoes = entityManager.createQuery(sql, ProducaoModel.class)
                .setMaxResults(100)
                .getResultList();

        List<ProducaoDTO> dtoList = new ArrayList<>();
        for (ProducaoModel p : producoes) {
            dtoList.add(new ProducaoDTO(
                    p.getId(),
                    p.getCarcaca(),
                    p.getMedida_pneu_raspado(),
                    p.getRegra(),
                    p.getFotos(),
                    p.getDt_create(),
                    p.getCriadoPor() != null ? p.getCriadoPor().getNome() : null,
                    p.getCriadoPor() != null ? p.getCriadoPor().getLogin() : null
            ));
        }

        return dtoList;
    }

    @GetMapping(path = "/api/producao/{id}")
    @Operation(
        summary = "Consultar produção por ID",
        description = "Retorna os detalhes de uma produção específica pelo seu ID"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Produção encontrada",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ProducaoDTO.class)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Produção não encontrada"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Token de autenticação inválido ou expirado"
        )
    })
    public ResponseEntity<?> consultar(
        @Parameter(description = "ID da produção", required = true, example = "1")
        @PathVariable("id") Integer id
    ) {
        return producaoRepository.findById(id)
                .map(record -> {
                    ProducaoModel p = record;
                    ProducaoDTO dto = new ProducaoDTO(
                            p.getId(),
                            p.getCarcaca(),
                            p.getMedida_pneu_raspado(),
                            p.getRegra(),
                            p.getFotos(),
                            p.getDt_create(),
                            p.getCriadoPor() != null ? p.getCriadoPor().getNome() : null,
                            p.getCriadoPor() != null ? p.getCriadoPor().getLogin() : null
                    );
                    return ResponseEntity.ok().body(dto);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping(produces = "application/json; charset=UTF-8", path = "/api/producao")
    @Operation(
        summary = "Criar nova produção",
        description = "Cria uma nova produção e atualiza o status da carcaça para 'in_production'"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Produção criada com sucesso",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ProducaoModel.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Dados inválidos ou carcaça já produzida",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiError.class)
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Token de autenticação inválido ou expirado"
        )
    })
    public Object salvar(
        @Parameter(description = "Dados da produção", required = true)
        @RequestBody ProducaoModel producao
    ) {
        var statusCarcaca = new StatusCarcacaModel();
        statusCarcaca.setId(2);

        var sql = "SELECT p FROM producao p where p.carcaca.id=" + producao.getCarcaca().getId();

        try {
            Query consulta = entityManager.createQuery(sql);
            List values = consulta.getResultList();
            if (!values.isEmpty()) {
                throw new RuntimeException("Já produzido!");
            }

            // Garantir status da regra e aplicar restrição: permitir apenas a primeira produção quando a regra está EM_VALIDACAO
            if (producao.getRegra() != null && producao.getRegra().getId() != null) {
                RegraModel regraDb = regraRepository.findById(producao.getRegra().getId())
                        .orElseThrow(() -> new RuntimeException("Regra não encontrada"));

                // Se o status da regra estiver null no momento da primeira produção, definir como EM_VALIDACAO
                if (regraDb.getStatus() == null) {
                    regraDb.setStatus(RegraStatus.EM_VALIDACAO);
                    regraDb.setDt_update(new Date());
                    regraDb = regraRepository.save(regraDb);
                }

                long producoesComRegra = producaoRepository.countByRegraId(regraDb.getId());
                if (regraDb.getStatus() == RegraStatus.EM_VALIDACAO && producoesComRegra >= 1) {
                    // Buscar a primeira produção da regra que precisa ser qualificada
                    List<ProducaoModel> producoesDaRegra = producaoRepository.findFirstByRegraIdOrderByDtCreateAsc(regraDb.getId());
                    String numeroEtiqueta = "etiqueta não encontrada";
                    
                    if (!producoesDaRegra.isEmpty()) {
                        ProducaoModel primeiraProducao = producoesDaRegra.get(0);
                        numeroEtiqueta = carcacaRepository.findById(primeiraProducao.getCarcaca().getId())
                                .map(carcaca -> carcaca.getNumero_etiqueta())
                                .orElse("etiqueta não encontrada");
                    }
                    
                    ApiError apiError = new ApiError(
                            HttpStatus.PRECONDITION_REQUIRED,
                            "Regra em validação: crie a primeira produção e aguarde aprovação na qualidade para liberar novas produções. Etiqueta que precisa ser qualificada: " + numeroEtiqueta,
                            null,
                            "Regra EM_VALIDACAO já possui uma produção"
                    );
                    return apiError;
                }

                // Vincular a regra carregada do banco para não confiar no payload recebido
                producao.setRegra(regraDb);
            }

            return carcacaRepository.findById(producao.getCarcaca().getId())
                    .map(record -> {
                        record.setStatus("in_production");
                        record.setStatus_carcaca(statusCarcaca);
                        carcacaRepository.save(record);

                        // Persistir JSON válido e curto para o campo 'dados' evitando estouro de coluna em H2
                        producao.setDados("{}");
                        producao.setDt_create(new Date());
                        producao.setDt_update(new Date());
                        producao.setUuid(UUID.randomUUID());

                        Integer userId = usuarioLogadoUtil.getUsuarioIdLogado();
                        UsuarioModel usuario = usuarioRepository.findById(userId)
                                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
                        producao.setCriadoPor(usuario);

                        return producaoRepository.save(producao);
                    }).orElseThrow(() -> new RuntimeException("Carcaça não encontrada"));

        } catch (Exception ex) {
            ex.printStackTrace();
            ApiError apiError = new ApiError(HttpStatus.OK, ex.getMessage(), ex,
                    ex.getCause() != null ? ex.getCause().toString() : "Erro");
            return apiError;
        }
    }

    @PutMapping(path = "/api/producao/{id}")
    @Operation(
        summary = "Atualizar produção",
        description = "Atualiza os dados de uma produção existente"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Produção atualizada com sucesso",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ProducaoModel.class)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Produção não encontrada"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Token de autenticação inválido ou expirado"
        )
    })
    public ResponseEntity<?> atualizar(
        @Parameter(description = "ID da produção", required = true, example = "1")
        @PathVariable("id") Integer id,
        @Parameter(description = "Dados atualizados da produção", required = true)
        @RequestBody ProducaoModel producao
    ) {
        return producaoRepository.findById(id)
                .map(record -> {
                    record.setCarcaca(producao.getCarcaca());
                    record.setMedida_pneu_raspado(producao.getMedida_pneu_raspado());
                    record.setRegra(producao.getRegra());
                    ProducaoModel updated = producaoRepository.save(record);
                    return ResponseEntity.ok().body(updated);
                }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping(path = "/api/producao/{id}")
    @Operation(
        summary = "Excluir produção",
        description = "Exclui uma produção e retorna o status da carcaça para 'start'"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Produção excluída com sucesso"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Produção não encontrada"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Token de autenticação inválido ou expirado"
        )
    })
    public ResponseEntity<?> delete(
        @Parameter(description = "ID da produção", required = true, example = "1")
        @PathVariable("id") Integer id
    ) {
        return producaoRepository.findById(id)
                .map(record -> {
                    carcacaRepository.findById(record.getCarcaca().getId())
                            .map(record2 -> {
                                record2.setStatus("start");
                                carcacaRepository.save(record2);
                                return null;
                            });
                    producaoRepository.deleteById(id);
                    return ResponseEntity.ok().build();
                }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping(path = "/api/producao/pesquisa")
    @Operation(
        summary = "Pesquisar produções",
        description = "Pesquisa produções com base em filtros opcionais como modelo, marca, medida, país e número da etiqueta"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Pesquisa realizada com sucesso",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ProducaoDTO.class)
            )
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Erro interno do servidor"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Token de autenticação inválido ou expirado"
        )
    })
    public ResponseEntity<?> consultarProducao(
        @Parameter(description = "ID do modelo", example = "1")
        @RequestParam(required = false) String modeloId,
        @Parameter(description = "ID da marca", example = "1")
        @RequestParam(required = false) String marcaId,
        @Parameter(description = "ID da medida", example = "1")
        @RequestParam(required = false) String medidaId,
        @Parameter(description = "ID do país", example = "1")
        @RequestParam(required = false) String paisId,
        @Parameter(description = "Número da etiqueta", example = "ET001")
        @RequestParam(required = false) String numeroEtiqueta
    ) {

        StringBuilder sql = new StringBuilder("SELECT pro FROM producao pro WHERE 1 = 1");
        Map<String, Object> parametros = new HashMap<>();

        Integer modeloIdInt = parseIntegerOrNull(modeloId);
        Integer marcaIdInt = parseIntegerOrNull(marcaId);
        Integer medidaIdInt = parseIntegerOrNull(medidaId);
        Integer paisIdInt = parseIntegerOrNull(paisId);

        if (modeloIdInt != null) {
            sql.append(" AND pro.carcaca.modelo.id = :modeloId");
            parametros.put("modeloId", modeloIdInt);
        }
        if (marcaIdInt != null) {
            sql.append(" AND pro.carcaca.modelo.marca.id = :marcaId");
            parametros.put("marcaId", marcaIdInt);
        }
        if (medidaIdInt != null) {
            sql.append(" AND pro.carcaca.medida.id = :medidaId");
            parametros.put("medidaId", medidaIdInt);
        }
        if (paisIdInt != null) {
            sql.append(" AND pro.carcaca.pais.id = :paisId");
            parametros.put("paisId", paisIdInt);
        }

        if (numeroEtiqueta != null && !numeroEtiqueta.isBlank() && !"null".equalsIgnoreCase(numeroEtiqueta.trim())) {
            sql.append(" AND pro.carcaca.numero_etiqueta = :numeroEtiqueta");
            parametros.put("numeroEtiqueta", numeroEtiqueta.trim());
        }

        sql.append(" ORDER BY pro.dt_create ASC");

        try {
            Query query = entityManager.createQuery(sql.toString());

            for (Map.Entry<String, Object> entry : parametros.entrySet()) {
                query.setParameter(entry.getKey(), entry.getValue());
            }

            @SuppressWarnings("unchecked")
            List<ProducaoModel> producoes = query.getResultList();

            List<ProducaoDTO> dtos = new ArrayList<>();
            for (ProducaoModel p : producoes) {
                dtos.add(new ProducaoDTO(
                        p.getId(),
                        p.getCarcaca(),
                        p.getMedida_pneu_raspado(),
                        p.getRegra(),
                        p.getFotos(),
                        p.getDt_create(),
                        p.getCriadoPor() != null ? p.getCriadoPor().getNome() : null,
                        p.getCriadoPor() != null ? p.getCriadoPor().getLogin() : null
                ));
            }

            return ResponseEntity.ok(dtos);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro ao consultar produção: " + e.getMessage());
        }
    }


    private Integer parseIntegerOrNull(String valor) {
        if (valor == null || valor.isBlank() || valor.equalsIgnoreCase("null")) {
            return null;
        }
        try {
            return Integer.parseInt(valor);
        } catch (NumberFormatException e) {
            return null;
        }
    }

}
