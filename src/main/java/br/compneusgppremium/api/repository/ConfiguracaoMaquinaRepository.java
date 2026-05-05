package br.compneusgppremium.api.repository;

import br.compneusgppremium.api.controller.model.ConfiguracaoMaquinaModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository para operações de ConfiguracaoMaquinaModel
 */
@Repository
public interface ConfiguracaoMaquinaRepository extends JpaRepository<ConfiguracaoMaquinaModel, Long> {

    /**
     * Busca configuração por ID excluindo registros deletados
     */
    @Query("SELECT c FROM maquina_configuracao c " +
           "LEFT JOIN FETCH c.matriz " +
           "LEFT JOIN FETCH c.maquina " +
           "WHERE c.id = :id AND c.dtDelete IS NULL")
    Optional<ConfiguracaoMaquinaModel> findByIdAndDtDeleteIsNull(@Param("id") Long id);

    /**
     * Busca todas as configurações excluindo registros deletados, ordenadas por data de criação
     */
    @Query(value = "SELECT c FROM maquina_configuracao c " +
           "LEFT JOIN FETCH c.matriz " +
           "LEFT JOIN FETCH c.maquina " +
           "WHERE c.dtDelete IS NULL ORDER BY c.dtCreate DESC",
           countQuery = "SELECT COUNT(c) FROM maquina_configuracao c WHERE c.dtDelete IS NULL")
    Page<ConfiguracaoMaquinaModel> findByDtDeleteIsNullOrderByDtCreateDesc(Pageable pageable);

    /**
     * Busca configurações por ID da máquina excluindo registros deletados, ordenadas por data de criação
     */
    @Query("SELECT c FROM maquina_configuracao c WHERE c.maquina.id = :maquinaId AND c.dtDelete IS NULL ORDER BY c.dtCreate DESC")
    Page<ConfiguracaoMaquinaModel> findByMaquinaIdAndDtDeleteIsNullOrderByDtCreateDesc(@Param("maquinaId") Long maquinaId, Pageable pageable);

    /**
     * Busca configurações por ID da máquina excluindo registros deletados
     */
    @Query("SELECT c FROM maquina_configuracao c WHERE c.maquina.id = :maquinaId AND c.dtDelete IS NULL")
    List<ConfiguracaoMaquinaModel> findByMaquinaIdAndDtDeleteIsNull(@Param("maquinaId") Long maquinaId);

    /**
     * Busca a configuração ativa (mais recente) por celularId excluindo registros deletados
     * Retorna apenas o registro mais atual caso existam múltiplos resultados
     */
    @Query("SELECT c FROM maquina_configuracao c " +
           "LEFT JOIN FETCH c.matriz " +
           "LEFT JOIN FETCH c.maquina " +
           "WHERE c.celularId = :celularId AND c.dtDelete IS NULL " +
           "ORDER BY c.dtCreate DESC")
    List<ConfiguracaoMaquinaModel> findActiveByCelularId(@Param("celularId") String celularId, Pageable pageable);

    /**
     * Busca todas as configurações por celularId excluindo registros deletados, ordenadas por data de criação
     */
    @Query("SELECT c FROM maquina_configuracao c WHERE c.celularId = :celularId AND c.dtDelete IS NULL ORDER BY c.dtCreate DESC")
    List<ConfiguracaoMaquinaModel> findByCelularIdAndDtDeleteIsNullOrderByDtCreateDesc(@Param("celularId") String celularId);

    /**
     * Busca configurações por ID da matriz excluindo registros deletados
     */
    @Query("SELECT c FROM maquina_configuracao c WHERE c.matriz.id = :matrizId AND c.dtDelete IS NULL")
    List<ConfiguracaoMaquinaModel> findByMatrizIdAndDtDeleteIsNull(@Param("matrizId") Long matrizId);

    /**
     * Busca configurações por celular ID excluindo registros deletados
     */
    @Query("SELECT c FROM maquina_configuracao c WHERE c.celularId = :celularId AND c.dtDelete IS NULL")
    List<ConfiguracaoMaquinaModel> findByCelularIdAndDtDeleteIsNull(@Param("celularId") String celularId);

