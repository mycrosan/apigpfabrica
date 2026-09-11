package br.compneusgppremium.api.ocr;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class DiagnosticoOcrTest {
    @TempDir Path temporario;
    private final ObjectMapper json = new ObjectMapper();
    private final Map<String, String> corpo = Map.of("campo", "DOT", "foto_base64", "AQID");

    @Test void exportaCorpoReutilizavelSemCredenciais() throws Exception {
        new DiagnosticoOcr(json, true, temporario.toString()).registrar(corpo);
        try (var arquivos = Files.list(temporario)) {
            var lista = arquivos.toList();
            assertEquals(1, lista.size());
            assertEquals(json.valueToTree(corpo), json.readTree(lista.get(0).toFile()));
            assertEquals("rw-------", java.nio.file.attribute.PosixFilePermissions.toString(
                    Files.getPosixFilePermissions(lista.get(0))));
        }
    }

    @Test void desativadoNaoCriaDiretorio() {
        Path destino = temporario.resolve("desativado");
        new DiagnosticoOcr(json, false, destino.toString()).registrar(corpo);
        assertFalse(Files.exists(destino));
    }

    @Test void exportaFotoComMesmosBytesDoJsonEmPngEJpeg() throws Exception {
        for (String formato : java.util.List.of("png", "jpg")) {
            var bytes = new java.io.ByteArrayOutputStream();
            var imagem = new java.awt.image.BufferedImage(2, 2, java.awt.image.BufferedImage.TYPE_INT_RGB);
            assertTrue(javax.imageio.ImageIO.write(imagem, formato, bytes));
            Path destino = temporario.resolve(formato);
            var requisicao = Map.of("campo", "DOT", "foto_base64",
                    java.util.Base64.getEncoder().encodeToString(bytes.toByteArray()));
            new DiagnosticoOcr(json, true, destino.toString()).registrar(requisicao);
            try (var arquivos = Files.list(destino)) {
                var lista = arquivos.toList();
                assertEquals(2, lista.size());
                Path arquivoJson = lista.stream().filter(p -> p.toString().endsWith(".json")).findFirst().orElseThrow();
                Path foto = arquivoJson.resolveSibling(arquivoJson.getFileName().toString().replace(".json", "." + formato));
                assertArrayEquals(java.util.Base64.getDecoder().decode(
                        json.readTree(arquivoJson.toFile()).get("foto_base64").asText()), Files.readAllBytes(foto));
                assertEquals("rw-------", java.nio.file.attribute.PosixFilePermissions.toString(
                        Files.getPosixFilePermissions(foto)));
            }
        }
    }

    @Test void falhaDeDiscoNaoImpedeLeitura() throws Exception {
        Path arquivo = Files.createFile(temporario.resolve("arquivo"));
        assertDoesNotThrow(() -> new DiagnosticoOcr(json, true, arquivo.toString()).registrar(corpo));
    }
}
