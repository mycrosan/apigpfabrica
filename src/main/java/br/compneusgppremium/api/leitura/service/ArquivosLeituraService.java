package br.compneusgppremium.api.leitura.service;
import br.compneusgppremium.api.exception.CadastroPneuException;
import java.io.IOException;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
@Service
public class ArquivosLeituraService {
    private final Path raiz;
    public ArquivosLeituraService(@Value("${pneus.evidencias.diretorio:./uploads/leitura}") final String diretorio) {
        raiz = Path.of(diretorio).toAbsolutePath().normalize();
    }
    public String salvar(final byte[] bytes) {
        Path temporario = null;
        try {
            Files.createDirectories(raiz);
            String nome = UUID.randomUUID() + ".foto";
            temporario = Files.createTempFile(raiz, "captura-", ".pendente");
            Files.write(temporario, bytes, StandardOpenOption.TRUNCATE_EXISTING);
            // O arquivo só fica disponível por UUID quando todos os bytes foram escritos.
            Files.move(temporario, raiz.resolve(nome), StandardCopyOption.ATOMIC_MOVE);
            return nome;
        } catch (IOException erro) {
            if (temporario != null) {
                try { Files.deleteIfExists(temporario); }
                catch (IOException limpeza) { erro.addSuppressed(limpeza); }
            }
            throw new IllegalStateException("Falha ao arquivar evidência", erro);
        }
    }
    public byte[] ler(final String nome) {
        Path arquivo = resolver(nome);
        try (var entrada = Files.newInputStream(arquivo, LinkOption.NOFOLLOW_LINKS)) { return entrada.readAllBytes(); }
        catch (IOException erro) { throw new IllegalStateException("Evidência indisponível", erro); }
    }
    public void removerOrfao(final String nome) {
        try { Files.deleteIfExists(resolver(nome)); }
        catch (IOException erro) { throw new IllegalStateException("Reconciliação de evidência necessária", erro); }
    }
    public java.util.List<String> listarAnteriores(final java.time.Instant limite) {
        if (!Files.isDirectory(raiz)) { return java.util.List.of(); }
        try (var arquivos = Files.list(raiz)) {
            var encontrados = new java.util.ArrayList<String>();
            for (Path arquivo : arquivos.toList()) {
                String nome = arquivo.getFileName().toString();
                if (nome.matches("[0-9a-f-]{36}\\.foto") && Files.isRegularFile(arquivo, LinkOption.NOFOLLOW_LINKS)
                        && Files.getLastModifiedTime(arquivo, LinkOption.NOFOLLOW_LINKS).toInstant().isBefore(limite)) {
                    encontrados.add(nome);
                }
            }
            return java.util.List.copyOf(encontrados);
        } catch (IOException erro) { throw new IllegalStateException("Falha ao listar evidências para reconciliação", erro); }
    }
    private Path resolver(final String nome) {
        if (nome == null || !nome.matches("[0-9a-f-]{36}\\.foto")) {
            throw new CadastroPneuException("IMAGEM_INVALIDA", "Referência de imagem inválida.");
        }
        return raiz.resolve(nome);
    }
    public static String hash(final byte[] bytes) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch (NoSuchAlgorithmException erro) { throw new IllegalStateException("SHA-256 indisponível", erro); }
    }
}
