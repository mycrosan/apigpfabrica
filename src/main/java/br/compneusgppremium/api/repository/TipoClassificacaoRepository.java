package br.compneusgppremium.api.repository;

import br.compneusgppremium.api.controller.model.TipoClassificacaoModel;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(collectionResourceRel = "tipo_classificacao", path = "tipo_classificacao")
public interface TipoClassificacaoRepository extends CrudRepository<TipoClassificacaoModel, Integer> {
}
