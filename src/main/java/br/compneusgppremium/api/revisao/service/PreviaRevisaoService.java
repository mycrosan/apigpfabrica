package br.compneusgppremium.api.revisao.service;
import br.compneusgppremium.api.exception.CadastroPneuException;
import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.exif.ExifIFD0Directory;
import java.awt.image.BufferedImage;
import java.io.*;
import javax.imageio.ImageIO;
import org.springframework.stereotype.Service;
@Service
public class PreviaRevisaoService {
    public record Previa(byte[] bytes, int largura, int altura) { }
    public Previa preparar(final byte[] original) {
        try {
            var metadados = ImageMetadataReader.readMetadata(new ByteArrayInputStream(original));
            var diretorio = metadados.getFirstDirectoryOfType(ExifIFD0Directory.class);
            Integer orientacao = diretorio == null ? null : diretorio.getInteger(ExifIFD0Directory.TAG_ORIENTATION);
            var imagem = orientar(ImageIO.read(new ByteArrayInputStream(original)), orientacao == null ? 1 : orientacao);
            var saida = new ByteArrayOutputStream();
            // Recodificação sem EXIF: GPS não chega ao aplicativo. Coordenadas seguem o exif_transpose do PaddleOCR.
            if (!ImageIO.write(imagem, "png", saida)) { throw new IOException("Codificador PNG indisponível"); }
            return new Previa(saida.toByteArray(), imagem.getWidth(), imagem.getHeight());
        } catch (IOException | com.drew.imaging.ImageProcessingException erro) {
            throw new CadastroPneuException("PREVIA_INVALIDA", "Não foi possível preparar a foto para revisão.");
        }
    }
    BufferedImage orientar(final BufferedImage original, final int orientacao) {
        if (original == null) { throw new CadastroPneuException("PREVIA_INVALIDA", "Foto inválida."); }
        int largura = original.getWidth(), altura = original.getHeight();
        boolean trocar = orientacao >= 5 && orientacao <= 8;
        var destino = new BufferedImage(trocar ? altura : largura, trocar ? largura : altura, BufferedImage.TYPE_INT_RGB);
        for (int linha = 0; linha < altura; linha++) {
            for (int coluna = 0; coluna < largura; coluna++) {
                int horizontal = switch (orientacao) {
                    case 2, 3 -> largura - 1 - coluna;
                    case 5, 8 -> linha;
                    case 6, 7 -> altura - 1 - linha;
                    default -> coluna;
                };
                int vertical = switch (orientacao) {
                    case 3, 4 -> altura - 1 - linha;
                    case 5, 6 -> coluna;
                    case 7, 8 -> largura - 1 - coluna;
                    default -> linha;
                };
                destino.setRGB(horizontal, vertical, original.getRGB(coluna, linha));
            }
        }
        return destino;
    }
}
