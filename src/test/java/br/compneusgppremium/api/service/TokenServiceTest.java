package br.compneusgppremium.api.service;

import br.compneusgppremium.api.controller.model.UsuarioModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenServiceTest {

    @InjectMocks
    private TokenService tokenService;

    @Mock
    private Authentication authentication;

    private UsuarioModel usuario;

    @BeforeEach
    void setUp() {
        // Configurar propriedades do JWT para teste
        ReflectionTestUtils.setField(tokenService, "secret", "testSecret123456789012345678901234567890");
        ReflectionTestUtils.setField(tokenService, "expiration", "86400000"); // 24 horas

        // Criar usuário de teste
        usuario = new UsuarioModel();
        usuario.setId(1L);
        usuario.setNome("Teste Usuario");
        usuario.setLogin("teste@teste.com");
    }

    @Test
    void deveGerarTokenComSucesso() {
        // Arrange
        when(authentication.getPrincipal()).thenReturn(usuario);

        // Act
        String token = tokenService.gerarToken(authentication);

        // Assert
        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertTrue(token.contains("."));
    }

    @Test
    void deveValidarTokenValido() {
        // Arrange
        when(authentication.getPrincipal()).thenReturn(usuario);
        String token = tokenService.gerarToken(authentication);

        // Act
        boolean isValid = tokenService.isTokenValido(token);

        // Assert
        assertTrue(isValid);
    }

    @Test
    void deveRejeitarTokenInvalido() {
        // Arrange
        String tokenInvalido = "token.invalido.aqui";

        // Act
        boolean isValid = tokenService.isTokenValido(tokenInvalido);

        // Assert
        assertFalse(isValid);
    }

    @Test
    void deveRejeitarTokenNulo() {
        // Act
        boolean isValid = tokenService.isTokenValido(null);

        // Assert
        assertFalse(isValid);
    }

    @Test
    void deveRejeitarTokenVazio() {
        // Act
        boolean isValid = tokenService.isTokenValido("");

        // Assert
        assertFalse(isValid);
    }

    @Test
    void deveExtrairIdUsuarioDoToken() {
        // Arrange
        when(authentication.getPrincipal()).thenReturn(usuario);
        String token = tokenService.gerarToken(authentication);

        // Act
        Long userId = tokenService.getIdUsuario(token);

        // Assert
        assertEquals(usuario.getId(), userId);
    }

    @Test
    void deveLancarExcecaoAoExtrairIdDeTokenInvalido() {
        // Arrange
        String tokenInvalido = "token.invalido.aqui";

        // Act & Assert
        assertThrows(Exception.class, () -> {
            tokenService.getIdUsuario(tokenInvalido);
        });
    }
}