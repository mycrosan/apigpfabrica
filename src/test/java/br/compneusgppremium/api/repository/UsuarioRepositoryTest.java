package br.compneusgppremium.api.repository;

import br.compneusgppremium.api.controller.model.PerfilModel;
import br.compneusgppremium.api.controller.model.UsuarioModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Transactional
@Rollback
public class UsuarioRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PerfilRepository perfilRepository;

    private BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private UsuarioModel usuarioTeste;
    private PerfilModel perfilTeste;

    @BeforeEach
    void setUp() {
        // Criar perfil de teste
        perfilTeste = new PerfilModel();
        perfilTeste.setDescricao("ROLE_USER");
        perfilTeste = entityManager.persistAndFlush(perfilTeste);

        // Criar usuário de teste
        usuarioTeste = new UsuarioModel();
        usuarioTeste.setLogin("teste@teste.com");
        usuarioTeste.setSenha(passwordEncoder.encode("123456"));
        usuarioTeste.setNome("Usuario Teste");
        usuarioTeste.getPerfil().add(perfilTeste);
        usuarioTeste = entityManager.persistAndFlush(usuarioTeste);
    }

    @Test
    void deveBuscarUsuarioPorLogin() {
        // When
        Optional<UsuarioModel> usuarioEncontrado = usuarioRepository.findByLogin("teste@teste.com");

        // Then
        assertThat(usuarioEncontrado).isPresent();
        assertThat(usuarioEncontrado.get().getNome()).isEqualTo("Usuario Teste");
        assertThat(usuarioEncontrado.get().getLogin()).isEqualTo("teste@teste.com");
    }

    @Test
    void deveRetornarVazioParaLoginInexistente() {
        // When
        Optional<UsuarioModel> usuarioEncontrado = usuarioRepository.findByLogin("inexistente@teste.com");

        // Then
        assertThat(usuarioEncontrado).isEmpty();
    }

    @Test
    void deveSalvarUsuarioComSucesso() {
        // Given
        UsuarioModel novoUsuario = new UsuarioModel();
        novoUsuario.setLogin("novo@teste.com");
        novoUsuario.setSenha(passwordEncoder.encode("senha123"));
        novoUsuario.setNome("Novo Usuario");
        novoUsuario.getPerfil().add(perfilTeste);

        // When
        UsuarioModel usuarioSalvo = usuarioRepository.save(novoUsuario);

        // Then
        assertThat(usuarioSalvo.getId()).isNotNull();
        assertThat(usuarioSalvo.getLogin()).isEqualTo("novo@teste.com");
        assertThat(usuarioSalvo.getNome()).isEqualTo("Novo Usuario");
    }

    @Test
    void deveListarTodosUsuarios() {
        // When
        Iterable<UsuarioModel> usuarios = usuarioRepository.findAll();

        // Then
        assertThat(usuarios).hasSize(2); // Usuario base do data.sql + usuario do setUp
        assertThat(usuarios).extracting(UsuarioModel::getLogin)
            .contains("teste@teste.com", "base@teste.com");
    }

    @Test
    void deveDeletarUsuario() {
        // When
        usuarioRepository.delete(usuarioTeste);
        Optional<UsuarioModel> usuarioEncontrado = usuarioRepository.findByLogin("teste@teste.com");

        // Then
        assertThat(usuarioEncontrado).isEmpty();
    }

    @Test
    void deveAtualizarUsuario() {
        // Given
        usuarioTeste.setNome("Nome Atualizado");

        // When
        UsuarioModel usuarioAtualizado = usuarioRepository.save(usuarioTeste);

        // Then
        assertThat(usuarioAtualizado.getNome()).isEqualTo("Nome Atualizado");
        assertThat(usuarioAtualizado.getId()).isEqualTo(usuarioTeste.getId());
    }
}