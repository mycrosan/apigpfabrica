package br.compneusgppremium.api.controller;

import br.compneusgppremium.api.controller.dto.ClassificacaoCombinacaoDTO;
import br.compneusgppremium.api.controller.dto.LeituraLateralDTO;
import br.compneusgppremium.api.controller.form.LeituraLateralForm;
import br.compneusgppremium.api.controller.model.CarcacaModel;
import br.compneusgppremium.api.controller.model.MedidaModel;
import br.compneusgppremium.api.controller.model.StatusCarcacaModel;
import br.compneusgppremium.api.controller.model.UsuarioModel;
import br.compneusgppremium.api.repository.CarcacaRepository;
import br.compneusgppremium.api.repository.UsuarioRepository;
import br.compneusgppremium.api.service.CombinacaoPneuService;
import br.compneusgppremium.api.service.LeituraCarcacaService;
import br.compneusgppremium.api.util.ApiError;
import br.compneusgppremium.api.util.OperationSystem;
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

import java.io.File;
import java.nio.file.Files;
import java.util.Date;
import java.util.List;



import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.io.IOException;
import java.util.UUID;


@RestController
@Tag(name = "Carcaça", description = "Operações relacionadas ao gerenciamento de carcaças de pneus")
@SecurityRequirement(name = "Bearer Authentication")
public class CarcacaController {

    @Autowired
    private CarcacaRepository repository;

    @Autowired
    private UsuarioLogadoUtil usuarioLogadoUtil;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private LeituraCarcacaService leituraCarcacaService;

    @Autowired
    private CombinacaoPneuService combinacaoPneuService;

    @PersistenceContext
    EntityManager entityManager;

    @Operation(summary = "Listar carcaças", description = "Retorna as últimas 50 carcaças com status 'start' ou status_carcaca = 1")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de carcaças retornada com sucesso"),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    @GetMapping(path = "/api/carcaca")
    public Object findAll() {
        var sql = "SELECT c FROM carcaca c where c.status = 'start' or c.status_carcaca = 1 ORDER BY c.dt_create DESC";
        try {
            Query consulta = entityManager.createQuery(sql);
            return consulta.setMaxResults(50).getResultList();
        } catch (Exception e) {
            return e;
        }
    }

