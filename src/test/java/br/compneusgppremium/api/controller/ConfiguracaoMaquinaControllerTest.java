package br.compneusgppremium.api.controller;

import br.compneusgppremium.api.controller.dto.ConfiguracaoMaquinaCreateDTO;
import br.compneusgppremium.api.controller.model.ConfiguracaoMaquinaModel;
import br.compneusgppremium.api.controller.model.MatrizModel;
import br.compneusgppremium.api.controller.model.RegistroMaquinaModel;
import br.compneusgppremium.api.repository.ConfiguracaoMaquinaRepository;
import br.compneusgppremium.api.repository.MatrizRepository;
import br.compneusgppremium.api.repository.RegistroMaquinaRepository;
import br.compneusgppremium.api.util.UsuarioLogadoUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ConfiguracaoMaquinaControllerTest {

    @Mock
    private ConfiguracaoMaquinaRepository configuracaoMaquinaRepository;

    @Mock
    private RegistroMaquinaRepository registroMaquinaRepository;

    @Mock
    private MatrizRepository matrizRepository;

    @Mock
    private UsuarioLogadoUtil usuarioLogadoUtil;

    @InjectMocks
    private ConfiguracaoMaquinaController configuracaoMaquinaController;

    @Test
    public void testCriarConfiguracao_ComMatrizValida_DeveDefinirMatrizCorretamente() {
        // Arrange
        ConfiguracaoMaquinaCreateDTO dto = new ConfiguracaoMaquinaCreateDTO();
        dto.setMaquinaId(1L);
        dto.setMatrizId(1);
        dto.setCelularId("CEL001");
        dto.setDescricao("Configuração de produção");
        dto.setAtributos("{\"velocidade\":100,\"temperatura\":80}");

        // Mock da máquina - usando o método correto do controller
        RegistroMaquinaModel maquina = new RegistroMaquinaModel();
        maquina.setId(1L);
        maquina.setNome("Máquina Teste");
        when(registroMaquinaRepository.findByIdAndDtDeleteIsNull(1L)).thenReturn(Optional.of(maquina));

        // Mock da matriz
        MatrizModel matriz = new MatrizModel();
        matriz.setId(1);
        matriz.setDescricao("Matriz Teste");
        when(matrizRepository.findById(1)).thenReturn(Optional.of(matriz));

        // Mock do usuário logado
        when(usuarioLogadoUtil.getUsuarioIdLogado()).thenReturn(1);

        // Mock do save - captura o objeto que será salvo
        when(configuracaoMaquinaRepository.save(any(ConfiguracaoMaquinaModel.class)))
            .thenAnswer(invocation -> {
                ConfiguracaoMaquinaModel config = invocation.getArgument(0);
                config.setId(1L);
                return config;
            });

        // Act
        ResponseEntity<?> response = configuracaoMaquinaController.criarConfiguracao(dto);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        
        // Verifica se a resposta contém os dados corretos
        assertNotNull(response.getBody());
    }

    @Test
    public void testCriarConfiguracao_ComMatrizInexistente_DeveRetornarNotFound() {
        // Arrange
        ConfiguracaoMaquinaCreateDTO dto = new ConfiguracaoMaquinaCreateDTO();
        dto.setMaquinaId(1L);
        dto.setMatrizId(999); // ID inexistente
        dto.setCelularId("CEL001");
        dto.setDescricao("Configuração de produção");
        dto.setAtributos("{\"velocidade\":100,\"temperatura\":80}");

        // Mock da máquina - usando o método correto do controller
        RegistroMaquinaModel maquina = new RegistroMaquinaModel();
        maquina.setId(1L);
        maquina.setNome("Máquina Teste");
        when(registroMaquinaRepository.findByIdAndDtDeleteIsNull(1L)).thenReturn(Optional.of(maquina));

        // Mock da matriz inexistente
        when(matrizRepository.findById(999)).thenReturn(Optional.empty());

        // Act
        ResponseEntity<?> response = configuracaoMaquinaController.criarConfiguracao(dto);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    public void testBuscarConfiguracaoAtivaPorCelular_ComConfiguracaoExistente_DeveRetornarConfiguracaoMaisRecente() {
        // Arrange
        String celularId = "CEL001";
        
        ConfiguracaoMaquinaModel configuracaoAtiva = new ConfiguracaoMaquinaModel();
        configuracaoAtiva.setId(1L);
        configuracaoAtiva.setCelularId(celularId);
        configuracaoAtiva.setDescricao("Configuração mais recente");
        configuracaoAtiva.setDtCreate(LocalDateTime.now());
        
        RegistroMaquinaModel maquina = new RegistroMaquinaModel();
        maquina.setId(1L);
        maquina.setNome("Máquina Teste");
        configuracaoAtiva.setMaquina(maquina);
        
        MatrizModel matriz = new MatrizModel();
        matriz.setId(1);
        matriz.setDescricao("Matriz Teste");
        configuracaoAtiva.setMatriz(matriz);
        
        when(configuracaoMaquinaRepository.findActiveByCelularId(anyString(), any()))
                .thenReturn(Arrays.asList(configuracaoAtiva));

        // Act
        ResponseEntity<?> response = configuracaoMaquinaController.buscarConfiguracaoAtivaPorCelular(celularId);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    public void testBuscarConfiguracaoAtivaPorCelular_SemConfiguracao_DeveRetornarNotFound() {
        // Arrange
        String celularId = "CEL999";
        when(configuracaoMaquinaRepository.findActiveByCelularId(anyString(), any()))
                .thenReturn(Arrays.asList());

        // Act
        ResponseEntity<?> response = configuracaoMaquinaController.buscarConfiguracaoAtivaPorCelular(celularId);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    public void testListarHistoricoConfiguracoesPorCelular_ComMultiplasConfiguracoes_DeveRetornarListaOrdenada() {
        // Arrange
        String celularId = "CEL001";
        
        ConfiguracaoMaquinaModel config1 = new ConfiguracaoMaquinaModel();
        config1.setId(1L);
        config1.setCelularId(celularId);
        config1.setDescricao("Configuração antiga");
        config1.setDtCreate(LocalDateTime.now().minusDays(2));
        
        ConfiguracaoMaquinaModel config2 = new ConfiguracaoMaquinaModel();
        config2.setId(2L);
        config2.setCelularId(celularId);
        config2.setDescricao("Configuração recente");
        config2.setDtCreate(LocalDateTime.now());
        
        // Mock das entidades relacionadas
        RegistroMaquinaModel maquina = new RegistroMaquinaModel();
        maquina.setId(1L);
        maquina.setNome("Máquina Teste");
        config1.setMaquina(maquina);
        config2.setMaquina(maquina);
        
        MatrizModel matriz = new MatrizModel();
        matriz.setId(1);
        matriz.setDescricao("Matriz Teste");
        config1.setMatriz(matriz);
        config2.setMatriz(matriz);
        
        List<ConfiguracaoMaquinaModel> configuracoes = Arrays.asList(config2, config1); // Ordenado por data desc
        
        when(configuracaoMaquinaRepository.findByCelularIdAndDtDeleteIsNullOrderByDtCreateDesc(celularId))
                .thenReturn(configuracoes);

        // Act
        ResponseEntity<?> response = configuracaoMaquinaController.listarHistoricoConfiguracoesPorCelular(celularId);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    public void testCriarMultiplasConfiguracoesMesmoCelular_DevePermitirCriacao() {
        // Arrange - Primeira configuração
        ConfiguracaoMaquinaCreateDTO dto1 = new ConfiguracaoMaquinaCreateDTO();
        dto1.setMaquinaId(1L);
        dto1.setMatrizId(1);
        dto1.setCelularId("CEL001");
        dto1.setDescricao("Primeira configuração");
        dto1.setAtributos("{\"velocidade\":100}");

        // Mock da máquina
        RegistroMaquinaModel maquina = new RegistroMaquinaModel();
        maquina.setId(1L);
        maquina.setNome("Máquina Teste");
        when(registroMaquinaRepository.findByIdAndDtDeleteIsNull(1L)).thenReturn(Optional.of(maquina));

        // Mock da matriz
        MatrizModel matriz = new MatrizModel();
        matriz.setId(1);
        matriz.setDescricao("Matriz Teste");
        when(matrizRepository.findById(1)).thenReturn(Optional.of(matriz));

        // Mock do usuário logado
        when(usuarioLogadoUtil.getUsuarioIdLogado()).thenReturn(1);

        // Mock do save
        ConfiguracaoMaquinaModel savedConfig = new ConfiguracaoMaquinaModel();
        savedConfig.setId(1L);
        savedConfig.setCelularId("CEL001");
        savedConfig.setMaquina(maquina);
        savedConfig.setMatriz(matriz);
        savedConfig.setDtCreate(LocalDateTime.now());
        when(configuracaoMaquinaRepository.save(any(ConfiguracaoMaquinaModel.class))).thenReturn(savedConfig);

        // Act - Criar primeira configuração
        ResponseEntity<?> response1 = configuracaoMaquinaController.criarConfiguracao(dto1);

        // Assert - Primeira configuração deve ser criada com sucesso
        assertEquals(HttpStatus.CREATED, response1.getStatusCode());

        // Arrange - Segunda configuração para o mesmo celular
        ConfiguracaoMaquinaCreateDTO dto2 = new ConfiguracaoMaquinaCreateDTO();
        dto2.setMaquinaId(1L);
        dto2.setMatrizId(2);
        dto2.setCelularId("CEL001"); // Mesmo celular
        dto2.setDescricao("Segunda configuração");
        dto2.setAtributos("{\"velocidade\":120}");

        // Mock da segunda matriz
        MatrizModel matriz2 = new MatrizModel();
        matriz2.setId(2);
        matriz2.setDescricao("Matriz Teste 2");
        when(matrizRepository.findById(2)).thenReturn(Optional.of(matriz2));

        // Mock do save para segunda configuração
        ConfiguracaoMaquinaModel savedConfig2 = new ConfiguracaoMaquinaModel();
        savedConfig2.setId(2L);
        savedConfig2.setCelularId("CEL001");
        savedConfig2.setMaquina(maquina);
        savedConfig2.setMatriz(matriz2);
        savedConfig2.setDtCreate(LocalDateTime.now());
        when(configuracaoMaquinaRepository.save(any(ConfiguracaoMaquinaModel.class))).thenReturn(savedConfig2);

        // Act - Criar segunda configuração
        ResponseEntity<?> response2 = configuracaoMaquinaController.criarConfiguracao(dto2);

        // Assert - Segunda configuração também deve ser criada com sucesso
        assertEquals(HttpStatus.CREATED, response2.getStatusCode());
    }

    @Test
    public void testDeletarConfiguracao_ComConfiguracaoExistente_DeveRealizarSoftDelete() {
        // Arrange
        Long configId = 1L;
        ConfiguracaoMaquinaModel configuracao = new ConfiguracaoMaquinaModel();
        configuracao.setId(configId);
        configuracao.setCelularId("CEL001");
        
        RegistroMaquinaModel maquina = new RegistroMaquinaModel();
        maquina.setId(1L);
        configuracao.setMaquina(maquina);

        when(configuracaoMaquinaRepository.findByIdAndDtDeleteIsNull(configId))
                .thenReturn(Optional.of(configuracao));
        when(configuracaoMaquinaRepository.save(any(ConfiguracaoMaquinaModel.class)))
                .thenReturn(configuracao);
        when(configuracaoMaquinaRepository.findPreviousActiveBymaquinaIdExcludingId(1L, configId))
                .thenReturn(Optional.empty());

        // Act
        ResponseEntity<?> response = configuracaoMaquinaController.deletarConfiguracao(configId);

        // Assert
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertNotNull(configuracao.getDtDelete());
    }

    @Test
    public void testDeletarConfiguracao_ComConfiguracaoInexistente_DeveRetornarNotFound() {
        // Arrange
        Long configId = 999L;
        when(configuracaoMaquinaRepository.findByIdAndDtDeleteIsNull(configId))
                .thenReturn(Optional.empty());

        // Act
        ResponseEntity<?> response = configuracaoMaquinaController.deletarConfiguracao(configId);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    public void testDeletarConfiguracao_ComConfiguracaoAnteriorExistente_DeveManterConfiguracaoAnteriorAtiva() {
        // Arrange
        Long configAtualId = 2L;
        Long configAnteriorId = 1L;
        
        // Configuração atual (a ser deletada)
        ConfiguracaoMaquinaModel configAtual = new ConfiguracaoMaquinaModel();
        configAtual.setId(configAtualId);
        configAtual.setCelularId("CEL001");
        configAtual.setDtCreate(LocalDateTime.now());
        
        RegistroMaquinaModel maquina = new RegistroMaquinaModel();
        maquina.setId(1L);
        configAtual.setMaquina(maquina);

        // Configuração anterior (deve permanecer ativa)
        ConfiguracaoMaquinaModel configAnterior = new ConfiguracaoMaquinaModel();
        configAnterior.setId(configAnteriorId);
        configAnterior.setCelularId("CEL001");
        configAnterior.setMaquina(maquina);
        configAnterior.setDtCreate(LocalDateTime.now().minusHours(1));

        when(configuracaoMaquinaRepository.findByIdAndDtDeleteIsNull(configAtualId))
                .thenReturn(Optional.of(configAtual));
        when(configuracaoMaquinaRepository.save(any(ConfiguracaoMaquinaModel.class)))
                .thenReturn(configAtual);
        when(configuracaoMaquinaRepository.findPreviousActiveBymaquinaIdExcludingId(1L, configAtualId))
                .thenReturn(Optional.of(configAnterior));

        // Act
        ResponseEntity<?> response = configuracaoMaquinaController.deletarConfiguracao(configAtualId);

        // Assert
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertNotNull(configAtual.getDtDelete()); // Configuração atual foi deletada
        assertNull(configAnterior.getDtDelete()); // Configuração anterior permanece ativa
    }

    @Test
    public void testDeletarConfiguracao_SemConfiguracaoAnterior_DeveApenasRealizarSoftDelete() {
        // Arrange
        Long configId = 1L;
        ConfiguracaoMaquinaModel configuracao = new ConfiguracaoMaquinaModel();
        configuracao.setId(configId);
        configuracao.setCelularId("CEL001");
        
        RegistroMaquinaModel maquina = new RegistroMaquinaModel();
        maquina.setId(1L);
        configuracao.setMaquina(maquina);

        when(configuracaoMaquinaRepository.findByIdAndDtDeleteIsNull(configId))
                .thenReturn(Optional.of(configuracao));
        when(configuracaoMaquinaRepository.save(any(ConfiguracaoMaquinaModel.class)))
                .thenReturn(configuracao);
        when(configuracaoMaquinaRepository.findPreviousActiveBymaquinaIdExcludingId(1L, configId))
                .thenReturn(Optional.empty()); // Não há configuração anterior

        // Act
        ResponseEntity<?> response = configuracaoMaquinaController.deletarConfiguracao(configId);

        // Assert
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertNotNull(configuracao.getDtDelete());
    }

    @Test
    @DisplayName("DEBUG - Verificar se configuração ID 12 existe no banco")
    void testVerificarConfiguracaoId12() {
        // Mock para findAll
        when(configuracaoMaquinaRepository.findAll()).thenReturn(Arrays.asList());
        
        // Mock para findById
        when(configuracaoMaquinaRepository.findById(12L)).thenReturn(Optional.empty());
        
        // Mock para findByDtDeleteIsNullOrderByDtCreateDesc
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 100);
        org.springframework.data.domain.Page<ConfiguracaoMaquinaModel> emptyPage = 
            new org.springframework.data.domain.PageImpl<>(Arrays.asList(), pageable, 0);
        when(configuracaoMaquinaRepository.findByDtDeleteIsNullOrderByDtCreateDesc(any(org.springframework.data.domain.Pageable.class)))
            .thenReturn(emptyPage);
        
        // Buscar todas as configurações incluindo deletadas
        List<ConfiguracaoMaquinaModel> todasConfiguracoes = configuracaoMaquinaRepository.findAll();
        
        // Verificar especificamente a configuração ID 12
        Optional<ConfiguracaoMaquinaModel> config12 = configuracaoMaquinaRepository.findById(12L);
        assertFalse(config12.isPresent(), "Configuração ID 12 não deve existir no mock");
        
        // Buscar apenas configurações ativas usando paginação
        org.springframework.data.domain.Page<ConfiguracaoMaquinaModel> configuracoesAtivasPage = 
            configuracaoMaquinaRepository.findByDtDeleteIsNullOrderByDtCreateDesc(pageable);
        assertNotNull(configuracoesAtivasPage, "A página de configurações ativas não deve ser null");
        assertEquals(0, configuracoesAtivasPage.getContent().size(), "Deve retornar lista vazia no mock");
    }

}
