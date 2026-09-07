package br.compneusgppremium.api.service;
import br.compneusgppremium.api.exception.CadastroPneuException;
import br.compneusgppremium.api.util.OperationSystem;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
@Service
public class ImagemCarcacaService {
    public byte[] carregar(final String caminho, final String nome) throws IOException {
        if (!caminho.matches("[a-zA-Z0-9_-]+") || !nome.matches("[a-zA-Z0-9_.-]+")) {
            throw new CadastroPneuException("IMAGEM_INVALIDA", "Caminho de imagem inválido.");
        }
        Path base = Path.of(new OperationSystem().placeImageSystem(caminho)).toRealPath();
        Path arquivo = base.resolve(nome).normalize();
        if (!arquivo.startsWith(base) || !Files.isRegularFile(arquivo) || !arquivo.toRealPath().startsWith(base)) {
            throw new CadastroPneuException("IMAGEM_AUSENTE", "Imagem não encontrada.", HttpStatus.NOT_FOUND);
        }
        return Files.readAllBytes(arquivo);
    }
}
