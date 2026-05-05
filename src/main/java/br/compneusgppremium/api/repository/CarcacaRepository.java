package br.compneusgppremium.api.repository;

import br.compneusgppremium.api.controller.model.CarcacaModel;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.security.access.prepost.PostFilter;

import java.util.List;

@RepositoryRestResource(collectionResourceRel = "carcaca", path = "carcaca")
public interface CarcacaRepository extends CrudRepository<CarcacaModel, Integer> {

//    @Query("from carcaca c where c.numero_etiqueta=:numeroEtiqueta and c.status='start'")
    @Query("from carcaca c where c.numero_etiqueta=:numeroEtiqueta")
    public List<Object> findByEtiqueta(@Param("numeroEtiqueta") String numeroEtiqueta);

    @Query("from carcaca c where c.status=:statusFilter")
    public List<Object> findAllWithFilter(@Param("statusFilter") String statusFilter);

    @Override
    @Query("from carcaca c where c.status='start'")
    public Iterable<CarcacaModel> findAll();

    @Query("from carcaca c where c.numero_etiqueta=:numeroEtiqueta")
    public List<Object> findByEtiquetaDuplicate(@Param("numeroEtiqueta") String numeroEtiqueta);

}
