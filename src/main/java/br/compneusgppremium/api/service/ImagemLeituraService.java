package br.compneusgppremium.api.service;
import br.compneusgppremium.api.config.OcrLocalProperties;
import br.compneusgppremium.api.exception.CadastroPneuException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
@Service
@RequiredArgsConstructor
public class ImagemLeituraService {
    private final OcrLocalProperties configuracao;
    public byte[] validar(final String entrada) {
        if (entrada == null || entrada.length() > ((configuracao.limiteBytes() + 2L) / 3) * 4 + 256) {
            throw new CadastroPneuException("IMAGEM_GRANDE", "Imagem acima do limite.", HttpStatus.PAYLOAD_TOO_LARGE);
        }
        String base64 = entrada;
        if (base64.startsWith("data:")) {
            int separador = base64.indexOf(',');
            if (separador < 0) {
                throw invalida();
            }
            base64 = base64.substring(separador + 1);
        }
        try {
            byte[] bytes = Base64.getDecoder().decode(base64);
            if (bytes.length > configuracao.limiteBytes()) {
                throw new CadastroPneuException("IMAGEM_GRANDE", "Imagem acima do limite.", HttpStatus.PAYLOAD_TOO_LARGE);
            }
            try (ImageInputStream stream = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
                var leitores = ImageIO.getImageReaders(stream);
                if (!leitores.hasNext()) {
                    throw invalida();
                }
                ImageReader leitor = leitores.next();
                try {
                    String formato = leitor.getFormatName();
                    if (!"JPEG".equalsIgnoreCase(formato) && !"PNG".equalsIgnoreCase(formato)) {
                        throw invalida();
                    }
                    leitor.setInput(stream);
                    long pixels = (long) leitor.getWidth(0) * leitor.getHeight(0);
                    if (pixels > configuracao.limitePixels()) {
                        throw new CadastroPneuException("IMAGEM_GRANDE", "Imagem acima do limite de pixels.",
                                HttpStatus.PAYLOAD_TOO_LARGE);
                    }
                    leitor.read(0);
                } finally {
                    leitor.dispose();
                }
            }
            return bytes;
        } catch (IllegalArgumentException | IOException erro) {
            throw invalida();
        }
    }
    public record Metadados(String mime, int largura, int altura) { }
    public Metadados descrever(final byte[] bytes) {
        try (var stream = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            var leitores = ImageIO.getImageReaders(stream);
            if (!leitores.hasNext()) { throw invalida(); }
            var leitor = leitores.next();
            try {
                leitor.setInput(stream);
                String mime = "PNG".equalsIgnoreCase(leitor.getFormatName()) ? "image/png" : "image/jpeg";
                return new Metadados(mime, leitor.getWidth(0), leitor.getHeight(0));
            } finally { leitor.dispose(); }
        } catch (IOException erro) { throw invalida(); }
    }
    private CadastroPneuException invalida() {
        return new CadastroPneuException("IMAGEM_INVALIDA", "Não foi possível decodificar a imagem.", HttpStatus.BAD_REQUEST);
    }
}
