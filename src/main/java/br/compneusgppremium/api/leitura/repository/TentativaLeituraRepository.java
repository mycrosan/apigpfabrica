package br.compneusgppremium.api.leitura.repository;
import br.compneusgppremium.api.leitura.model.TentativaLeitura;
import org.springframework.data.jpa.repository.JpaRepository;
@org.springframework.data.rest.core.annotation.RepositoryRestResource(exported = false)
public interface TentativaLeituraRepository extends JpaRepository<TentativaLeitura, String> {
    @org.springframework.data.jpa.repository.Query("select t.id from TentativaLeitura t where t.estado <> 'PROCESSANDO' "
            + "and not exists (select i.id from ItemRevisao i where i.tentativaId = t.id) order by t.criadaEm")
    java.util.List<String> pendentesRevisao(org.springframework.data.domain.Pageable pagina);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select t from TentativaLeitura t where t.id = :id")
    java.util.Optional<TentativaLeitura> bloquear(@org.springframework.data.repository.query.Param("id") String id);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    java.util.List<TentativaLeitura> findByEstadoAndCriadaEmBefore(String estado, java.time.Instant limite,
            org.springframework.data.domain.Pageable pagina);
    java.util.Optional<TentativaLeitura> findBySessaoIdAndChaveIdempotencia(String sessaoId, String chave);
    java.util.List<TentativaLeitura> findBySessaoIdOrderByCriadaEmAsc(String sessaoId);
}
