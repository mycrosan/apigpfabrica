package br.compneusgppremium.api.controller;

import br.compneusgppremium.api.controller.model.CarcacaModel;
import br.compneusgppremium.api.controller.model.RegraModel;
import br.compneusgppremium.api.repository.RegraRepository;
import br.compneusgppremium.api.util.ApiError;
import br.compneusgppremium.api.controller.enums.RegraStatus;
import net.bytebuddy.implementation.bytecode.Throw;
import org.hibernate.engine.jdbc.Size;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.util.*;


@RestController
public class RegraController {

    @Autowired
    private RegraRepository repository;

    @PersistenceContext
    EntityManager entityManager;

    @GetMapping(path = "/api/regra")
    public List<RegraModel> findAll() {
        var it = repository.findAll();
        var pneus = new ArrayList<RegraModel>();
        it.forEach(e -> pneus.add(e));
        return pneus;
    }

    @GetMapping(path = "/api/regra/{id}")
    public ResponseEntity consultar(@PathVariable("id") Integer id) {
        return repository.findById(id)
                .map(record -> ResponseEntity.ok().body(record))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping(path = "/api/regra")
    public Object salvar(@RequestBody RegraModel regra) {

        try {
            var retornoConsulta = repository.findByRange(regra.getMatriz().getId(), regra.getMedida().getId(), regra.getModelo().getId(), regra.getPais().getId(), regra.getTamanho_min(), regra.getTamanho_max());
            if (retornoConsulta.size() > 0) {
                throw new RuntimeException("O sistema encontrou uma regra para os parâmetros enviados, revise as regras cadastradas");
            }
            regra.setDt_create(new Date());
            regra.setStatus(RegraStatus.EM_VALIDACAO);
            return repository.save(regra);
        } catch (Exception ex) {
            System.out.println(ex);
            ApiError apiError = new ApiError(HttpStatus.EXPECTATION_FAILED, "Regra duplicada", ex, ex.getCause() != null ? ex.getCause().getCause().getMessage() : "Erro");
            return apiError;
        }
    }

    @PutMapping(path = "/api/regra/{id}")
    public ResponseEntity atualizar(@PathVariable("id") Integer id, @RequestBody RegraModel regra) {
        return repository.findById(id)
                .map(record -> {
                    record.setTamanho_min(regra.getTamanho_min());
                    record.setTamanho_max(regra.getTamanho_max());
                    record.setAntiquebra1(regra.getAntiquebra1());
                    record.setAntiquebra2(regra.getAntiquebra2());
                    record.setAntiquebra3(regra.getAntiquebra3());
                    record.setEspessuramento(regra.getEspessuramento());
                    record.setTempo(regra.getTempo());
                    record.setCamelback(regra.getCamelback());
                    record.setMatriz(regra.getMatriz());
                    record.setMedida(regra.getMedida());
                    record.setModelo(regra.getModelo());
                    record.setPais(regra.getPais());
                    // Ao editar a regra, ela volta para EM_VALIDACAO
                    record.setStatus(RegraStatus.EM_VALIDACAO);
                    record.setDt_update(new Date());
                    RegraModel updated = repository.save(record);
                    return ResponseEntity.ok().body(updated);
                }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping(path = "/api/regra/pesquisa/{matriz}/{medida}/{modelo}/{pais}/{medidaPneuRaspado}")
    public Object consultarRegra(@PathVariable("matriz") Integer matriz, @PathVariable("medida") Integer medida, @PathVariable("modelo") Integer modelo, @PathVariable("pais") Integer pais, @PathVariable("medidaPneuRaspado") Double medidaPneuRaspado) {
        try {
            var retornoConsulta = repository.findRule(matriz, medida, modelo, pais, medidaPneuRaspado);
            if (retornoConsulta.size() > 1) {
                throw new RuntimeException("O sistema encontrou mais de uma regra para os parâmetros enviados, revise as regras cadastradas");
            } else if (retornoConsulta.size() == 1) {
                return retornoConsulta.get(0);
            }
            throw new RuntimeException("Nenhuma regra encontrada!");
        } catch (Exception ex) {
            ApiError apiError = new ApiError(HttpStatus.CONFLICT, "O sistema encontrou um erro!", ex, ex.getCause() != null ? ex.getCause().getCause().getMessage() : "Erro");
            return apiError;
        }
    }

    @DeleteMapping(path = "/api/regra/{id}")
    public ResponseEntity<?> delete(@PathVariable("id") Integer id) {
        return repository.findById(id)
                .map(record -> {
                    repository.deleteById(id);
                    return ResponseEntity.ok().build();
                }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping(path = "/api/regra/consulta")
    public Object consultarProducao(@RequestParam Map<Integer, String> params) {
// Iniciando a consulta
        var sql = "SELECT r FROM regra r where 1 = 1";
// Montando a consulta

        String modeloId = params.get("modeloId").equals("null") ? "null" : params.get("modeloId");
        if (!modeloId.equals("null"))
            sql = sql + " and r.modelo.id = " + modeloId;

        String marcaId = params.get("marcaId").equals("null") ? "null" : params.get("marcaId");
        if (!marcaId.equals("null"))
            sql = sql + " and r.modelo.marca.id = " + marcaId;

        String medidaId = params.get("medidaId").equals("null") ? "null" : params.get("medidaId");
        if (!medidaId.equals("null"))
            sql = sql + " and r.medida.id = " + medidaId;
//
        String paisId = params.get("paisId").equals("null") ? "null" : params.get("paisId");
        if (!paisId.equals("null"))
            sql = sql + " and r.pais.id = " + paisId;
//
        String numeroRegra = params.get("numeroRegra").equals("null") ? "null" : params.get("numeroRegra");
        if (!numeroRegra.equals("null"))
            sql = sql + " and r.id = " + "'" + numeroRegra + "'";
//
        sql = sql + " ORDER BY r.dt_create ASC";

        try {
            Query consulta = entityManager.createQuery(sql);
            return consulta.setMaxResults(50).getResultList();
        } catch (Exception e) {
            return e;
        }

    }


}
