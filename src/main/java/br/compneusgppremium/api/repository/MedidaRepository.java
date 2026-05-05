package br.compneusgppremium.api.repository;

import br.compneusgppremium.api.controller.model.MedidaModel;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(collectionResourceRel = "medida", path = "medida")
public interface MedidaRepository extends CrudRepository<MedidaModel, Integer> {
}
