package br.compneusgppremium.api.repository;

import br.compneusgppremium.api.controller.model.RegraModel;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;

@RepositoryRestResource(collectionResourceRel = "regra", path = "regra")
public interface RegraRepository extends CrudRepository<RegraModel, Integer> {

    @Query("from regra r where (:medidaPneuRaspado >= tamanho_min and :medidaPneuRaspado <= tamanho_max) " +
            "and (r.matriz.id = :matrizId)" +
            "and (r.medida.id = :medidaId)" +
            "and (r.modelo.id = :modeloId)" +
            "and (r.pais.id = :paisId)")
    public List<Object> findRule(@Param("matrizId") Integer matrizId, @Param("medidaId") Integer medidaId, @Param("modeloId") Integer modeloId, @Param("paisId") Integer paisId, @Param("medidaPneuRaspado") Double medidaPneuRaspado);

    @Query("from regra r where ((:tamanhoMin >= tamanho_min and :tamanhoMin <= tamanho_max) or (:tamanhoMax >= tamanho_min and :tamanhoMax <= tamanho_max))"+
            "and (r.matriz.id = :matrizId)" +
            "and (r.medida.id = :medidaId)" +
            "and (r.modelo.id = :modeloId)" +
            "and (r.pais.id = :paisId)")
    public List<Object> findByRange(@Param("matrizId") Integer matrizId, @Param("medidaId") Integer medidaId, @Param("modeloId") Integer modeloId, @Param("paisId") Integer paisId,@Param("tamanhoMin") Double tamanhoMin, @Param("tamanhoMax") Double tamanhoMax);

}
