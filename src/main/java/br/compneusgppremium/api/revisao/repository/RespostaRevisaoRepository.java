package br.compneusgppremium.api.revisao.repository;
import br.compneusgppremium.api.revisao.model.RespostaRevisao;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
@org.springframework.data.rest.core.annotation.RepositoryRestResource(exported = false)
public interface RespostaRevisaoRepository extends JpaRepository<RespostaRevisao, String> {
    List<RespostaRevisao> findByItemIdAndCicloOrderByCriadaEmAsc(String itemId, int ciclo);
    Optional<RespostaRevisao> findByRevisorIdAndChave(Integer revisorId, String chave);
    boolean existsByItemIdAndRevisorId(String itemId, Integer revisorId);
}
