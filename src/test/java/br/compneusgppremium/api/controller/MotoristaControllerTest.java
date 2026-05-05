package br.compneusgppremium.api.controller;

import br.compneusgppremium.api.controller.dto.MotoristaCreateDTO;
import br.compneusgppremium.api.controller.dto.MotoristaUpdateDTO;
import br.compneusgppremium.api.controller.model.PerfilModel;
import br.compneusgppremium.api.controller.model.UsuarioModel;
import br.compneusgppremium.api.repository.MotoristaRepository;
import br.compneusgppremium.api.repository.PerfilRepository;
import br.compneusgppremium.api.repository.UsuarioRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class MotoristaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PerfilRepository perfilRepository;

    @Autowired
    private MotoristaRepository motoristaRepository;

    @Autowired
    private BCryptPasswordEncoder encoder;

    private UsuarioModel usuarioMotorista;

    @BeforeEach
    void setUp() {
        motoristaRepository.deleteAll();
        usuarioRepository.deleteAll();
        perfilRepository.deleteAll();

        PerfilModel perfil = new PerfilModel();
        perfil.setDescricao("MOTORISTA");
        perfil = perfilRepository.save(perfil);

        UsuarioModel u = new UsuarioModel();
        u.setNome("Motorista Usuário");
        u.setLogin("motorista@teste.com");
        u.setPassword(encoder.encode("123456"));
        u.setPerfil(Collections.singletonList(perfil));
        usuarioMotorista = usuarioRepository.save(u);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deveCriarMotorista() throws Exception {
        MotoristaCreateDTO dto = new MotoristaCreateDTO();
        dto.setNome("João da Silva");
        dto.setCpf("12345678901");
        dto.setTelefone("11988887777");
        dto.setPlacaVeiculo("ABC1234");
        dto.setObservacoes("Disponível em horário comercial");
        dto.setUsuarioId(usuarioMotorista.getId().intValue());

        mockMvc.perform(post("/api/motoristas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.nome").value("João da Silva"))
                .andExpect(jsonPath("$.cpf").value("12345678901"))
                .andExpect(jsonPath("$.usuarioId").value(usuarioMotorista.getId().intValue()))
                .andExpect(jsonPath("$.ativo").value(true));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deveListarMotoristasAtivos() throws Exception {
        MotoristaCreateDTO dto = new MotoristaCreateDTO();
        dto.setNome("Maria Souza");
        dto.setCpf("98765432100");
        dto.setUsuarioId(usuarioMotorista.getId().intValue());

        mockMvc.perform(post("/api/motoristas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/motoristas"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].nome").value("Maria Souza"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deveConsultarMotoristaPorId() throws Exception {
        MotoristaCreateDTO dto = new MotoristaCreateDTO();
        dto.setNome("Carlos Lima");
        dto.setCpf("11122233344");
        dto.setUsuarioId(usuarioMotorista.getId().intValue());

        String response = mockMvc.perform(post("/api/motoristas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Integer id = objectMapper.readTree(response).get("id").asInt();

        mockMvc.perform(get("/api/motoristas/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Carlos Lima"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deveAtualizarMotorista() throws Exception {
        MotoristaCreateDTO dto = new MotoristaCreateDTO();
        dto.setNome("Ana Paula");
        dto.setCpf("22233344455");
        dto.setUsuarioId(usuarioMotorista.getId().intValue());

        String response = mockMvc.perform(post("/api/motoristas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Integer id = objectMapper.readTree(response).get("id").asInt();

        MotoristaUpdateDTO update = new MotoristaUpdateDTO();
        update.setNome("Ana Paula Atualizada");
        update.setTelefone("11977776666");
        update.setPlacaVeiculo("XYZ9876");
        update.setObservacoes("Atualizado");

        mockMvc.perform(put("/api/motoristas/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Ana Paula Atualizada"))
                .andExpect(jsonPath("$.placaVeiculo").value("XYZ9876"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deveInativarMotorista() throws Exception {
        MotoristaCreateDTO dto = new MotoristaCreateDTO();
        dto.setNome("Pedro Alves");
        dto.setCpf("33344455566");
        dto.setUsuarioId(usuarioMotorista.getId().intValue());

        String response = mockMvc.perform(post("/api/motoristas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Integer id = objectMapper.readTree(response).get("id").asInt();

        mockMvc.perform(patch("/api/motoristas/{id}/inativar", id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/motoristas/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo").value(false));
    }
}

