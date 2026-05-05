package br.compneusgppremium.api.repository;

import br.compneusgppremium.api.controller.model.PerfilModel;
import br.compneusgppremium.api.controller.model.ProducaoModel;
import br.compneusgppremium.api.controller.model.UsuarioModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
public class ProducaoRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ProducaoRepository producaoRepository;

    private BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private UsuarioModel usuarioTeste;
    private PerfilModel perfilTeste;
    private ProducaoModel producaoTeste;

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

        // Criar produção de teste
        producaoTeste = new ProducaoModel();
        producaoTeste.setMedida_pneu_raspado(10.5);
        producaoTeste.setCriadoPor(usuarioTeste);
        producaoTeste.setDt_create(new Date());
        producaoTeste.setUuid(UUID.randomUUID());
        producaoTeste = entityManager.persistAndFlush(producaoTeste);
    }

    @Test
    void deveSalvarProducaoComSucesso() {
        // Given
        ProducaoModel novaProducao = new ProducaoModel();
        novaProducao.setMedida_pneu_raspado(15.0);
        novaProducao.setCriadoPor(usuarioTeste);
        novaProducao.setDt_create(new Date());
        novaProducao.setUuid(UUID.randomUUID());

        // When
        ProducaoModel producaoSalva = producaoRepository.save(novaProducao);

        // Then
        assertThat(producaoSalva.getId()).isNotNull();
        assertThat(producaoSalva.getMedida_pneu_raspado()).isEqualTo(15.0);
        assertThat(producaoSalva.getCriadoPor()).isEqualTo(usuarioTeste);
    }

    @Test
    void deveBuscarProducaoPorId() {
        // When
        Optional<ProducaoModel> producaoEncontrada = producaoRepository.findById(producaoTeste.getId());

        // Then
        assertThat(producaoEncontrada).isPresent();
        assertThat(producaoEncontrada.get().getMedida_pneu_raspado()).isEqualTo(10.5);
        assertThat(producaoEncontrada.get().getCriadoPor()).isEqualTo(usuarioTeste);
    }

    @Test
    void deveRetornarVazioParaIdInexistente() {
        // When
        Optional<ProducaoModel> producaoEncontrada = producaoRepository.findById(999999);

        // Then
        assertThat(producaoEncontrada).isEmpty();
    }

    @Test
    void deveListarTodasProducoes() {
        // When
        Iterable<ProducaoModel> producoes = producaoRepository.findAll();

        // Then
        assertThat(producoes).hasSize(1);
        assertThat(producoes).extracting(ProducaoModel::getMedida_pneu_raspado).contains(10.5);
    }

    @Test
    void deveDeletarProducao() {
        // When
        producaoRepository.delete(producaoTeste);
        Optional<ProducaoModel> producaoEncontrada = producaoRepository.findById(producaoTeste.getId());

        // Then
        assertThat(producaoEncontrada).isEmpty();
    }

    @Test
    void deveAtualizarProducao() {
        // Given
        producaoTeste.setMedida_pneu_raspado(20.0);
        producaoTeste.setDt_update(new Date());

        // When
        ProducaoModel producaoAtualizada = producaoRepository.save(producaoTeste);

        // Then
        assertThat(producaoAtualizada.getMedida_pneu_raspado()).isEqualTo(20.0);
        assertThat(producaoAtualizada.getDt_update()).isNotNull();
        assertThat(producaoAtualizada.getId()).isEqualTo(producaoTeste.getId());
    }

    @Test
    void deveContarProducoes() {
        // When
        long count = producaoRepository.count();

        // Then
        assertThat(count).isEqualTo(1);
    }

    @Test
    void deveVerificarExistenciaPorId() {
        // When
        boolean existe = producaoRepository.existsById(producaoTeste.getId());
        boolean naoExiste = producaoRepository.existsById(999999);

        // Then
        assertThat(existe).isTrue();
        assertThat(naoExiste).isFalse();
    }
}