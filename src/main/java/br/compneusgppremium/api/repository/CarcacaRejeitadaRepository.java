package br.compneusgppremium.api.repository;

import br.compneusgppremium.api.controller.model.CarcacaRejeitadaModel;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;

@RepositoryRestResource(collectionResourceRel = "carcaca_rejeitada", path = "carcaca_rejeitada")
public interface CarcacaRejeitadaRepository extends CrudRepository<CarcacaRejeitadaModel, Integer> {

    @Query("from carcaca c where c.numero_etiqueta=:numeroEtiqueta")
    public List<Object> findByEtiquetaDuplicate(@Param("numeroEtiqueta") String numeroEtiqueta);

}
