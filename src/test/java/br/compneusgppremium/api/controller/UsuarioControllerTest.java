package br.compneusgppremium.api.controller;

import br.compneusgppremium.api.controller.model.PerfilModel;
import br.compneusgppremium.api.controller.model.UsuarioModel;
import br.compneusgppremium.api.repository.PerfilRepository;
import br.compneusgppremium.api.repository.UsuarioRepository;
import br.compneusgppremium.api.util.UsuarioLogadoUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class UsuarioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PerfilRepository perfilRepository;

    @MockBean
    private UsuarioLogadoUtil usuarioLogadoUtil;

    @Autowired
    private BCryptPasswordEncoder encoder;

    private UsuarioModel usuarioTeste;
    private PerfilModel perfilTeste;

    @BeforeEach
    void setUp() {
        // Limpar dados existentes para garantir isolamento
        usuarioRepository.deleteAll();
        perfilRepository.deleteAll();
        
        // Criar perfil de teste
        perfilTeste = new PerfilModel();
        perfilTeste.setDescricao("ADMIN");
        perfilTeste = perfilRepository.save(perfilTeste);

        // Criar usuário de teste
        usuarioTeste = new UsuarioModel();
        usuarioTeste.setNome("Usuario Teste");
        usuarioTeste.setLogin("teste@teste.com");
        usuarioTeste.setPassword(encoder.encode("123456"));
        usuarioTeste.setPerfil(Arrays.asList(perfilTeste));
        usuarioTeste = usuarioRepository.save(usuarioTeste);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deveListarTodosUsuarios() throws Exception {
        mockMvc.perform(get("/api/usuario"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].nome").value("Usuario Teste"))
                .andExpect(jsonPath("$[0].login").value("teste@teste.com"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deveBuscarUsuarioPorId() throws Exception {
        mockMvc.perform(get("/api/usuario/{id}", usuarioTeste.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.nome").value("Usuario Teste"))
                .andExpect(jsonPath("$.login").value("teste@teste.com"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deveRetornar404ParaUsuarioInexistente() throws Exception {
        mockMvc.perform(get("/api/usuario/{id}", 99999L))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deveObterUsuarioLogado() throws Exception {
        // Arrange
        when(usuarioLogadoUtil.getUsuarioIdLogado()).thenReturn(usuarioTeste.getId());

        // Act & Assert
        mockMvc.perform(get("/api/usuario/me"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.nome").value("Usuario Teste"))
                .andExpect(jsonPath("$.login").value("teste@teste.com"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deveRetornar401QuandoUsuarioLogadoNaoEncontrado() throws Exception {
        // Arrange
        when(usuarioLogadoUtil.getUsuarioIdLogado()).thenThrow(new RuntimeException("Usuário não autenticado"));

        // Act & Assert
        mockMvc.perform(get("/api/usuario/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deveCriarNovoUsuario() throws Exception {
        // Arrange
        String usuarioJson = "{"
                + "\"nome\":\"Novo Usuario\","
                + "\"login\":\"novo@teste.com\","
                + "\"senha\":\"123456\","
                + "\"perfil\":[{\"id\":" + perfilTeste.getId() + "}]"
                + "}";

        // Act & Assert
        mockMvc.perform(post("/api/usuario")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(usuarioJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("Novo Usuario"))
                .andExpect(jsonPath("$.login").value("novo@teste.com"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deveRejeitarUsuarioComLoginExistente() throws Exception {
        // Arrange
        String usuarioJson = "{"
                + "\"nome\":\"Usuario Duplicado\","
                + "\"login\":\"" + usuarioTeste.getLogin() + "\","
                + "\"senha\":\"123456\","
                + "\"perfil\":[{\"id\":" + perfilTeste.getId() + "}]"
                + "}";

        // Act & Assert
        mockMvc.perform(post("/api/usuario")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(usuarioJson))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Login já existe!"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deveAtualizarUsuario() throws Exception {
        // Arrange
        usuarioTeste.setNome("Nome Atualizado");
        usuarioTeste.setLogin("atualizado@teste.com");

        // Act & Assert
        mockMvc.perform(put("/api/usuario/{id}", usuarioTeste.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(usuarioTeste)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Nome Atualizado"))
                .andExpect(jsonPath("$.login").value("atualizado@teste.com"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deveRetornar404AoAtualizarUsuarioInexistente() throws Exception {
        // Arrange
        UsuarioModel usuarioInexistente = new UsuarioModel();
        usuarioInexistente.setNome("Usuario Inexistente");
        usuarioInexistente.setLogin("inexistente@teste.com");

        // Act & Assert
        mockMvc.perform(put("/api/usuario/{id}", 99999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(usuarioInexistente)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deveDeletarUsuario() throws Exception {
        mockMvc.perform(delete("/api/usuario/{id}", usuarioTeste.getId()))
                .andExpect(status().isOk());

        // Verificar se foi deletado
        mockMvc.perform(get("/api/usuario/{id}", usuarioTeste.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deveRetornar404AoDeletarUsuarioInexistente() throws Exception {
        mockMvc.perform(delete("/api/usuario/{id}", 99999L))
                .andExpect(status().isNotFound());
    }
}