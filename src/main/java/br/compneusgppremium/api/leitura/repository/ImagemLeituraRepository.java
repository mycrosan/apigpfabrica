package br.compneusgppremium.api.leitura.repository;
import br.compneusgppremium.api.leitura.model.ImagemLeitura;
import org.springframework.data.jpa.repository.JpaRepository;
@org.springframework.data.rest.core.annotation.RepositoryRestResource(exported = false)
public interface ImagemLeituraRepository extends JpaRepository<ImagemLeitura, String> {
    boolean existsByArquivo(String arquivo);
    java.util.List<ImagemLeitura> findBySessaoId(String sessaoId);
}
