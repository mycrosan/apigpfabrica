package br.compneusgppremium.api.controller;


import br.compneusgppremium.api.controller.model.TipoObservacaoModel;
import br.compneusgppremium.api.repository.TipoObservacaoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
public class TipoObservacaoController {

    @Autowired
    private TipoObservacaoRepository repository;

    @PersistenceContext
    EntityManager entityManager;

    @GetMapping(path = "/api/tipoobservacao")
    public List<TipoObservacaoModel> findAll() {
        var it = repository.findAll();
        var values = new ArrayList<TipoObservacaoModel>();
        it.forEach(e -> values.add(e));
        return values;
    }

    @GetMapping(path = "/api/tipoobservacao/{id}")
    public ResponseEntity consultar(@PathVariable("id") Integer id) {
        return repository.findById(id)
                .map(record -> ResponseEntity.ok().body(record))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping(path = "/api/tipoobservacao")
    public Object salvar(@RequestBody TipoObservacaoModel value) {
        try {
            return repository.save(value);
        } catch (Exception ex) {
            return ex;
        }
    }

    @PutMapping(path = "/api/tipoobservacao/{id}")
    public ResponseEntity atualizar(@PathVariable("id") Integer id, @RequestBody TipoObservacaoModel value) {
        return repository.findById(id)
                .map(record -> {
                    record.setDescricao(value.getDescricao());
                    TipoObservacaoModel updated = repository.save(record);
                    return ResponseEntity.ok().body(updated);
                }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping(path = "/api/tipoobservacao/{id}")
    public ResponseEntity<?> delete(@PathVariable("id") Integer id) {
        return repository.findById(id)
                .map(record -> {
                    repository.deleteById(id);
                    return ResponseEntity.ok().build();
                }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping(produces = "application/json; charset=UTF-8", path = "/api/tipoobservacao/pesquisa")
    public Object consultarProducao(@RequestParam Map<Integer, String> params) {
// Iniciando a consulta
        var sql = "SELECT to FROM tipo_observacao to where to.tipo_classificacao =" + params.get("tipoClassificacaoId");;
// Montando a consulta

//        String modeloId = params.get("modeloId").equals("null") ? "null" : params.get("modeloId");
//        if (!modeloId.equals("null"))
//            sql = sql + " and pro.carcaca.modelo.id =" + modeloId;
//
//        String marcaId = params.get("marcaId").equals("null") ? "null" : params.get("marcaId");
//        if (!marcaId.equals("null"))
//            sql = sql + " and pro.carcaca.modelo.marca.id = " + marcaId;
//
//        String medidaId = params.get("medidaId").equals("null") ? "null" : params.get("medidaId");
//        if (!medidaId.equals("null"))
//            sql = sql + " and pro.carcaca.medida.id = " + medidaId;
//
//        String paisId = params.get("paisId").equals("null") ? "null" : params.get("paisId");
//        if (!paisId.equals("null"))
//            sql = sql + " and pro.carcaca.pais.id = " + paisId;
//
//        String numeroEtiqueta = params.get("numeroEtiqueta").equals("null") ? "null" : params.get("numeroEtiqueta");
//        if (!numeroEtiqueta.equals("null"))
//            sql = sql + " and pro.carcaca.numero_etiqueta = " + "'" + numeroEtiqueta + "'";
////
//        sql = sql + " ORDER BY pro.dt_create ASC";

        try {
            Query consulta = entityManager.createQuery(sql);
            return consulta.setMaxResults(50).getResultList();
        } catch (Exception e) {
            return e;
        }

    }

}