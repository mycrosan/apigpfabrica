package br.compneusgppremium.api.repository;

import br.compneusgppremium.api.controller.model.MotoristaModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MotoristaRepository extends JpaRepository<MotoristaModel, Integer> {
    Optional<MotoristaModel> findByIdAndAtivoTrue(Integer id);
    boolean existsByUsuarioId(Long usuarioId);
    boolean existsByCpf(String cpf);

    @Query("SELECT m FROM motorista m WHERE (:ativo IS NULL OR m.ativo = :ativo) ORDER BY m.dataCriacao DESC")
    List<MotoristaModel> findAllByAtivo(@Param("ativo") Boolean ativo);
}
