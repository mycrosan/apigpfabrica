package br.compneusgppremium.api.repository;

import br.compneusgppremium.api.controller.model.AntiquebraModel;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(collectionResourceRel = "antiquebra", path = "antiquebra")
public interface AntiquebraRepository extends CrudRepository<AntiquebraModel, Integer> {
}
