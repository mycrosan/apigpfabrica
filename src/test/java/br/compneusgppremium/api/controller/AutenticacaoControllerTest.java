package br.compneusgppremium.api.controller;

import br.compneusgppremium.api.controller.model.PerfilModel;
import br.compneusgppremium.api.controller.model.UsuarioModel;
import br.compneusgppremium.api.repository.PerfilRepository;
import br.compneusgppremium.api.repository.UsuarioRepository;
import br.compneusgppremium.api.service.TokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.Arrays;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class AutenticacaoControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PerfilRepository perfilRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private ObjectMapper objectMapper;

    private UsuarioModel usuarioTeste;
    private PerfilModel perfilTeste;

    @BeforeEach
    void setUp() {
        // Configurar MockMvc
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        // Criar perfil de teste
        perfilTeste = new PerfilModel();
        perfilTeste.setDescricao("ROLE_USER");
        perfilTeste = perfilRepository.save(perfilTeste);

        // Criar usuário de teste
        usuarioTeste = new UsuarioModel();
        usuarioTeste.setLogin("teste@teste.com");
        usuarioTeste.setSenha(passwordEncoder.encode("123456"));
        usuarioTeste.setNome("Usuario Teste");
        usuarioTeste.getPerfil().add(perfilTeste);
        usuarioTeste = usuarioRepository.save(usuarioTeste);
    }

    @Test
    void deveAutenticarComCredenciaisValidas() throws Exception {
        String loginJson = "{\n" +
                "    \"login\": \"teste@teste.com\",\n" +
                "    \"senha\": \"123456\"\n" +
                "}";

        mockMvc.perform(post("/api/auth")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.status").value(true));
    }

    @Test
    void deveRetornarForbiddenComCredenciaisInvalidas() throws Exception {
        String loginJson = "{\n" +
                "    \"login\": \"teste@teste.com\",\n" +
                "    \"senha\": \"senhaerrada\"\n" +
                "}";

        mockMvc.perform(post("/api/auth")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginJson))
                .andExpect(status().isForbidden())
                .andExpect(content().string("Verifique seu usuário e senha"));
    }

    @Test
    void deveRetornarForbiddenComUsuarioInexistente() throws Exception {
        String loginJson = "{\n" +
                "    \"login\": \"inexistente@teste.com\",\n" +
                "    \"senha\": \"123456\"\n" +
                "}";

        mockMvc.perform(post("/api/auth")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginJson))
                .andExpect(status().isForbidden())
                .andExpect(content().string("Verifique seu usuário e senha"));
    }

    @Test
    void deveRetornarBadRequestComDadosVazios() throws Exception {
        String loginJson = "{\n" +
                "    \"login\": \"\",\n" +
                "    \"senha\": \"\"\n" +
                "}";

        mockMvc.perform(post("/api/auth")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveRetornarBadRequestComJsonInvalido() throws Exception {
        String loginJson = "{\n" +
                "    \"login\": \"teste@teste.com\"\n" +
                "}";

        mockMvc.perform(post("/api/auth")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginJson))
                .andExpect(status().isBadRequest());
    }
}