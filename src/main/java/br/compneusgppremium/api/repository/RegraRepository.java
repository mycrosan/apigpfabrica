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

    // Usado pela validação de combinação no cadastro de carcaça: existe regra de
    // produção pra esse trio? (independente de matriz/tamanho do pneu raspado)
    @Query("select count(r) > 0 from regra r where r.modelo.id = :modeloId and r.medida.id = :medidaId and r.pais.id = :paisId")
    public boolean existeParaTrio(@Param("modeloId") Integer modeloId, @Param("medidaId") Integer medidaId, @Param("paisId") Integer paisId);

    // Medidas que já têm regra de produção pra esse modelo — usado pra filtrar o
    // dropdown de medida na cascata do cadastro.
    @Query("select distinct r.medida.id from regra r where r.modelo.id = :modeloId")
    public List<Integer> findMedidaIdsPorModelo(@Param("modeloId") Integer modeloId);

}