    /**
     * Busca configurações por descrição (busca parcial) excluindo registros deletados
     */
    @Query("SELECT c FROM maquina_configuracao c WHERE LOWER(c.descricao) LIKE LOWER(CONCAT('%', :descricao, '%')) AND c.dtDelete IS NULL")
    Page<ConfiguracaoMaquinaModel> findByDescricaoContainingIgnoreCaseAndDtDeleteIsNull(
            @Param("descricao") String descricao, 
            Pageable pageable);

    /**
     * Busca configurações por máquina e matriz excluindo registros deletados
     */
    @Query("SELECT c FROM maquina_configuracao c WHERE c.maquina.id = :maquinaId AND c.matriz.id = :matrizId AND c.dtDelete IS NULL")
    List<ConfiguracaoMaquinaModel> findByMaquinaIdAndMatrizIdAndDtDeleteIsNull(
            @Param("maquinaId") Long maquinaId, 
            @Param("matrizId") Long matrizId);

    /**
     * Verifica se existe configuração com celular ID para uma máquina específica (exceto o ID fornecido)
     */
    @Query("SELECT COUNT(c) > 0 FROM maquina_configuracao c WHERE " +
           "c.maquina.id = :maquinaId AND " +
           "c.celularId = :celularId AND " +
           "c.id != :id AND " +
           "c.dtDelete IS NULL")
    boolean existsByMaquinaIdAndCelularIdAndIdNotAndDtDeleteIsNull(
            @Param("maquinaId") Long maquinaId,
            @Param("celularId") String celularId,
            @Param("id") Long id);

    /**
     * Verifica se existe configuração com celular ID para uma máquina específica
     */
    @Query("SELECT COUNT(c) > 0 FROM maquina_configuracao c WHERE " +
           "c.maquina.id = :maquinaId AND " +
           "c.celularId = :celularId AND " +
           "c.dtDelete IS NULL")
    boolean existsByMaquinaIdAndCelularIdAndDtDeleteIsNull(
            @Param("maquinaId") Long maquinaId,
            @Param("celularId") String celularId);

    /**
     * Busca configurações com filtros opcionais excluindo registros deletados
     */
    @Query(value = "SELECT c FROM maquina_configuracao c " +
           "LEFT JOIN FETCH c.matriz " +
           "LEFT JOIN FETCH c.maquina " +
           "WHERE " +
           "(:maquinaId IS NULL OR c.maquina.id = :maquinaId) AND " +
           "(:matrizId IS NULL OR c.matriz.id = :matrizId) AND " +
           "(:celularId IS NULL OR c.celularId = :celularId) AND " +
           "c.dtDelete IS NULL " +
           "ORDER BY c.dtCreate DESC",
           countQuery = "SELECT COUNT(c) FROM maquina_configuracao c WHERE " +
           "(:maquinaId IS NULL OR c.maquina.id = :maquinaId) AND " +
           "(:matrizId IS NULL OR c.matriz.id = :matrizId) AND " +
           "(:celularId IS NULL OR c.celularId = :celularId) AND " +
           "c.dtDelete IS NULL")
    Page<ConfiguracaoMaquinaModel> findByFiltersAndDtDeleteIsNull(
            @Param("maquinaId") Long maquinaId,
            @Param("matrizId") Long matrizId,
            @Param("celularId") String celularId,
            Pageable pageable);

    /**
     * Busca a configuração anterior ativa para uma máquina específica (excluindo a configuração atual)
     * Retorna a configuração mais recente que não seja a que está sendo deletada
     */
    @Query("SELECT c FROM maquina_configuracao c WHERE " +
           "c.maquina.id = :maquinaId AND " +
           "c.id != :excludeId AND " +
           "c.dtDelete IS NULL " +
           "ORDER BY c.dtCreate DESC")
    Optional<ConfiguracaoMaquinaModel> findPreviousActiveBymaquinaIdExcludingId(
            @Param("maquinaId") Long maquinaId,
            @Param("excludeId") Long excludeId);
}