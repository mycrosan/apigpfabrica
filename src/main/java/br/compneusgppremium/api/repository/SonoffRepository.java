package br.compneusgppremium.api.repository;

import br.compneusgppremium.api.controller.model.SonoffModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface SonoffRepository extends JpaRepository<SonoffModel, Long> {
    Optional<SonoffModel> findByIpAndDtDeleteIsNull(String ip);

    List<SonoffModel> findByCelularIdAndDtDeleteIsNullOrderByDtCreateDesc(String celularId);

    List<SonoffModel> findByDtDeleteIsNullOrderByDtCreateDesc();
}