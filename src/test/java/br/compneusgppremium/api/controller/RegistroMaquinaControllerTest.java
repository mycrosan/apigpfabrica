package br.compneusgppremium.api.controller;

import br.compneusgppremium.api.controller.dto.RegistroMaquinaCreateDTO;
import br.compneusgppremium.api.controller.model.RegistroMaquinaModel;
import br.compneusgppremium.api.controller.model.StatusMaquina;
import br.compneusgppremium.api.repository.RegistroMaquinaRepository;
import br.compneusgppremium.api.repository.UsuarioRepository;
import br.compneusgppremium.api.service.AutenticacaoService;
import br.compneusgppremium.api.service.TokenService;
import br.compneusgppremium.api.util.UsuarioLogadoUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class RegistroMaquinaControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @MockBean
    private RegistroMaquinaRepository registroMaquinaRepository;

    @MockBean
    private UsuarioRepository usuarioRepository;

    @MockBean
    private UsuarioLogadoUtil usuarioLogadoUtil;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    public void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    public void testRegistrarMaquina_NumeroSerieJaExiste_DeveRetornarBadRequest() throws Exception {
        // Arrange
        RegistroMaquinaCreateDTO dto = new RegistroMaquinaCreateDTO();
        dto.setNome("Máquina Teste");
        dto.setNumeroSerie("SN-001-2025");
        dto.setDescricao("Descrição teste");

        // Simular que já existe uma máquina com esse número de série
        when(registroMaquinaRepository.existsByNumeroSerieAndDtDeleteIsNull("SN-001-2025"))
                .thenReturn(true);

        // Act & Assert
        mockMvc.perform(post("/api/registro-maquina")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Número de série já existe"))
                .andExpect(jsonPath("$.error").value("Já existe uma máquina registrada com o número de série: SN-001-2025"));
    }

    @Test
    public void testRegistrarMaquina_DataIntegrityViolationException_DeveRetornarBadRequest() throws Exception {
        // Arrange
        RegistroMaquinaCreateDTO dto = new RegistroMaquinaCreateDTO();
        dto.setNome("Máquina Teste");
        dto.setNumeroSerie("SN-001-2025");
        dto.setDescricao("Descrição teste");

        // Simular que não existe máquina com esse número de série na verificação prévia
        when(registroMaquinaRepository.existsByNumeroSerieAndDtDeleteIsNull("SN-001-2025"))
                .thenReturn(false);

        // Simular DataIntegrityViolationException no save
        when(registroMaquinaRepository.save(any(RegistroMaquinaModel.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate entry 'SN-001-2025' for key 'numero_serie'"));

        // Act & Assert
        mockMvc.perform(post("/api/registro-maquina")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Número de série já existe"))
                .andExpect(jsonPath("$.error").value("O número de série 'SN-001-2025' já está sendo usado por outra máquina."));
    }
}