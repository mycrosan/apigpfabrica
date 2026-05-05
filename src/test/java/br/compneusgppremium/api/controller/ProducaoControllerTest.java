package br.compneusgppremium.api.controller;

import br.compneusgppremium.api.controller.enums.RegraStatus;
import br.compneusgppremium.api.controller.model.CarcacaModel;
import br.compneusgppremium.api.controller.model.ProducaoModel;
import br.compneusgppremium.api.controller.model.RegraModel;
import br.compneusgppremium.api.repository.CarcacaRepository;
import br.compneusgppremium.api.repository.ProducaoRepository;
import br.compneusgppremium.api.repository.RegraRepository;
import br.compneusgppremium.api.util.ApiError;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ProducaoControllerTest {

    @Mock
    private ProducaoRepository producaoRepository;

    @Mock
    private CarcacaRepository carcacaRepository;

    @Mock
    private EntityManager entityManager;

    @Mock
    private RegraRepository regraRepository;

    @InjectMocks
    private ProducaoController producaoController;

    @Test
    void deveBloquearNovaProducaoQuandoRegraEmValidacaoJaPossuiUmaProducao() {
        // Arrange
        CarcacaModel carcaca = new CarcacaModel();
        carcaca.setId(10);

        RegraModel regra = new RegraModel();
        regra.setId(5);
        regra.setStatus(RegraStatus.EM_VALIDACAO);

        ProducaoModel producao = new ProducaoModel();
        producao.setCarcaca(carcaca);
        producao.setRegra(regra);

        // Mock consulta de duplicidade por carcaça: retornar vazio para passar na primeira verificação
        Query queryMock = Mockito.mock(Query.class);
        when(entityManager.createQuery(anyString())).thenReturn(queryMock);
        when(queryMock.getResultList()).thenReturn(Collections.emptyList());

        // Mock contagem de produções vinculadas à regra: já existe uma
        when(producaoRepository.countByRegraId(regra.getId())).thenReturn(1L);

        // Mock busca da regra por ID retornando a regra com status EM_VALIDACAO
        when(regraRepository.findById(regra.getId())).thenReturn(Optional.of(regra));

        // Act
        Object resultado = producaoController.salvar(producao);

        // Assert
        assertTrue(resultado instanceof ApiError, "Deve retornar ApiError quando bloquear criação");
        ApiError erro = (ApiError) resultado;
        assertEquals("PRECONDITION_REQUIRED", erro.getStatus().name());
        assertTrue(erro.getMessage().contains("Regra em validação"));
    }
}