package br.compneusgppremium.api.controller;

import br.compneusgppremium.api.controller.enums.RegraStatus;
import br.compneusgppremium.api.controller.model.CarcacaModel;
import br.compneusgppremium.api.controller.model.QualidadeModel;
import br.compneusgppremium.api.controller.model.ProducaoModel;
import br.compneusgppremium.api.controller.model.RegraModel;
import br.compneusgppremium.api.controller.model.StatusCarcacaModel;
import br.compneusgppremium.api.controller.model.TipoClassificacaoModel;
import br.compneusgppremium.api.controller.model.TipoObservacaoModel;
import br.compneusgppremium.api.repository.CarcacaRepository;
import br.compneusgppremium.api.repository.ProducaoRepository;
import br.compneusgppremium.api.repository.QualidadeRepository;
import br.compneusgppremium.api.repository.RegraRepository;
import br.compneusgppremium.api.repository.UsuarioRepository;
import br.compneusgppremium.api.util.ApiError;
import br.compneusgppremium.api.util.UsuarioLogadoUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.Date;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyInt;
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

    @Mock
    private QualidadeRepository qualidadeRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private UsuarioLogadoUtil usuarioLogadoUtil;

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
        TypedQuery<ProducaoModel> queryMock = Mockito.mock(TypedQuery.class);
        when(entityManager.createQuery(anyString())).thenReturn(queryMock);
        when(entityManager.createQuery(anyString(), Mockito.eq(ProducaoModel.class))).thenReturn(queryMock);
        when(queryMock.getResultList()).thenReturn(Collections.emptyList());
        when(queryMock.setMaxResults(anyInt())).thenReturn(queryMock);

        ProducaoModel primeiraProducao = new ProducaoModel();
        primeiraProducao.setId(99);
        primeiraProducao.setCarcaca(carcaca);

        when(producaoRepository.findFirstByRegraIdOrderByDtCreateAsc(regra.getId())).thenReturn(List.of(primeiraProducao));
        when(qualidadeRepository.findByProducaoId(primeiraProducao.getId())).thenReturn(Optional.empty());

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

    @Test
    void deveBloquearNovaProducaoParaMesmaCarcacaMesmoQuandoCarcacaRejeitada() {
        CarcacaModel carcacaPayload = new CarcacaModel();
        carcacaPayload.setId(10);

        ProducaoModel producao = new ProducaoModel();
        producao.setCarcaca(carcacaPayload);
        producao.setDados("{}");
        producao.setDt_create(new Date());
        producao.setDt_update(new Date());
        producao.setUuid(UUID.randomUUID());

        TypedQuery<ProducaoModel> queryMock = Mockito.mock(TypedQuery.class);
        when(entityManager.createQuery(anyString())).thenReturn(queryMock);
        when(entityManager.createQuery(anyString(), Mockito.eq(ProducaoModel.class))).thenReturn(queryMock);
        when(queryMock.setMaxResults(anyInt())).thenReturn(queryMock);

        ProducaoModel producaoExistente = new ProducaoModel();
        producaoExistente.setId(1);
        producaoExistente.setCarcaca(carcacaPayload);
        when(queryMock.getResultList()).thenReturn(List.of(producaoExistente));

        CarcacaModel carcacaDb = new CarcacaModel();
        carcacaDb.setId(10);
        StatusCarcacaModel statusRejeitada = new StatusCarcacaModel();
        statusRejeitada.setId(4);
        carcacaDb.setStatus_carcaca(statusRejeitada);
        when(carcacaRepository.findById(carcacaPayload.getId())).thenReturn(Optional.of(carcacaDb));
        when(carcacaRepository.save(Mockito.any(CarcacaModel.class))).thenAnswer(invocation -> invocation.getArgument(0));

        when(usuarioLogadoUtil.getUsuarioIdLogado()).thenReturn(1);
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(new br.compneusgppremium.api.controller.model.UsuarioModel()));

        when(producaoRepository.save(Mockito.any(ProducaoModel.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Object resultado = producaoController.salvar(producao);

        assertTrue(resultado instanceof ApiError);
        ApiError erro = (ApiError) resultado;
        assertTrue(erro.getMessage().contains("Já produzido!"));
    }

    @Test
    void devePermitirNovaProducaoQuandoRegraEmValidacaoEPrimeiraProducaoFoiRejeitadaNaQualidade() {
        CarcacaModel carcaca = new CarcacaModel();
        carcaca.setId(10);

        RegraModel regra = new RegraModel();
        regra.setId(5);
        regra.setStatus(RegraStatus.EM_VALIDACAO);

        ProducaoModel producao = new ProducaoModel();
        producao.setCarcaca(carcaca);
        producao.setRegra(regra);

        TypedQuery<ProducaoModel> queryMock = Mockito.mock(TypedQuery.class);
        when(entityManager.createQuery(anyString())).thenReturn(queryMock);
        when(entityManager.createQuery(anyString(), Mockito.eq(ProducaoModel.class))).thenReturn(queryMock);
        when(queryMock.setMaxResults(anyInt())).thenReturn(queryMock);
        when(queryMock.getResultList()).thenReturn(Collections.emptyList());

        ProducaoModel primeiraProducao = new ProducaoModel();
        primeiraProducao.setId(99);
        primeiraProducao.setCarcaca(carcaca);

        when(regraRepository.findById(regra.getId())).thenReturn(Optional.of(regra));
        when(producaoRepository.findFirstByRegraIdOrderByDtCreateAsc(regra.getId())).thenReturn(List.of(primeiraProducao));

        TipoClassificacaoModel tipoClassificacao = new TipoClassificacaoModel();
        tipoClassificacao.setId(2);
        TipoObservacaoModel tipoObservacao = new TipoObservacaoModel();
        tipoObservacao.setTipo_classificacao(tipoClassificacao);

        QualidadeModel qualidadeRejeitada = new QualidadeModel();
        qualidadeRejeitada.setTipo_observacao(tipoObservacao);
        when(qualidadeRepository.findByProducaoId(primeiraProducao.getId())).thenReturn(Optional.of(qualidadeRejeitada));

        when(carcacaRepository.findById(carcaca.getId())).thenReturn(Optional.of(carcaca));
        when(carcacaRepository.save(Mockito.any(CarcacaModel.class))).thenAnswer(invocation -> invocation.getArgument(0));

        when(usuarioLogadoUtil.getUsuarioIdLogado()).thenReturn(1);
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(new br.compneusgppremium.api.controller.model.UsuarioModel()));

        when(producaoRepository.save(Mockito.any(ProducaoModel.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Object resultado = producaoController.salvar(producao);

        assertFalse(resultado instanceof ApiError);
        assertTrue(resultado instanceof ProducaoModel);
    }
}
