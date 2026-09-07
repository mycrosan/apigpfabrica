package br.compneusgppremium.api.leitura.repository;
import br.compneusgppremium.api.leitura.model.ExecucaoLeitura;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
@RepositoryRestResource(exported = false)
public interface ExecucaoLeituraRepository extends JpaRepository<ExecucaoLeitura, String> { }
