package br.compneusgppremium.api.repository;

import br.compneusgppremium.api.controller.model.PaisModel;
import br.compneusgppremium.api.controller.model.StatusCarcacaModel;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(collectionResourceRel = "status_carcaca", path = "status_carcaca")
public interface StatusCarcacaRepository extends CrudRepository<StatusCarcacaModel, Integer> {
}
