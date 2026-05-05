package br.compneusgppremium.api.repository;

import br.compneusgppremium.api.controller.model.EspessuramentoModel;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(collectionResourceRel = "espessuramento", path = "espessuramento")
public interface EspessuramentoRepository extends CrudRepository<EspessuramentoModel, Integer> {
}
