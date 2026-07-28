package br.compneusgppremium.api.repository;

import br.compneusgppremium.api.controller.model.CarcacaModel;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.security.access.prepost.PostFilter;

import java.util.List;

@RepositoryRestResource(collectionResourceRel = "carcaca", path = "carcaca")
public interface CarcacaRepository extends CrudRepository<CarcacaModel, Integer> {

//    @Query("from carcaca c where c.numero_etiqueta=:numeroEtiqueta and c.status='start'")
    @Query("from carcaca c where c.numero_etiqueta=:numeroEtiqueta")
    public List<Object> findByEtiqueta(@Param("numeroEtiqueta") String numeroEtiqueta);

    @Query("from carcaca c where c.status=:statusFilter")
    public List<Object> findAllWithFilter(@Param("statusFilter") String statusFilter);

    @Override
    @Query("from carcaca c where c.status='start'")
    public Iterable<CarcacaModel> findAll();

    @Query("from carcaca c where c.numero_etiqueta=:numeroEtiqueta")
    public List<Object> findByEtiquetaDuplicate(@Param("numeroEtiqueta") String numeroEtiqueta);

    // Usado pela leitura automática por IA: quantas carcaças já cadastradas neste
    // pátio têm cada combinação modelo+medida (sinal de frequência/plausibilidade).
    @Query("select c.modelo.id, c.medida.id, count(c) from carcaca c " +
            "where c.modelo is not null and c.medida is not null " +
            "group by c.modelo.id, c.medida.id")
    public List<Object[]> contarPorModeloMedida();

    // Usado pela leitura automática por IA: carcaças já confirmadas com a mesma
    // combinação modelo+medida, para reaproveitar a foto arquivada como referência
    // visual quando os candidatos da leitura ficam ambíguos.
    @Query("from carcaca c where c.modelo.id=:modeloId and c.medida.id=:medidaId " +
            "and c.fotos is not null order by c.dt_create desc")
    public List<CarcacaModel> findComFotoPorModeloMedida(@Param("modeloId") Integer modeloId,
                                                          @Param("medidaId") Integer medidaId);

    // Usado pela validação de combinação no cadastro de carcaça: esse trio
    // modelo+medida+país já foi cadastrado antes (mesmo sem regra de produção)?
    @Query("select count(c) from carcaca c where c.modelo.id = :modeloId and c.medida.id = :medidaId and c.pais.id = :paisId")
    public long countPorTrio(@Param("modeloId") Integer modeloId, @Param("medidaId") Integer medidaId, @Param("paisId") Integer paisId);

    // Medidas já cadastradas com frequência mínima pra esse modelo — complementa
    // as medidas de `regra` na cascata do cadastro (pega casos com histórico real
    // mas sem regra formal ainda).
    @Query("select c.medida.id from carcaca c where c.modelo.id = :modeloId group by c.medida.id having count(c) >= :minOcorrencias")
    public List<Integer> findMedidaIdsFrequentesPorModelo(@Param("modeloId") Integer modeloId, @Param("minOcorrencias") long minOcorrencias);

}
