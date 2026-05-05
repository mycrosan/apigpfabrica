package br.compneusgppremium.api.repository;

import br.compneusgppremium.api.controller.model.MarcaModel;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(collectionResourceRel = "marca", path = "marca")
public interface MarcaRepository extends CrudRepository<MarcaModel, Integer> {
}
