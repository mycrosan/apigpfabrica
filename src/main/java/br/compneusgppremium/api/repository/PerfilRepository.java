package br.compneusgppremium.api.repository;

import br.compneusgppremium.api.controller.model.PerfilModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(collectionResourceRel = "perfil", path = "perfil")
public interface PerfilRepository extends JpaRepository<PerfilModel, Integer> {}
