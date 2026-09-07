package br.compneusgppremium.api.leitura.repository;
import br.compneusgppremium.api.leitura.model.SessaoLeitura;
import org.springframework.data.jpa.repository.JpaRepository;
@org.springframework.data.rest.core.annotation.RepositoryRestResource(exported = false)
public interface SessaoLeituraRepository extends JpaRepository<SessaoLeitura, String> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select s from SessaoLeitura s where s.id = :id")
    java.util.Optional<SessaoLeitura> bloquear(@org.springframework.data.repository.query.Param("id") String id);
}
