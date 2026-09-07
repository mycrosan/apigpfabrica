package br.compneusgppremium.api.leitura.service;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.assertj.core.api.Assertions.*;
class ArquivosLeituraServiceTest {
    @TempDir Path diretorio;
    @Test
    void gravaIntegralmenteEIdentificaOrfaoSemExporTemporario() throws Exception {
        var arquivos = new ArquivosLeituraService(diretorio.toString());
        String nome = arquivos.salvar(new byte[] {1, 2, 3});
        assertThat(arquivos.ler(nome)).containsExactly(1, 2, 3);
        assertThat(arquivos.listarAnteriores(Instant.now().plusSeconds(1))).containsExactly(nome);
        try (var encontrados = Files.list(diretorio)) { assertThat(encontrados.toList()).hasSize(1); }
        arquivos.removerOrfao(nome); assertThat(Files.exists(diretorio.resolve(nome))).isFalse();
    }
    @Test
    void naoSegueLinkSimbolicoNemAceitaCaminhoExterno() throws Exception {
        var arquivos = new ArquivosLeituraService(diretorio.toString());
        Path alvo = Files.write(diretorio.resolve("fora.txt"), new byte[] {1});
        String nome = java.util.UUID.randomUUID() + ".foto";
        Files.createSymbolicLink(diretorio.resolve(nome), alvo);
        assertThatThrownBy(() -> arquivos.ler(nome)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> arquivos.removerOrfao("../fora.txt")).isInstanceOf(RuntimeException.class);
        assertThat(Files.exists(alvo)).isTrue();
    }
    @Test
    void falhaDeArmazenamentoNaoRetornaIdentificadorDeSucesso() throws Exception {
        Path arquivo = Files.write(diretorio.resolve("nao-e-pasta"), new byte[] {1});
        var arquivos = new ArquivosLeituraService(arquivo.toString());
        assertThatThrownBy(() -> arquivos.salvar(new byte[] {2})).isInstanceOf(IllegalStateException.class);
        assertThat(Files.readAllBytes(arquivo)).containsExactly(1);
    }
}
