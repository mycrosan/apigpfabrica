package br.compneusgppremium.api.repository;

import br.compneusgppremium.api.controller.model.PneuVulcanizadoModel;
import br.compneusgppremium.api.controller.model.PneuVulcanizadoModel.StatusVulcanizacao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;
import java.util.Optional;

@RepositoryRestResource(collectionResourceRel = "pneus-vulcanizados", path = "pneus-vulcanizados")
public interface PneuVulcanizadoRepository extends CrudRepository<PneuVulcanizadoModel, Long> {

    /**
     * Busca pneus vulcanizados por usuário
     */
    @Query("SELECT p FROM PneuVulcanizadoModel p WHERE p.usuarioId = :usuarioId AND p.dtDelete IS NULL ORDER BY p.dtCreate DESC")
    List<PneuVulcanizadoModel> findByUsuarioId(@Param("usuarioId") Long usuarioId);

    /**
     * Busca pneus vulcanizados por usuário com paginação
     */
    @Query("SELECT p FROM PneuVulcanizadoModel p WHERE p.usuarioId = :usuarioId AND p.dtDelete IS NULL ORDER BY p.dtCreate DESC")
    Page<PneuVulcanizadoModel> findByUsuarioId(@Param("usuarioId") Long usuarioId, Pageable pageable);

    /**
     * Busca pneus vulcanizados por produção
     */
    @Query("SELECT p FROM PneuVulcanizadoModel p WHERE p.producaoId = :producaoId AND p.dtDelete IS NULL ORDER BY p.dtCreate DESC")
    List<PneuVulcanizadoModel> findByProducaoId(@Param("producaoId") Long producaoId);

    /**
     * Busca pneus vulcanizados por status
     */
    @Query("SELECT p FROM PneuVulcanizadoModel p WHERE p.status = :status AND p.dtDelete IS NULL ORDER BY p.dtCreate DESC")
    List<PneuVulcanizadoModel> findByStatus(@Param("status") StatusVulcanizacao status);

    /**
     * Busca pneus vulcanizados por status com paginação
     */
    @Query("SELECT p FROM PneuVulcanizadoModel p WHERE p.status = :status AND p.dtDelete IS NULL ORDER BY p.dtCreate DESC")
    Page<PneuVulcanizadoModel> findByStatus(@Param("status") StatusVulcanizacao status, Pageable pageable);

    /**
     * Busca pneus vulcanizados por usuário e status
     */
    @Query("SELECT p FROM PneuVulcanizadoModel p WHERE p.usuarioId = :usuarioId AND p.status = :status AND p.dtDelete IS NULL ORDER BY p.dtCreate DESC")
    List<PneuVulcanizadoModel> findByUsuarioIdAndStatus(@Param("usuarioId") Long usuarioId, @Param("status") StatusVulcanizacao status);

    /**
     * Busca pneus vulcanizados por usuário e status com paginação
     */
    @Query("SELECT p FROM PneuVulcanizadoModel p WHERE p.usuarioId = :usuarioId AND p.status = :status AND p.dtDelete IS NULL ORDER BY p.dtCreate DESC")
    Page<PneuVulcanizadoModel> findByUsuarioIdAndStatus(@Param("usuarioId") Long usuarioId, @Param("status") StatusVulcanizacao status, Pageable pageable);

    /**
     * Busca pneu vulcanizado específico por usuário e produção
     */
    @Query("SELECT p FROM PneuVulcanizadoModel p WHERE p.usuarioId = :usuarioId AND p.producaoId = :producaoId AND p.dtDelete IS NULL")
    Optional<PneuVulcanizadoModel> findByUsuarioIdAndProducaoId(@Param("usuarioId") Long usuarioId, @Param("producaoId") Long producaoId);

    /**
     * Busca todos os pneus vulcanizados ativos com paginação
     */
    @Query("SELECT p FROM PneuVulcanizadoModel p WHERE p.dtDelete IS NULL ORDER BY p.dtCreate DESC")
    Page<PneuVulcanizadoModel> findAllActive(Pageable pageable);

    /**
     * Conta pneus vulcanizados por status
     */
    @Query("SELECT COUNT(p) FROM PneuVulcanizadoModel p WHERE p.status = :status AND p.dtDelete IS NULL")
    Long countByStatus(@Param("status") StatusVulcanizacao status);

    /**
     * Conta pneus vulcanizados por usuário
     */
    @Query("SELECT COUNT(p) FROM PneuVulcanizadoModel p WHERE p.usuarioId = :usuarioId AND p.dtDelete IS NULL")
    Long countByUsuarioId(@Param("usuarioId") Long usuarioId);

    /**
     * Conta pneus vulcanizados finalizados para uma determinada produção
     */
    @Query("SELECT COUNT(p) FROM PneuVulcanizadoModel p WHERE p.producaoId = :producaoId AND p.status = :status AND p.dtDelete IS NULL")
    Long countByProducaoIdAndStatus(@Param("producaoId") Long producaoId, @Param("status") StatusVulcanizacao status);
}