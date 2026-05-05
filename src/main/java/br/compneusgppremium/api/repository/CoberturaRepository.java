package br.compneusgppremium.api.repository;

import br.compneusgppremium.api.controller.model.CoberturaModel;
import br.compneusgppremium.api.controller.model.ColaModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CoberturaRepository extends JpaRepository<CoberturaModel, Integer> {

    // Buscar por cola
    Optional<CoberturaModel> findByColaId(Integer colaId);

    // Buscar cobertura pela etiqueta (via cola -> produção -> carcaça)
    @Query("SELECT c FROM CoberturaModel c " +
            "JOIN c.cola col " +
            "JOIN col.producao p " +
            "JOIN p.carcaca ca " +
            "WHERE ca.numero_etiqueta = :etiqueta")
    Optional<CoberturaModel> findByEtiqueta(String etiqueta);

    // Verificar se existe cobertura por produção
    boolean existsByColaProducaoId(Integer producaoId);

    // Buscar cobertura por produção
    Optional<CoberturaModel> findByColaProducaoId(Integer producaoId);

    // Colas sem cobertura
    @Query("SELECT col FROM ColaModel col " +
            "WHERE NOT EXISTS (" +
            "   SELECT 1 FROM CoberturaModel cb " +
            "   WHERE cb.cola.id = col.id" +
            ")")
    List<ColaModel> findColasSemCobertura();

}
