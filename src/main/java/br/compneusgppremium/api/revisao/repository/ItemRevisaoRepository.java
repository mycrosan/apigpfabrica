package br.compneusgppremium.api.revisao.repository;
import br.compneusgppremium.api.revisao.model.ItemRevisao;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
@org.springframework.data.rest.core.annotation.RepositoryRestResource(exported = false)
public interface ItemRevisaoRepository extends JpaRepository<ItemRevisao, String>, JpaSpecificationExecutor<ItemRevisao> {
    Optional<ItemRevisao> findByChaveOrigem(String chave);
    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from ItemRevisao i where i.id = :id")
    Optional<ItemRevisao> bloquear(@Param("id") String id);
    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    List<ItemRevisao> findBySessaoIdOrderById(String sessaoId);
    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from ItemRevisao i where i.sessaoId in "
            + "(select s.id from SessaoLeitura s where s.carcacaId = :id) order by i.id")
    List<ItemRevisao> porCarcaca(@Param("id") Integer id);
}
