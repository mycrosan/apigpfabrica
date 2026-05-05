package br.compneusgppremium.api.repository;

import br.compneusgppremium.api.controller.model.RegraModel;
import br.compneusgppremium.api.controller.model.ValidaRegraModel;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RepositoryRestResource(collectionResourceRel = "valida_regra", path = "valida_regra")
public interface ValidaRegraRepository extends CrudRepository<ValidaRegraModel, Integer> {

    @Query("SELECT v FROM valida_regra v WHERE v.regra.id = :regraId")
    List<ValidaRegraModel> findByRegraId(@Param("regraId") Integer regraId);

    @Query("SELECT COUNT(v) FROM valida_regra v WHERE v.regra.id = :regraId AND v.status = true")
    long countAprovadasByRegraId(@Param("regraId") Integer regraId);

    /**
     * Remove todas as validações vinculadas ao controle de qualidade informado.
     * Necessário para evitar violação de chave estrangeira ao excluir o controle de qualidade.
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM valida_regra v WHERE v.qualidade.id = :qualidadeId")
    void deleteByQualidadeId(@Param("qualidadeId") Integer qualidadeId);
}
