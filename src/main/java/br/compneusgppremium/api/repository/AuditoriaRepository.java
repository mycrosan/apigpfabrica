package br.compneusgppremium.api.repository;

import br.compneusgppremium.api.controller.model.AuditoriaModel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditoriaRepository extends JpaRepository<AuditoriaModel, Integer> {
    boolean existsByTabelaAfetadaAndUsuario_Id(String tabelaAfetada, Integer usuarioId);
}
