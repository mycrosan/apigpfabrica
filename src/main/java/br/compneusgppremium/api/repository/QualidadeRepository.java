package br.compneusgppremium.api.repository;

import br.compneusgppremium.api.controller.model.QualidadeModel;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;
import java.util.Optional;

@RepositoryRestResource(collectionResourceRel = "controle_qualidade", path = "controle_qualidade")
public interface QualidadeRepository extends CrudRepository<QualidadeModel, Integer> {

    @Query("from controle_qualidade qc where qc.producao.id=:producaoId")
    public Optional<QualidadeModel> findByProducaoId(@Param("producaoId") Integer producaoId);

    @Query("SELECT cq FROM controle_qualidade cq JOIN cq.producao e JOIN e.carcaca c WHERE c.numero_etiqueta = :numeroEtiqueta")
    public List<Object> findByEtiqueta(@Param("numeroEtiqueta") String numeroEtiqueta);


}
