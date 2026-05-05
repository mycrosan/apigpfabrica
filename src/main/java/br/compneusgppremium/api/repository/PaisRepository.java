package br.compneusgppremium.api.repository;

import br.compneusgppremium.api.controller.model.PaisModel;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(collectionResourceRel = "pais", path = "pais")
public interface PaisRepository extends CrudRepository<PaisModel, Integer> {
}
