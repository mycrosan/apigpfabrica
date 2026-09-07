package br.compneusgppremium.api.revisao.repository;
import br.compneusgppremium.api.revisao.model.ImagemRevisao;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
@org.springframework.data.rest.core.annotation.RepositoryRestResource(exported = false)
public interface ImagemRevisaoRepository extends JpaRepository<ImagemRevisao, String> {
    Optional<ImagemRevisao> findBySha256(String sha256);
}
