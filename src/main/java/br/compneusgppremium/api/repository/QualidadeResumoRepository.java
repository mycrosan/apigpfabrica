package br.compneusgppremium.api.repository;

import br.compneusgppremium.api.controller.model.QualidadeModel;
import br.compneusgppremium.api.controller.model.ProducaoModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;

@Repository
public interface QualidadeResumoRepository extends JpaRepository<QualidadeModel, Integer> {

    // Corrigido para usar "qualidade" que é o nome da entidade, não o nome da tabela
    @Query("SELECT COUNT(q) FROM controle_qualidade q WHERE q.dt_create BETWEEN :start AND :end")
    Long countByDateRange(@Param("start") Date start, @Param("end") Date end);

    @Query("SELECT COUNT(c) FROM controle_qualidade c WHERE c.dt_create BETWEEN :start AND :end")
    Long countByData(@Param("start") Date start, @Param("end") Date end);

    @Query("SELECT COUNT(q) FROM controle_qualidade q WHERE q.dt_create >= :inicioMes AND q.dt_create < :inicioProximoMes")
    Long countByMes(@Param("inicioMes") Date inicioMes, @Param("inicioProximoMes") Date inicioProximoMes);

    // Contar quantidade de registros de qualidade vinculados a uma produção específica
    @Query("SELECT COUNT(q) FROM controle_qualidade q WHERE q.producao = :producao")
    Long countByProducao(@Param("producao") ProducaoModel producao);

    // Buscar lista de qualidade filtrando por produção
    List<QualidadeModel> findByProducao(ProducaoModel producao);

}
