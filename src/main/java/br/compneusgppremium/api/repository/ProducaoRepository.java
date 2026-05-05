package br.compneusgppremium.api.repository;

import br.compneusgppremium.api.controller.model.*;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;
import java.util.Optional;

@RepositoryRestResource(collectionResourceRel = "producao", path = "producao")
public interface ProducaoRepository extends CrudRepository<ProducaoModel, Integer> {

    @Query("FROM producao p WHERE p.carcaca.numero_etiqueta = :numeroEtiqueta")
    List<Object> findDuplicate(@Param("numeroEtiqueta") String numeroEtiqueta);

    @Query("SELECT p FROM producao p JOIN p.carcaca ca WHERE ca.numero_etiqueta = :numeroEtiqueta")
    Optional<ProducaoModel> findByEtiqueta(@Param("numeroEtiqueta") String numeroEtiqueta);

    @Query("SELECT COUNT(p) FROM producao p WHERE p.regra.id = :regraId")
    long countByRegraId(@Param("regraId") Integer regraId);

    @Query("SELECT p FROM producao p WHERE p.regra.id = :regraId ORDER BY p.dt_create ASC")
    List<ProducaoModel> findFirstByRegraIdOrderByDtCreateAsc(@Param("regraId") Integer regraId);
}

