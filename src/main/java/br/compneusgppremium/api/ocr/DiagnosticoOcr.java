package br.compneusgppremium.api.ocr;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Base64;
import java.util.Map;
import javax.imageio.ImageIO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

/** Exportação local opcional da requisição e da imagem enviada ao OCR. */
@Component
@Slf4j
public class DiagnosticoOcr {
    private final ObjectMapper json;
    private final boolean salvarJson;
    private final Path diretorio;

    public DiagnosticoOcr(ObjectMapper json,
            @Value("${pneus.ocr.diagnostico.salvar-json:false}") boolean salvarJson,
            @Value("${pneus.ocr.diagnostico.diretorio:diagnostico-ocr}") String diretorio) {
        this.json = json;
        this.salvarJson = salvarJson;
        this.diretorio = Path.of(diretorio).toAbsolutePath().normalize();
    }

    public void registrar(Map<String, String> corpo) {
        String base64 = corpo.get("foto_base64");
        byte[] imagem = Base64.getDecoder().decode(base64);
        log.info("ocr corpo_preparado id={} campo={} imagemBytes={} base64Caracteres={} imagemMd5={} exportacaoJson={}",
                MDC.get("leituraId"), corpo.get("campo"), imagem.length, base64.length(),
                DigestUtils.md5DigestAsHex(imagem), salvarJson);
        if (!salvarJson) return;
        try {
            Files.createDirectories(diretorio, PosixFilePermissions.asFileAttribute(
                    PosixFilePermissions.fromString("rwx------")));
            Path arquivo = Files.createTempFile(diretorio, "requisicao-", ".json",
                    PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
            json.writeValue(arquivo.toFile(), corpo);
            log.info("ocr json_salvo id={} arquivo={} jsonBytes={}",
                    MDC.get("leituraId"), arquivo, Files.size(arquivo));
            salvarFoto(arquivo, imagem);
        } catch (IOException | UnsupportedOperationException | SecurityException erro) {
            // Diagnóstico não deve impedir a leitura; nunca registrar o corpo na falha.
            log.warn("ocr json_nao_salvo id={} tipo={}", MDC.get("leituraId"), erro.getClass().getSimpleName());
        }
    }

    private void salvarFoto(Path arquivoJson, byte[] imagem) {
        try {
            String nome = arquivoJson.getFileName().toString().replaceFirst("\\.json$", "");
            Path foto = arquivoJson.resolveSibling(nome + "." + formatoImagem(imagem));
            Files.createFile(foto, PosixFilePermissions.asFileAttribute(
                    PosixFilePermissions.fromString("rw-------")));
            // Preserva os bytes enviados, sem recompressão, rotação ou outro pré-processamento.
            Files.write(foto, imagem);
            log.info("ocr foto_salva id={} arquivo={} imagemBytes={}", MDC.get("leituraId"), foto, imagem.length);
        } catch (IOException | UnsupportedOperationException | SecurityException erro) {
            log.warn("ocr foto_nao_salva id={} tipo={}", MDC.get("leituraId"), erro.getClass().getSimpleName());
        }
    }

    private String formatoImagem(byte[] imagem) throws IOException {
        try (var stream = ImageIO.createImageInputStream(new ByteArrayInputStream(imagem))) {
            var leitores = ImageIO.getImageReaders(stream);
            if (!leitores.hasNext()) throw new IOException("Formato de imagem não reconhecido.");
            var leitor = leitores.next();
            try {
                return switch (leitor.getFormatName().toUpperCase(java.util.Locale.ROOT)) {
                    case "JPEG", "JPG" -> "jpg";
                    case "PNG" -> "png";
                    default -> throw new IOException("Formato de imagem não suportado.");
                };
            } finally {
                leitor.dispose();
            }
        }
    }
}