    @Operation(summary = "Consultar carcaça por ID", description = "Retorna uma carcaça específica pelo seu ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Carcaça encontrada"),
            @ApiResponse(responseCode = "404", description = "Carcaça não encontrada")
    })
    @GetMapping(path = "/api/carcaca/{id}")
    public ResponseEntity consultar(@Parameter(description = "ID da carcaça") @PathVariable("id") Integer id) {
        return repository.findById(id)
                .map(record -> ResponseEntity.ok().body(record))
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Atualizar carcaça", description = "Atualiza uma carcaça existente, verificando se não é uma carcaça rejeitada")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Carcaça atualizada com sucesso"),
            @ApiResponse(responseCode = "404", description = "Carcaça não encontrada"),
            @ApiResponse(responseCode = "400", description = "Carcaça proibída/rejeitada"),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    @PutMapping(produces = "application/json; charset=UTF-8", path = "/api/carcaca/{id}")
    public Object atualizar(@Parameter(description = "ID da carcaça") @PathVariable("id") Integer id,
            @RequestBody CarcacaModel carcaca,
            @Parameter(description = "Confirma explicitamente uma combinação modelo+medida+país nunca vista")
            @RequestParam(name = "confirmarCombinacaoNova", required = false, defaultValue = "false") Boolean confirmarCombinacaoNova) {

        ClassificacaoCombinacaoDTO classificacao = combinacaoPneuService.classificar(
                carcaca.getModelo() != null ? carcaca.getModelo().getId() : null,
                carcaca.getMedida() != null ? carcaca.getMedida().getId() : null,
                carcaca.getPais() != null ? carcaca.getPais().getId() : null);
        if ("VERMELHO".equals(classificacao.getClassificacao()) && !Boolean.TRUE.equals(confirmarCombinacaoNova)) {
            ApiError erroCombinacao = new ApiError(HttpStatus.OK, classificacao.getMensagem(), "COMBINACAO_NAO_RECONHECIDA");
            // editdatawidget.dart exibe value.debugMessage sem tratar null — preenche
            // igual ao message pra não quebrar a tela de edição nesse bloqueio.
            erroCombinacao.setDebugMessage(classificacao.getMensagem());
            return erroCombinacao;
        }

        var sql = "SELECT cr FROM carcaca_rejeitada cr where cr.modelo.id=" + carcaca.getModelo().getId() +
                " and cr.medida.id=" + carcaca.getMedida().getId() +
                " and cr.pais.id=" + carcaca.getPais().getId();

        try {
            Query consulta = entityManager.createQuery(sql);
            List values = consulta.getResultList();
            if (values.size() > 0) {
                throw new RuntimeException("Carcaca Proibída!");
            }
        } catch (Exception ex) {
            System.out.println(ex);
            ApiError apiError = new ApiError(HttpStatus.OK, ex.getMessage(), ex, ex.getCause() != null ? ex.getCause().toString() : "Erro");
            return apiError;
        }

        return repository.findById(id)
                .map(record -> {
                    record.setNumero_etiqueta(carcaca.getNumero_etiqueta());
                    record.setDot(carcaca.getDot());
                    record.setModelo(carcaca.getModelo());
                    record.setMedida(carcaca.getMedida());
                    record.setPais(carcaca.getPais());
                    CarcacaModel updated = repository.save(record);
                    return ResponseEntity.ok().body(updated);
                }).orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Criar nova carcaça", description = "Cria uma nova carcaça, verificando duplicatas de etiqueta e carcaças rejeitadas")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Carcaça criada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Etiqueta duplicada ou carcaça proibída"),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    @PostMapping(produces = "application/json; charset=UTF-8", path = "/api/carcaca")
    public Object salvar(@RequestBody CarcacaModel carcaca,
            @Parameter(description = "Confirma explicitamente uma combinação modelo+medida+país nunca vista")
            @RequestParam(name = "confirmarCombinacaoNova", required = false, defaultValue = "false") Boolean confirmarCombinacaoNova) {
        var statusCarcaca = new StatusCarcacaModel();
        statusCarcaca.setId(1);

        ClassificacaoCombinacaoDTO classificacao = combinacaoPneuService.classificar(
                carcaca.getModelo() != null ? carcaca.getModelo().getId() : null,
                carcaca.getMedida() != null ? carcaca.getMedida().getId() : null,
                carcaca.getPais() != null ? carcaca.getPais().getId() : null);
        if ("VERMELHO".equals(classificacao.getClassificacao()) && !Boolean.TRUE.equals(confirmarCombinacaoNova)) {
            ApiError erroCombinacao = new ApiError(HttpStatus.OK, classificacao.getMensagem(), "COMBINACAO_NAO_RECONHECIDA");
            // editdatawidget.dart exibe value.debugMessage sem tratar null — preenche
            // igual ao message pra não quebrar a tela de edição nesse bloqueio.
            erroCombinacao.setDebugMessage(classificacao.getMensagem());
            return erroCombinacao;
        }

        var sql = "SELECT cr FROM carcaca_rejeitada cr where cr.modelo.id=" + carcaca.getModelo().getId() +
                " and cr.medida.id=" + carcaca.getMedida().getId() +
                " and cr.pais.id=" + carcaca.getPais().getId();

        try {
            Query consulta = entityManager.createQuery(sql);
            List values = consulta.getResultList();
            if (values.size() > 0) {
                throw new RuntimeException("Carcaca Proibída!");
            }
        } catch (Exception ex) {
            System.out.println(ex);
            ApiError apiError = new ApiError(HttpStatus.OK, ex.getMessage(), ex, ex.getCause() != null ? ex.getCause().toString() : "Erro");
            return apiError;
        }

        try {
            List retornoConsulta = repository.findByEtiquetaDuplicate(carcaca.getNumero_etiqueta());

            if (retornoConsulta.size() > 0) {
                throw new RuntimeException("Etiqueta duplicada, favor inserir uma etiqueta diferente");
            }
            carcaca.setStatus("start");
            carcaca.setStatus_carcaca(statusCarcaca);
//            carcaca.setDados(carcaca.toString());
            carcaca.setDt_create(new Date());
            carcaca.setDt_update(new Date());
            carcaca.setUuid(UUID.randomUUID());

            Integer usuarioId = usuarioLogadoUtil.getUsuarioIdLogado();
            UsuarioModel usuario = usuarioRepository.findById(usuarioId)
                    .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
            carcaca.setCriadoPor(usuario);

            return repository.save(carcaca);
        } catch (Exception e) {
            return e;
        }

    }

    @Operation(summary = "Reconhecimento automático do pneu pela foto da lateral",
            description = "Recebe a foto da lateral do pneu em base64 e usa IA para reconhecer qual pneu (marca+modelo+medida) "
                    + "é aquele, comparando com os catálogos — não é uma leitura de texto campo a campo. Retorna de 1 a 4 "
                    + "candidatos ranqueados (1 só quando a IA tem certeza), além de DOT e país lidos à parte. "
                    + "O cadastro continua manual: o operador confere/escolhe e confirma.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Leitura realizada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Foto ausente ou inválida"),
            @ApiResponse(responseCode = "500", description = "Falha na leitura por IA")
    })
    @PostMapping(produces = "application/json; charset=UTF-8", path = "/api/carcaca/leitura-lateral")
    public Object leituraLateral(@RequestBody LeituraLateralForm form) {
        try {
            LeituraLateralDTO resultado = leituraCarcacaService.lerLateral(form.getFoto_base64());
            return ResponseEntity.ok().body(resultado);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return new ApiError(HttpStatus.BAD_REQUEST, ex.getMessage(), ex,
                    ex.getCause() != null ? ex.getCause().toString() : "Erro");
        } catch (Exception ex) {
            System.out.println(ex);
            return new ApiError(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Não foi possível ler a lateral do pneu", ex,
                    ex.getCause() != null ? ex.getCause().toString() : "Erro");
        }
    }

    @Operation(summary = "Classificar combinação modelo+medida+país",
            description = "Verifica se a fábrica já sabe produzir esse pneu antes de salvar a carcaça: "
                    + "VERDE (tem regra de produção), AMARELO (já cadastrado antes mas sem regra) ou "
                    + "VERMELHO (combinação nunca vista — precisa de confirmação explícita pra salvar).")
    @GetMapping(path = "/api/carcaca/classificar-combinacao")
    public ClassificacaoCombinacaoDTO classificarCombinacao(
            @Parameter(description = "ID do modelo") @RequestParam("modeloId") Integer modeloId,
            @Parameter(description = "ID da medida") @RequestParam("medidaId") Integer medidaId,
            @Parameter(description = "ID do país") @RequestParam("paisId") Integer paisId) {
        return combinacaoPneuService.classificar(modeloId, medidaId, paisId);
    }

    @Operation(summary = "Medidas plausíveis para um modelo",
            description = "Lista as medidas que já têm regra de produção ou histórico de cadastro pra esse "
                    + "modelo — usado pra filtrar o dropdown de medida no cadastro (cascata modelo→medida).")
    @GetMapping(path = "/api/carcaca/medidas-plausiveis")
    public List<MedidaModel> medidasPlausiveis(
            @Parameter(description = "ID do modelo") @RequestParam("modeloId") Integer modeloId) {
        return combinacaoPneuService.medidasPlausiveis(modeloId);
    }

    @Operation(summary = "Excluir carcaça", description = "Exclui uma carcaça pelo ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Carcaça excluída com sucesso"),
            @ApiResponse(responseCode = "404", description = "Carcaça não encontrada"),
            @ApiResponse(responseCode = "422", description = "Não foi possível excluir a carcaça")
    })
    @DeleteMapping(produces = "application/json; charset=UTF-8", path = "/api/carcaca/{id}")
    public Object delete(@Parameter(description = "ID da carcaça") @PathVariable("id") Integer id) {
        try {
            return repository.findById(id)
                    .map(record -> {
                        repository.deleteById(id);
                        return ResponseEntity.ok().build();
                    }).orElse(ResponseEntity.notFound().build());
        } catch (Exception ex) {
            System.out.println(ex);
            ApiError apiError = new ApiError(HttpStatus.UNPROCESSABLE_ENTITY, "Não foi possível excluir a carçaca " + id, ex, ex.getCause() != null ? ex.getCause().getCause().getMessage() : "Erro");
            return apiError;
        }
    }

    @GetMapping(produces = "application/json; charset=UTF-8", path = "/api/carcaca/pesquisa/{etiqueta}")
    public Object consultarPneu(@PathVariable("etiqueta") String etiqueta) {
        try {
            var retornoConsulta = repository.findByEtiqueta(etiqueta);
            if (retornoConsulta.size() > 1) {
                throw new RuntimeException("O sistema encontrou mais de uma carcaca com a mesma etiqueta");
            } else if (retornoConsulta.size() == 1) {
                return retornoConsulta.get(0);
            }
            throw new RuntimeException("Carcaça etiqueta " + etiqueta + " não cadastrada");
        } catch (Exception ex) {
            ApiError apiError = new ApiError(HttpStatus.EXPECTATION_FAILED, "Não foi encontrado resultado para etiqueta " + etiqueta, ex, ex.getCause() != null ? ex.getCause().getCause().getMessage() : "Erro");
            return apiError;
        }
    }

    @Operation(summary = "Exibir imagem", description = "Retorna uma imagem do sistema de arquivos")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Imagem retornada com sucesso"),
            @ApiResponse(responseCode = "404", description = "Imagem não encontrada"),
            @ApiResponse(responseCode = "500", description = "Erro ao ler a imagem")
    })
    @GetMapping(path = "/api/image/{caminho}/{idImg}")
    @ResponseBody
    public byte[] exibirImagem(@Parameter(description = "Caminho da imagem") @PathVariable("caminho") String caminho, 
                              @Parameter(description = "ID/nome da imagem") @PathVariable("idImg") String idImg) throws IOException {
        String caminhoImagem = new OperationSystem().placeImageSystem(caminho);
        File imagemArquivo = new File(caminhoImagem + idImg);
        if (idImg != null || idImg.trim().length() > 0) {
            System.out.println("No if");
            return Files.readAllBytes(imagemArquivo.toPath());
        }
        return null;
    }

//    private Object checkReject(CarcacaModel carcaca){
//
//        var sql = "SELECT cr FROM carcaca_rejeitada cr where cr.modelo.id=" + carcaca.getModelo().getId() +
//                " and cr.medida.id=" + carcaca.getMedida().getId() +
//                " and cr.pais.id=" + carcaca.getPais().getId();
//
//        try {
//            Query consulta = entityManager.createQuery(sql);
//            List values = consulta.getResultList();
//            if (values.size() > 0) {
//                throw new RuntimeException("Carcaca Proibída!");
//            }
//        } catch (Exception ex) {
//            System.out.println(ex);
//            ApiError apiError = new ApiError(HttpStatus.OK, ex.getMessage(), ex, ex.getCause() != null ? ex.getCause().toString(): "Erro");
//            return apiError;
//        }
//        return false;
//    }
}
