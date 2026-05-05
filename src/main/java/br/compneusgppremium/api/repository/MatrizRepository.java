package br.compneusgppremium.api.repository;

import br.compneusgppremium.api.controller.model.MatrizModel;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(collectionResourceRel = "matriz", path = "matriz")
public interface MatrizRepository extends CrudRepository<MatrizModel, Integer> {}
