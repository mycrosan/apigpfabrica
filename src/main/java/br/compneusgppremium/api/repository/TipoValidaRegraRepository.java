package br.compneusgppremium.api.repository;

import br.compneusgppremium.api.controller.model.PaisModel;
import br.compneusgppremium.api.controller.model.TipoValidaRegraModel;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(collectionResourceRel = "tipo_valida_regra", path = "tipo_valida_regra")
public interface TipoValidaRegraRepository extends CrudRepository<TipoValidaRegraModel, Integer> {
}
