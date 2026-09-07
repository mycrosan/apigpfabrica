package br.compneusgppremium.api.controller;
import br.compneusgppremium.api.service.ImagemCarcacaService;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
@RestController
@RequiredArgsConstructor
public class ImagemCarcacaController {
    private final ImagemCarcacaService imagens;
    @GetMapping("/api/image/{caminho}/{idImg}")
    public byte[] exibir(@PathVariable final String caminho, @PathVariable final String idImg) throws IOException {
        return imagens.carregar(caminho, idImg);
    }
}
