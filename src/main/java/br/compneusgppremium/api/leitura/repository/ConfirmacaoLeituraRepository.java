package br.compneusgppremium.api.leitura.repository;
import br.compneusgppremium.api.leitura.model.ConfirmacaoLeitura;
import org.springframework.data.jpa.repository.JpaRepository;
@org.springframework.data.rest.core.annotation.RepositoryRestResource(exported = false)
public interface ConfirmacaoLeituraRepository extends JpaRepository<ConfirmacaoLeitura, String> {
    java.util.Optional<br.compneusgppremium.api.leitura.model.ConfirmacaoLeitura>
            findFirstByTentativaIdOrderByCriadaEmDesc(String tentativaId);
}
