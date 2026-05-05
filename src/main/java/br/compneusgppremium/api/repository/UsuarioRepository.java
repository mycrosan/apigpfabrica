package br.compneusgppremium.api.repository;

import br.compneusgppremium.api.controller.model.UsuarioModel;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface UsuarioRepository extends CrudRepository<UsuarioModel, Long> {
    Optional<UsuarioModel> findByLogin(String login);
}
