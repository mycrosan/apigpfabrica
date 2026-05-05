package br.compneusgppremium.api.service;

import br.compneusgppremium.api.controller.dto.ProducaoFilterDTO;
import br.compneusgppremium.api.controller.model.ProducaoModel;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProducaoPesquisaService
{
    @PersistenceContext
    private static EntityManager entityManager;

    public static List<ProducaoModel> consultarProducoes(ProducaoFilterDTO filtro) {
        StringBuilder sql = new StringBuilder("SELECT pro FROM producao pro WHERE 1=1");
        Map<String, Object> params = new HashMap<>();

        if (filtro.getModeloId() != null) {
            sql.append(" AND pro.carcaca.modelo.id = :modeloId");
            params.put("modeloId", filtro.getModeloId());
        }

        if (filtro.getMarcaId() != null) {
            sql.append(" AND pro.carcaca.modelo.marca.id = :marcaId");
            params.put("marcaId", filtro.getMarcaId());
        }

        if (filtro.getMedidaId() != null) {
            sql.append(" AND pro.carcaca.medida.id = :medidaId");
            params.put("medidaId", filtro.getMedidaId());
        }

        if (filtro.getPaisId() != null) {
            sql.append(" AND pro.carcaca.pais.id = :paisId");
            params.put("paisId", filtro.getPaisId());
        }

        if (filtro.getNumeroEtiqueta() != null && !filtro.getNumeroEtiqueta().isEmpty()) {
            sql.append(" AND pro.carcaca.numero_etiqueta = :numeroEtiqueta");
            params.put("numeroEtiqueta", filtro.getNumeroEtiqueta());
        }

        sql.append(" ORDER BY pro.dt_create ASC");

        TypedQuery<ProducaoModel> query = entityManager.createQuery(sql.toString(), ProducaoModel.class);
        params.forEach(query::setParameter);

        return query.getResultList();
    }
}
