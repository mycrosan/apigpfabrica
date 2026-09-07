package br.compneusgppremium.api.revisao.service;
import java.awt.image.BufferedImage;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
class PreviaRevisaoServiceTest {
    @Test void aplicaAsOitoOrientacoesExifSemTrocarRegiao() {
        var original = new BufferedImage(3, 2, BufferedImage.TYPE_INT_RGB);
        int valor = 1;
        for (int linha = 0; linha < 2; linha++) for (int coluna = 0; coluna < 3; coluna++) original.setRGB(coluna, linha, valor++);
        int[][] esperados = {{1,2,3,4,5,6}, {3,2,1,6,5,4}, {6,5,4,3,2,1}, {4,5,6,1,2,3},
                {1,4,2,5,3,6}, {4,1,5,2,6,3}, {6,3,5,2,4,1}, {3,6,2,5,1,4}};
        for (int orientacao = 1; orientacao <= 8; orientacao++) {
            var previa = new PreviaRevisaoService().orientar(original, orientacao);
            assertThat(previa.getWidth()).isEqualTo(orientacao >= 5 ? 2 : 3);
            int indice = 0;
            for (int linha = 0; linha < previa.getHeight(); linha++) {
                for (int coluna = 0; coluna < previa.getWidth(); coluna++) {
                    assertThat(previa.getRGB(coluna, linha) & 0xffffff).isEqualTo(esperados[orientacao - 1][indice++]);
                }
            }
        }
    }
}
